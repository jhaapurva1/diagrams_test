package com.meesho.cps.service;

import com.meesho.ad.client.data.AdsMetadata;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ad.client.response.SupplierCampaignCatalogMetaDataResponse;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.*;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.helper.InteractionEventAttributionHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.transformer.OriginScreenREMapper;
import com.meesho.cps.transformer.PrismEventTransformer;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

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
    private UserCatalogInteractionCacheDao userCatalogInteractionCacheDao;

    @Autowired
    private AdBillFactory adBillFactory;

    @Autowired
    private CampaignPerformanceHelper campaignHelper;

    @Autowired
    private AdService adService;

    @Autowired
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private InteractionEventAttributionHelper interactionEventAttributionHelper;

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "class=CatalogInteractionEventService")
    public void handle(AdInteractionEvent adInteractionEvent) throws ExternalRequestFailedException {
        Long interactionTime = adInteractionEvent.getEventTimestamp();
        String userId = adInteractionEvent.getUserId();
        Long catalogId = adInteractionEvent.getProperties().getId();
        Long productId = adInteractionEvent.getProperties().getProductId();
        AdsMetadata adsMetadataObject = AdsMetadata.decrypt(adInteractionEvent.getProperties().getAdsMetadata(), applicationProperties.getAdsMetadataEncryptionKey());
        Long campaignId = adsMetadataObject.getCampaignId();
        BigDecimal cpc = Objects.isNull(adsMetadataObject.getCpc()) ? null : BigDecimal.valueOf(adsMetadataObject.getCpc());
        if (Objects.nonNull(cpc) && BigDecimal.ZERO.equals(cpc)) {
            cpc = null;
        }

        AdInteractionPrismEvent adInteractionPrismEvent =
                PrismEventTransformer.getAdInteractionPrismEvent(adInteractionEvent, userId, catalogId, productId);


        String feedType = null;
        if(Objects.nonNull(adInteractionEvent.getProperties())){
            String origin = adInteractionEvent.getProperties().getOrigin();
            String screen = adInteractionEvent.getProperties().getScreen();
            feedType = OriginScreenREMapper.getFeedType(origin, screen);
        }

        SupplierCampaignCatalogMetaDataResponse response = adService.getSupplierCampaignCatalogMetadata(catalogId, campaignId, userId, feedType);
        SupplierCampaignCatalogMetaDataResponse.CatalogMetadata catalogMetadata = response.getCatalogMetadata();
        SupplierCampaignCatalogMetaDataResponse.SupplierMetadata supplierMetadata = response.getSupplierMetadata();

        if (Objects.isNull(catalogMetadata) || Objects.isNull(catalogMetadata.getCampaignDetails())) {
            log.warn("No active ad on catalogId {}", catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.CAMPAIGN_INACTIVE);
            interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.CAMPAIGN_INACTIVE.name());
            return;
        }

        CampaignDetails campaignDetails = catalogMetadata.getCampaignDetails();
        BigDecimal catalogBudgetUtilisationLimit = catalogMetadata.getCatalogBudget();

        BigDecimal totalBudget = campaignDetails.getBudget();
        Integer billVersion = campaignDetails.getBillVersion();
        CampaignType campaignType = CampaignType.fromValue(campaignDetails.getCampaignType());
        campaignId = campaignDetails.getCampaignId();
        cpc = interactionEventAttributionHelper.getChargeableCpc(cpc, campaignDetails, catalogId);
        if (Objects.isNull(cpc)) {
            log.warn("can not process interaction event due to null cpc.  {} - {}", campaignId, catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.CPC_NOT_FOUND);
            interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.CPC_NOT_FOUND.name());
            return;
        }

        adInteractionPrismEvent.setCampaignId(campaignId);
        LocalDate eventDate = campaignHelper.getLocalDateForDailyCampaignFromLocalDateTime(
                DateTimeUtils.getCurrentLocalDateTimeInIST());
        Long supplierId = supplierMetadata.getSupplierId();
        BigDecimal weeklyBudgetUtilisationLimit = supplierMetadata.getBudgetUtilisationLimit();
        LocalDate weekStartDate = DateTimeUtils.getFirstDayOfWeek().toLocalDate();

        log.info("CPC for event_id {} catalog id {} in campaign {} is {}", adInteractionEvent.getEventId(), catalogId,
                campaignId, cpc);
        adInteractionPrismEvent.setCpc(cpc);

        BillHandler billHandler = adBillFactory.getBillHandlerForBillVersion(billVersion);

        //Check if event is valid for the bill version of campaign
        if (!billHandler.getValidEvents().contains(adInteractionEvent.getEventName())) {
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.NON_BILLABLE_INTERACTION);
            interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.NON_BILLABLE_INTERACTION.name());
            return;
        }

        if (interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(campaignDetails, weekStartDate, eventDate, weeklyBudgetUtilisationLimit, catalogId)) {
            log.warn("Budget exhausted for catalogId {}", catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.BUDGET_EXHAUSTED);
            interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.BUDGET_EXHAUSTED.name());
            return;
        }

        if (StringUtils.isEmpty(adInteractionEvent.getProperties().getScreen())) {
            adInteractionEvent.getProperties().setScreen(Constants.DefaultRealEstateMetaData.SCREEN);
        }
        if (StringUtils.isEmpty(adInteractionEvent.getProperties().getOrigin())) {
            adInteractionEvent.getProperties().setOrigin(Constants.DefaultRealEstateMetaData.ORIGIN);
        }
        String origin = adInteractionEvent.getProperties().getOrigin();
        String screen = adInteractionEvent.getProperties().getScreen();
        //Perform deduplication
        if (billHandler.performWindowDeDuplication()) {
            Long id = Objects.nonNull(productId) ? productId : catalogId;
            AdUserInteractionType type = Objects.nonNull(productId) ? AdUserInteractionType.PRODUCT_ID : AdUserInteractionType.CATALOG_ID;
            Long previousInteractionTime = userCatalogInteractionCacheDao.get(userId, id, origin, screen, type);
            if (!interactionEventAttributionHelper.checkIfInteractionNeedsToBeConsidered(previousInteractionTime, interactionTime)) {
                log.warn("Ignoring click event since window hasn't passed or wrong ordering," +
                        " event : {}, previousInteractionTime {}", adInteractionEvent, previousInteractionTime);
                adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
                adInteractionPrismEvent.setReason(AdInteractionInvalidReason.DUPLICATE);
                interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
                telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                        adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                        AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.DUPLICATE.name());
                return;
            }
            userCatalogInteractionCacheDao.set(userId, id, origin, screen, interactionTime, type);
        }

        //Update campaign catalog date metrics
        interactionEventAttributionHelper.incrementInteractionCount(supplierId, campaignId, catalogId, eventDate, adInteractionEvent.getEventName());
        // Update budget utilised
        BudgetUtilisedData budgetUtilised = interactionEventAttributionHelper.modifyAndGetBudgetUtilised(cpc, supplierId, campaignId, catalogId, eventDate, campaignType);

        if (budgetUtilised.getCampaignBudgetUtilised().compareTo(totalBudget) >= 0) {
            interactionEventAttributionHelper.sendBudgetExhaustedEvent(campaignId, catalogId);
        } //If we have paused the campaign then no need to pause the catalog. hence using else-if
        else if (Objects.nonNull(catalogBudgetUtilisationLimit)
                && catalogBudgetUtilisationLimit.compareTo(BigDecimal.ZERO) > 0
                && budgetUtilised.getCatalogBudgetUtilised().compareTo(catalogBudgetUtilisationLimit) >= 0) {
            interactionEventAttributionHelper.sendCatalogBudgetExhaustEvent(campaignId, catalogId);
        }

        //update supplier weekly budget utilised
        BigDecimal supplierWeeklyBudgetUtilised = interactionEventAttributionHelper.modifyAndGetSupplierWeeklyBudgetUtilised(supplierId, weekStartDate, cpc);
        if (Objects.nonNull(weeklyBudgetUtilisationLimit) && supplierWeeklyBudgetUtilised.compareTo(weeklyBudgetUtilisationLimit) >= 0) {
            interactionEventAttributionHelper.sendSupplierBudgetExhaustedEvent(supplierId, catalogId);
        }

        adInteractionPrismEvent.setStatus(AdInteractionStatus.VALID);
        interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);

        updatedCampaignCatalogCacheDao.add(Arrays.asList(new CampaignCatalogDate(campaignId, catalogId,
                eventDate.toString())));

        telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
        int cpcNormalised = cpc.multiply(BigDecimal.valueOf(100)).intValue();
        telegrafMetricsHelper.increment(INTERACTION_EVENT_CPC_KEY, cpcNormalised, INTERACTION_EVENT_CPC_TAGS,
                adInteractionEvent.getEventName(), adInteractionEvent.getProperties().getScreen(), adInteractionEvent.getProperties().getOrigin());
    }
}
