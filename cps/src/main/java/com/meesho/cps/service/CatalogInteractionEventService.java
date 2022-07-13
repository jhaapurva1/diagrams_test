package com.meesho.cps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.*;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.kafka.BudgetExhaustedEvent;
import com.meesho.cps.data.entity.kafka.SupplierWeeklyBudgetExhaustedEvent;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
import com.meesho.cps.db.redis.dao.SuppliersWeeklyBudgetExhaustEventStateDao;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cps.transformer.PrismEventTransformer;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.meesho.cps.constants.TelegrafConstants.*;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Slf4j
@Service
public class CatalogInteractionEventService {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Autowired
    private CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Autowired
    private UserCatalogInteractionCacheDao userCatalogInteractionCacheDao;

    @Autowired
    private AdBillFactory adBillFactory;

    @Autowired
    private CampaignPerformanceHelper campaignHelper;

    @Autowired
    private CampaignMetricsRepository campaignMetricsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrismService prismService;

    @Autowired
    private AdService adService;

    @Autowired
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private SupplierWeekWiseMetricsRepository supplierWeekWiseMetricsRepository;

    @Autowired
    private SuppliersWeeklyBudgetExhaustEventStateDao suppliersWeeklyBudgetExhaustEventStateDao;

    @Value(Constants.Kafka.BUDGET_EXHAUSTED_TOPIC)
    String budgetExhaustedTopic;

