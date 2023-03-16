package com.meesho.cps.service;

import com.meesho.ad.client.response.SupplierCampaignCatalogMetaDataResponse;
import com.meesho.ad.client.data.AdsMetadata;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.*;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.enums.FeedType;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.AdWidgetValidationHelper;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.helper.InteractionEventAttributionHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.transformer.PrismEventTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static com.meesho.cps.constants.TelegrafConstants.*;


@Slf4j
@Service
public class WidgetClickEventService {

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    AdService adService;

    @Autowired
    TelegrafMetricsHelper telegrafMetricsHelper;

    @Autowired
    private CampaignPerformanceHelper campaignHelper;

    @Autowired
    private InteractionEventAttributionHelper interactionEventAttributionHelper;

    @Autowired
    private AdBillFactory adBillFactory;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private UserCatalogInteractionCacheDao userCatalogInteractionCacheDao;

    public void handle(AdWidgetClickEvent adWidgetClickEvent) throws ExternalRequestFailedException {
        log.debug("processing widget click event: {}", adWidgetClickEvent);

        // check if valid ad-widget event
        if (!adWidgetClickEvent.getProperties().getIsAdWidget()
                || !AdWidgetValidationHelper.isValidWidgetRealEstate(adWidgetClickEvent.getProperties().getPrimaryRealEstate())) {
            log.error("Not a valid event userId {} eventId {} for the real estate {}",
                    adWidgetClickEvent.getUserId(), adWidgetClickEvent.getEventId(), adWidgetClickEvent.getProperties().getPrimaryRealEstate());
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, String.format(INTERACTION_EVENT_TAGS,
                    adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(), INVALID,
                    AdInteractionInvalidReason.NOT_AD_WIDGET));
            return;
        }


        Long interactionTime = adWidgetClickEvent.getEventTimestamp();
        String userId = adWidgetClickEvent.getUserId();
        Long catalogId = adWidgetClickEvent.getProperties().getCatalogId();
        Long campaignId = adWidgetClickEvent.getProperties().getCampaignId();

        BigDecimal cpc = null;
        if(Objects.nonNull(adWidgetClickEvent.getProperties().getAdsMetadata())) {
            AdsMetadata adsMetadataObject = AdsMetadata.decrypt(adWidgetClickEvent.getProperties().getAdsMetadata(), applicationProperties.getAdsMetadataEncryptionKey());
            campaignId = adsMetadataObject.getCampaignId();
            cpc = Objects.isNull(adsMetadataObject.getCpc()) ? null : BigDecimal.valueOf(adsMetadataObject.getCpc());
            if (Objects.nonNull(cpc) && BigDecimal.ZERO.equals(cpc)) {
                cpc = null;
            }
        }


        AdInteractionPrismEvent adInteractionPrismEvent =
                PrismEventTransformer.getInteractionEventForWidgetClick(adWidgetClickEvent, userId, catalogId);

        // set feedType
        String feedType = FeedType.TEXT_SEARCH.getValue();

        SupplierCampaignCatalogMetaDataResponse response = adService.getSupplierCampaignCatalogMetadata(catalogId, campaignId, userId, feedType);
        log.debug("campaign catalog metadata: {}", response);
        SupplierCampaignCatalogMetaDataResponse.CatalogMetadata catalogMetadata = response.getCatalogMetadata();
        SupplierCampaignCatalogMetaDataResponse.SupplierMetadata supplierMetadata = response.getSupplierMetadata();

