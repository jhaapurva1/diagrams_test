package com.meesho.cps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.ads.lib.utils.Utils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.*;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.kafka.BudgetExhaustedEvent;
import com.meesho.cps.data.entity.mysql.RealEstateMetadata;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.redis.dao.RealEstateMetadataCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cps.transformer.PrismEventTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

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
    private CampaignCatalogMetricsRepository campaignCatalogMetricsRepository;

    @Autowired
    private CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Autowired
    private RealEstateMetadataCacheDao realEstateMetadataCacheDao;

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

    @Transactional
    public void handle(AdInteractionEvent adInteractionEvent) {
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
            return;
        }

        CampaignDetails catalogMetadata = catalogMetadataList.get(0).getCampaignDetails();
        Long campaignId = catalogMetadata.getCampaignId();
        BigDecimal totalBudget = catalogMetadata.getBudget();
        Integer billVersion = catalogMetadata.getBillVersion();
        BigDecimal cpc = catalogMetadata.getCpc();
        CampaignType campaignType = CampaignType.fromValue(catalogMetadata.getCampaignType());
        adInteractionPrismEvent.setCampaignId(campaignId);

        if (StringUtils.isEmpty(adInteractionEvent.getProperties().getScreen())) {
            adInteractionEvent.getProperties().setScreen(Constants.DefaultRealEstateMetaData.SCREEN);
        }

        CampaignCatalogMetrics campaignCatalogMetrics = campaignCatalogMetricsRepository.get(campaignId, catalogId);
        if (Objects.isNull(campaignCatalogMetrics)) {
            log.info("Creating campaignCatalogMetrics for campaignId {}, catalogId {}",campaignId, catalogId);
            campaignCatalogMetrics =
                    CampaignPerformanceTransformer.getCampaignCatalogMetricsFromRequest(campaignId, catalogId);
            campaignCatalogMetricsRepository.put(campaignCatalogMetrics);
        }

        String origin = adInteractionEvent.getProperties().getOrigin();
        String screen = adInteractionEvent.getProperties().getScreen();
        RealEstateMetadata realEstateMetadata = realEstateMetadataCacheDao.get(origin, Utils.getCountry());
        if (Objects.isNull(realEstateMetadata)) {
            log.warn("Invalid click event with origin {}", origin);
            realEstateMetadata =
                    realEstateMetadataCacheDao.get(Constants.DefaultRealEstateMetaData.ORIGIN, Utils.getCountry());
        }
        adInteractionPrismEvent.setClickMultiplier(realEstateMetadata.getClickMultiplier());

        BillHandler billHandler = adBillFactory.getBillHandlerForBillVersion(billVersion);

        log.info("CPC for event_id {} catalog id {} in campaign {} is {}", adInteractionEvent.getEventId(),
                campaignCatalogMetrics.getCatalogId(), campaignId, cpc);
        adInteractionPrismEvent.setCpc(cpc);

        //Check if event is valid for the bill version of campaign
        if (!billHandler.getValidEvents().contains(adInteractionEvent.getEventName())) {
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.NON_BILLABLE_INTERACTION);
            publishPrismEvent(adInteractionPrismEvent);
            return;
        }

        //Perform deduplication
        if (billHandler.performWindowDeDuplication()) {
            Long previousInteractionTime = userCatalogInteractionCacheDao.get(userId, catalogId, origin, screen);
            if (!checkIfInteractionNeedsToBeConsidered(previousInteractionTime, interactionTime)) {
                log.warn("Ignoring click event since window hasn't passed or wrong ordering," +
                        " event : {}, previousInteractionTime {}", adInteractionEvent, previousInteractionTime);
                adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
                adInteractionPrismEvent.setReason(AdInteractionInvalidReason.DUPLICATE);
                publishPrismEvent(adInteractionPrismEvent);
                return;
            }
            userCatalogInteractionCacheDao.set(userId, catalogId, origin, screen, interactionTime);
        }

        LocalDate eventDate = campaignHelper.getLocalDateForDailyCampaignFromLocalDateTime(
                DateTimeUtils.getCurrentLocalDateTimeInIST());

        //Update campaign catalog metrics
        modifyInteractionCounts(campaignCatalogMetrics, realEstateMetadata.getClickMultiplier(),
                adInteractionEvent.getEventName());

        Map<String, Long> originWiseClickCount = campaignCatalogMetrics.getOriginWiseClickCount();
        originWiseClickCount = Objects.isNull(originWiseClickCount) ? new HashMap<>() : originWiseClickCount;
        originWiseClickCount.put(origin, originWiseClickCount.getOrDefault(origin, 0L) + 1);
        campaignCatalogMetrics.setOriginWiseClickCount(originWiseClickCount);
        campaignCatalogMetricsRepository.put(campaignCatalogMetrics);

        // Update budget utilised
        BigDecimal budgetUtilised =
                modifyAndGetBudgetUtilised(cpc, realEstateMetadata.getClickMultiplier(), campaignId, catalogId,
                        eventDate, campaignType);


        if (budgetUtilised.compareTo(totalBudget) >= 0) {
            BudgetExhaustedEvent budgetExhaustedEvent = BudgetExhaustedEvent.builder().campaignId(campaignId).build();
            try {
                kafkaService.sendMessage(Constants.BUDGET_EXHAUSTED_TOPIC, String.valueOf(campaignId),
                        objectMapper.writeValueAsString(budgetExhaustedEvent));
            } catch (Exception e) {
                log.error("Exception while sending budgetExhausted event {}", budgetExhaustedEvent, e);
            }
        }
        adInteractionPrismEvent.setStatus(AdInteractionStatus.VALID);
        publishPrismEvent(adInteractionPrismEvent);

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

    public void modifyInteractionCounts(CampaignCatalogMetrics campaignCatalogMetrics, BigDecimal clickMultiplier,
                                        String eventName) {
        // CAUTION: Please do not change the order of cases here since action is the combination of multiple cases
        switch (eventName) {
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_SHARED_TOPIC:
            case ConsumerConstants.IngestionInteractionEvents.AD_SHARED_TOPIC:
                campaignCatalogMetrics.setWeightedSharesCount(
                        clickMultiplier.add(campaignCatalogMetrics.getWeightedSharesCount()));
                break;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_WISHLISTED_TOPIC:
            case ConsumerConstants.IngestionInteractionEvents.AD_WISHLISTED_TOPIC:
                campaignCatalogMetrics.setWeightedWishlistCount(
                        clickMultiplier.add(campaignCatalogMetrics.getWeightedWishlistCount()));
                break;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_CLICK_TOPIC:
            case ConsumerConstants.IngestionInteractionEvents.AD_CLICK_TOPIC:
                campaignCatalogMetrics.setWeightedClickCount(
                        clickMultiplier.add(campaignCatalogMetrics.getWeightedClickCount()));
                break;
        }
    }

    public BigDecimal modifyAndGetBudgetUtilised(BigDecimal cpc, BigDecimal clickMultiplier, Long campaignId,
                                                 Long catalogId, LocalDate date, CampaignType campaignType) {
        BigDecimal interactionMultiplier = clickMultiplier.multiply(cpc);
        campaignCatalogMetricsRepository.incrementBudgetUtilised(campaignId, catalogId, interactionMultiplier);
        if (CampaignType.DAILY_BUDGET.equals(campaignType)) {
            return campaignDatewiseMetricsRepository.incrementBudgetUtilised(campaignId, date, interactionMultiplier);
        }
        return campaignMetricsRepository.incrementBudgetUtilised(campaignId, interactionMultiplier);
    }

}