    @Value(Constants.Kafka.SUPPLIER_WEEKLY_BUDGET_EXHAUSTED_TOPIC)
    private String suppliersWeeklyBudgetExhaustedTopic;

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "class=CatalogInteractionEventService")
    public void handle(AdInteractionEvent adInteractionEvent) throws ExternalRequestFailedException {
        Long interactionTime = adInteractionEvent.getEventTimestamp();
        String userId = adInteractionEvent.getUserId();
        Long catalogId = adInteractionEvent.getProperties().getId();

        AdInteractionPrismEvent adInteractionPrismEvent =
                PrismEventTransformer.getAdInteractionPrismEvent(adInteractionEvent, userId, catalogId);

        List<CampaignCatalogMetadataResponse.CatalogMetadata> catalogMetadataList =
                adService.getCampaignCatalogMetadata(Lists.newArrayList(catalogId));

        if (CollectionUtils.isEmpty(catalogMetadataList) ||
                Objects.isNull(catalogMetadataList.get(0).getCampaignDetails())) {
            log.error("No active ad on catalogId {}", catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.CAMPAIGN_INACTIVE);
            publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.CAMPAIGN_INACTIVE.name());
            return;
        }

        CampaignDetails catalogMetadata = catalogMetadataList.get(0).getCampaignDetails();
        Long campaignId = catalogMetadata.getCampaignId();
        Long supplierId = catalogMetadata.getSupplierId();
        BigDecimal totalBudget = catalogMetadata.getBudget();
        BigDecimal weeklyBudgetUtilisationLimit = BigDecimal.ZERO; //ToDo - implementation pending from the Ad-Server side
        Integer billVersion = catalogMetadata.getBillVersion();
        BigDecimal cpc = catalogMetadata.getCpc();
        CampaignType campaignType = CampaignType.fromValue(catalogMetadata.getCampaignType());
        adInteractionPrismEvent.setCampaignId(campaignId);
        LocalDate eventDate = campaignHelper.getLocalDateForDailyCampaignFromLocalDateTime(
                DateTimeUtils.getCurrentLocalDateTimeInIST());
        LocalDate weekStartDate = campaignHelper.getWeekStartDate(DateTimeUtils.getCurrentLocalDateTimeInIST());

        log.info("CPC for event_id {} catalog id {} in campaign {} is {}", adInteractionEvent.getEventId(), catalogId,
                campaignId, cpc);
        adInteractionPrismEvent.setCpc(cpc);

        BillHandler billHandler = adBillFactory.getBillHandlerForBillVersion(billVersion);

        //Check if event is valid for the bill version of campaign
        if (!billHandler.getValidEvents().contains(adInteractionEvent.getEventName())) {
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.NON_BILLABLE_INTERACTION);
            publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.NON_BILLABLE_INTERACTION.name());
            return;
        }

        if (StringUtils.isEmpty(adInteractionEvent.getProperties().getScreen())) {
            adInteractionEvent.getProperties().setScreen(Constants.DefaultRealEstateMetaData.SCREEN);
        }
        String origin = adInteractionEvent.getProperties().getOrigin();
        String screen = adInteractionEvent.getProperties().getScreen();
        //Perform deduplication
        if (billHandler.performWindowDeDuplication()) {
            Long previousInteractionTime = userCatalogInteractionCacheDao.get(userId, catalogId, origin, screen);
            if (!checkIfInteractionNeedsToBeConsidered(previousInteractionTime, interactionTime)) {
                log.warn("Ignoring click event since window hasn't passed or wrong ordering," +
                        " event : {}, previousInteractionTime {}", adInteractionEvent, previousInteractionTime);
                adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
                adInteractionPrismEvent.setReason(AdInteractionInvalidReason.DUPLICATE);
                publishPrismEvent(adInteractionPrismEvent);
                telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                        adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                        AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.DUPLICATE.name());
                return;
            }
            userCatalogInteractionCacheDao.set(userId, catalogId, origin, screen, interactionTime);
        }

        //Update campaign catalog date metrics
        incrementInteractionCount(campaignId, catalogId, eventDate, adInteractionEvent.getEventName());
        // Update budget utilised
        BigDecimal budgetUtilised = modifyAndGetBudgetUtilised(cpc, campaignId, catalogId, eventDate, campaignType);

        if (budgetUtilised.compareTo(totalBudget) >= 0) {
            BudgetExhaustedEvent budgetExhaustedEvent = BudgetExhaustedEvent.builder().catalogId(catalogId).campaignId(campaignId).build();
            try {
                kafkaService.sendMessage(budgetExhaustedTopic, String.valueOf(campaignId),
                        objectMapper.writeValueAsString(budgetExhaustedEvent));
            } catch (Exception e) {
                log.error("Exception while sending budgetExhausted event {}", budgetExhaustedEvent, e);
            }
        }

        //update supplier weekly budget utilised
        BigDecimal supplierWeeklyBudgetUtilised = modifyAndGetSupplierWeeklyBudgetUtilised(supplierId, weekStartDate, cpc);
        if (supplierWeeklyBudgetUtilised.compareTo(weeklyBudgetUtilisationLimit) >= 0 &&
                !suppliersWeeklyBudgetExhaustEventStateDao.isEventAlreadyFired(supplierId)) {
            SupplierWeeklyBudgetExhaustedEvent supplierWeeklyBudgetExhaustedEvent =
                    SupplierWeeklyBudgetExhaustedEvent.builder().supplierId(supplierId).catalogId(catalogId).build();
            try {
                kafkaService.sendMessage(suppliersWeeklyBudgetExhaustedTopic, String.valueOf(supplierId),
                        objectMapper.writeValueAsString(supplierWeeklyBudgetExhaustedEvent));
                suppliersWeeklyBudgetExhaustEventStateDao.setEventAsFired(supplierId);
            } catch (Exception e) {
                log.error("Exception while sending supplierWeeklyBudgetExhausted event {}", supplierWeeklyBudgetExhaustedEvent, e);
            }
        }

        adInteractionPrismEvent.setStatus(AdInteractionStatus.VALID);
        publishPrismEvent(adInteractionPrismEvent);

        updatedCampaignCatalogCacheDao.add(Arrays.asList(new CampaignCatalogDate(campaignId, catalogId,
                eventDate.toString())));

        telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
        int cpcNormalised = cpc.multiply(BigDecimal.valueOf(100)).intValue();
        telegrafMetricsHelper.increment(INTERACTION_EVENT_CPC_KEY, cpcNormalised, INTERACTION_EVENT_CPC_TAGS,
                adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin());
    }

    private void publishPrismEvent(AdInteractionPrismEvent adInteractionPrismEvent) {
        List<AdInteractionPrismEvent> prismEvents = new ArrayList<>(Arrays.asList(adInteractionPrismEvent));
        prismService.publishEvent(Constants.PrismEventNames.AD_INTERACTIONS, prismEvents);
    }

    /**
     * @param previousInteractionTime in millis
     * @param interactionTime         in millis
     * @return boolean
     */
    public boolean checkIfInteractionNeedsToBeConsidered(Long previousInteractionTime, Long interactionTime) {
        int windowTimeInMillis = applicationProperties.getUserCatalogInteractionWindowInSeconds() * 1000;
        return Objects.isNull(previousInteractionTime) || ((previousInteractionTime <= interactionTime) &&
                (interactionTime - previousInteractionTime >= windowTimeInMillis));
    }

    public void incrementInteractionCount(Long campaignId, Long catalogId, LocalDate date, String eventName) {
        log.info("campaignId {}, catalogId {}, date{}, eventName {}" , campaignId, catalogId, date, eventName);
        // CAUTION: Please do not change the order of cases here since action is the combination of multiple cases
        switch (eventName) {
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_SHARED_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_SHARED_EVENT_NAME:
                campaignCatalogDateMetricsRepository.incrementSharesCount(campaignId, catalogId, date);
                break;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_WISHLISTED_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_WISHLISTED_EVENT_NAME:
                campaignCatalogDateMetricsRepository.incrementWishlistCount(campaignId, catalogId, date);
                break;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_CLICK_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_CLICK_EVENT_NAME:
                campaignCatalogDateMetricsRepository.incrementClickCount(campaignId, catalogId, date);
                break;
        }
    }

    public BigDecimal modifyAndGetBudgetUtilised(BigDecimal cpc, Long campaignId, Long catalogId, LocalDate date,
                                                 CampaignType campaignType) {
        campaignCatalogDateMetricsRepository.incrementBudgetUtilised(campaignId, catalogId, date, cpc);
        if (CampaignType.DAILY_BUDGET.equals(campaignType)) {
            return campaignDatewiseMetricsRepository.incrementBudgetUtilised(campaignId, date, cpc);
        }
        return campaignMetricsRepository.incrementBudgetUtilised(campaignId, cpc);
    }

    public BigDecimal modifyAndGetSupplierWeeklyBudgetUtilised(Long supplierId, LocalDate weekStartDate, BigDecimal cpc) {
        return supplierWeekWiseMetricsRepository.incrementBudgetUtilised(supplierId, weekStartDate, cpc);
    }

}