        // Check if catalog metadata is empty
        if (Objects.isNull(catalogMetadata) || Objects.isNull(catalogMetadata.getCampaignDetails())) {
            log.error("No active ad on catalogId {}", catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.CAMPAIGN_INACTIVE);
            interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.CAMPAIGN_INACTIVE.name());
            return;
        }

        CampaignDetails campaignDetails = catalogMetadata.getCampaignDetails();
        BigDecimal catalogBudgetUtilisationLimit = catalogMetadata.getCatalogBudget();

        BigDecimal totalBudget = campaignDetails.getBudget();
        Integer billVersion = campaignDetails.getBillVersion();
        CampaignType campaignType = CampaignType.fromValue(campaignDetails.getCampaignType());
        campaignId = campaignDetails.getCampaignId();
        cpc = interactionEventAttributionHelper.getChargeableCpc(cpc, campaignDetails);
        cpc = interactionEventAttributionHelper.getMultipliedCpc(cpc,
            adWidgetClickEvent.getProperties().getPrimaryRealEstate());
        if (Objects.isNull(cpc)) {
            log.error("can not process widget interaction event due to null cpc.  {} - {}", campaignId, catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.CPC_NOT_FOUND);
            interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.CPC_NOT_FOUND.name());
            return;
        }
        LocalDate eventDate = campaignHelper.getLocalDateForDailyCampaignFromLocalDateTime(
                DateTimeUtils.getCurrentLocalDateTimeInIST());
        Long supplierId = supplierMetadata.getSupplierId();
        BigDecimal weeklyBudgetUtilisationLimit = supplierMetadata.getBudgetUtilisationLimit();
        LocalDate weekStartDate = DateTimeUtils.getFirstDayOfWeek().toLocalDate();

        log.info("CPC for event_id {} catalog id {} in campaign {} is {}",
                adWidgetClickEvent.getEventId(), catalogId, campaignId, cpc);
        adInteractionPrismEvent.setCampaignId(campaignId);
        adInteractionPrismEvent.setCpc(cpc);

        BillHandler billHandler = adBillFactory.getBillHandlerForBillVersion(billVersion);

        if (interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(campaignDetails, weekStartDate, eventDate, weeklyBudgetUtilisationLimit, catalogId)) {
            log.error("Budget exhausted for catalogId {}", catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.BUDGET_EXHAUSTED);
            interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.BUDGET_EXHAUSTED.name());
            return;
        }

        //Perform deduplication
        if(performDedup(billHandler, adWidgetClickEvent, adInteractionPrismEvent, userId, interactionTime)) {
            interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.DUPLICATE.name());
            return;
        }

        //Update campaign catalog date metrics
        log.debug("campaignId {}, catalogId {}, date{}, eventName {}", campaignId, catalogId, eventDate, "AdWidgetClickEvent");
        interactionEventAttributionHelper.incrementInteractionCount(campaignId, catalogId, eventDate,
                ConsumerConstants.IngestionInteractionEvents.AD_CLICK_EVENT_NAME);

        // Update budget utilised
        BudgetUtilisedData budgetUtilised = interactionEventAttributionHelper.modifyAndGetBudgetUtilised(cpc, campaignId, catalogId, eventDate, campaignType);
        if (budgetUtilised.getCampaignBudgetUtilised().compareTo(totalBudget) >= 0) {
            interactionEventAttributionHelper.sendBudgetExhaustedEvent(campaignId, catalogId);
        }//If we have paused the campaign then no need to pause the catalog. hence using else-if
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

        updatedCampaignCatalogCacheDao.add(Arrays.asList(new CampaignCatalogDate(campaignId, catalogId, eventDate.toString())));

        // publish instrumentation event and metrics
        adInteractionPrismEvent.setStatus(AdInteractionStatus.VALID);
        interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);

        telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
        int cpcNormalised = cpc.multiply(BigDecimal.valueOf(100)).intValue();
        telegrafMetricsHelper.increment(INTERACTION_EVENT_CPC_KEY, cpcNormalised, INTERACTION_EVENT_CPC_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin());

    }

    private boolean performDedup(BillHandler billHandler, AdWidgetClickEvent adWidgetClickEvent,
                              AdInteractionPrismEvent adInteractionPrismEvent, String userId, Long interactionTime) {
        log.debug("perform dedup: {} {} {} {} {}", billHandler, adWidgetClickEvent, adWidgetClickEvent, userId, interactionTime);

        // Populating the ORIGIN and SCREEN as per the product requirements:
        // https://docs.google.com/spreadsheets/d/1WOY4CGfMnn5aGgA8kAYLQfU12t6C8dztpF2UhgGKY2E/edit?usp=sharing
        if(Objects.nonNull(adWidgetClickEvent.getProperties().getWidgetGroupPosition()) &&
                adWidgetClickEvent.getProperties().getWidgetGroupPosition() > 1) {
            adWidgetClickEvent.getProperties().setScreen(String.format(Constants.AdWidgets.SCREEN_MID_FEED_SEARCH,
                    adWidgetClickEvent.getProperties().getWidgetGroupPosition()));
        } else {
            adWidgetClickEvent.getProperties().setScreen(Constants.AdWidgets.SCREEN_TOP_OF_SEARCH);
        }
        adWidgetClickEvent.getProperties().setOrigin(Constants.AdWidgets.ORIGIN_SEARCH);
        String origin = adWidgetClickEvent.getProperties().getOrigin();
        String screen = adWidgetClickEvent.getProperties().getScreen();
        adInteractionPrismEvent.setOrigin(origin);
        adInteractionPrismEvent.setScreen(screen);

        if (billHandler.performWindowDeDuplication()) {
            Long id = adWidgetClickEvent.getProperties().getCatalogId();
            AdUserInteractionType type = AdUserInteractionType.CATALOG_ID;
                Long previousInteractionTime = userCatalogInteractionCacheDao.get(userId, id, origin, screen, type);
            if (!interactionEventAttributionHelper.checkIfInteractionNeedsToBeConsidered(previousInteractionTime, interactionTime)) {
                log.warn("Ignoring click event since window hasn't passed or wrong ordering," +
                        " event : {}, previousInteractionTime {}", adWidgetClickEvent, previousInteractionTime);
                adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
                adInteractionPrismEvent.setReason(AdInteractionInvalidReason.DUPLICATE);
                return true;
            }
            userCatalogInteractionCacheDao.set(userId, id, origin, screen, interactionTime, type);
        }
        return false;
    }
}
