package com.meesho.cps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.ads.lib.utils.Utils;
import com.meesho.commons.utils.DateUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.*;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.BudgetExhaustedEvent;
import com.meesho.cps.data.entity.kafka.SupplierWeeklyBudgetExhaustedEvent;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.enums.FeedType;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.AdWidgetValidationHelper;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.helper.ClickAttributionHelper;
import com.meesho.cps.helper.ValidationHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cps.transformer.PrismEventTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
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
    PrismService prismService;

    @Autowired
    private CampaignPerformanceHelper campaignHelper;

    @Autowired
    private ClickAttributionHelper clickAttributionHelper;

    @Autowired
    private AdBillFactory adBillFactory;

    @Autowired
    private CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private UserCatalogInteractionCacheDao userCatalogInteractionCacheDao;

    public void handle(AdWidgetClickEvent adWidgetClickEvent) throws ExternalRequestFailedException {
        // TODO: remove log line before pushing
        log.info("processing widget click event: {}", adWidgetClickEvent);

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

        AdInteractionPrismEvent adInteractionPrismEvent =
                PrismEventTransformer.getInteractionEventForWidgetClick(adWidgetClickEvent, userId, catalogId);


        // set feedType
        String feedType = FeedType.TEXT_SEARCH.getValue();

        List<Long> catalogIds = Objects.isNull(catalogId) ? null : Collections.singletonList(catalogId);
        List<Long> campaignIds = Objects.isNull(campaignId) ? null : Collections.singletonList(campaignId);

        CampaignCatalogMetadataResponse campaignCatalogMetadataResponse =
                adService.getCampaignCatalogMetadata(catalogIds, campaignIds, userId, feedType);
        log.info("campaign catalog metadata: {}", campaignCatalogMetadataResponse);
        List<CampaignCatalogMetadataResponse.CatalogMetadata> catalogMetadataList = campaignCatalogMetadataResponse.getCampaignDetailsList();
        List<CampaignCatalogMetadataResponse.SupplierMetadata> supplierMetadataList = campaignCatalogMetadataResponse.getSupplierDetailsList();

        // Check if catalog metadata is empty
        if (CollectionUtils.isEmpty(catalogMetadataList) ||
                Objects.isNull(catalogMetadataList.get(0).getCampaignDetails())) {
            log.error("No active ad on catalogId {}", catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.CAMPAIGN_INACTIVE);
            clickAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.CAMPAIGN_INACTIVE.name());
            return;
        }

        CampaignDetails catalogMetadata = catalogMetadataList.get(0).getCampaignDetails();
        CampaignCatalogMetadataResponse.SupplierMetadata supplierMetadata = supplierMetadataList.get(0);

        BigDecimal totalBudget = catalogMetadata.getBudget();
        Integer billVersion = catalogMetadata.getBillVersion();
        CampaignType campaignType = CampaignType.fromValue(catalogMetadata.getCampaignType());
        campaignId = catalogMetadata.getCampaignId();
        BigDecimal cpc = catalogMetadata.getCpc();
        LocalDate eventDate = campaignHelper.getLocalDateForDailyCampaignFromLocalDateTime(
                DateTimeUtils.getCurrentLocalDateTimeInIST());
        Long supplierId = supplierMetadata.getSupplierId();
        BigDecimal weeklyBudgetUtilisationLimit = supplierMetadata.getUtilizationBudget();
        LocalDate weekStartDate = DateTimeUtils.getFirstDayOfWeek().toLocalDate();

        log.info("CPC for event_id {} catalog id {} in campaign {} is {}",
                adWidgetClickEvent.getEventId(), catalogId, campaignId, cpc);
        adInteractionPrismEvent.setCampaignId(campaignId);
        adInteractionPrismEvent.setCpc(cpc);

        BillHandler billHandler = adBillFactory.getBillHandlerForBillVersion(billVersion);

        //TODO : Check if event is valid for the bill version of campaign

        if (clickAttributionHelper.initialiseAndCheckIsBudgetExhausted(catalogMetadata, weekStartDate, eventDate, weeklyBudgetUtilisationLimit, catalogId)) {
            log.error("Budget exhausted for catalogId {}", catalogId);
            adInteractionPrismEvent.setStatus(AdInteractionStatus.INVALID);
            adInteractionPrismEvent.setReason(AdInteractionInvalidReason.BUDGET_EXHAUSTED);
            clickAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.BUDGET_EXHAUSTED.name());
            return;
        }

        //Perform deduplication
        if(performDedup(billHandler, adWidgetClickEvent, adInteractionPrismEvent, userId, interactionTime)) {
            clickAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                    adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                    AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.DUPLICATE.name());
            return;
        }

        //Update campaign catalog date metrics
        log.info("campaignId {}, catalogId {}, date{}, eventName {}", campaignId, catalogId, eventDate, "AdWidgetClickEvent");
        campaignCatalogDateMetricsRepository.incrementClickCount(campaignId, catalogId, eventDate);

        // Update budget utilised
        BigDecimal budgetUtilised = clickAttributionHelper.modifyAndGetBudgetUtilised(cpc, campaignId, catalogId, eventDate, campaignType);
        if (budgetUtilised.compareTo(totalBudget) >= 0) {
            clickAttributionHelper.sendBudgetExhaustedEvent(campaignId, catalogId);
        }

        //update supplier weekly budget utilised
        BigDecimal supplierWeeklyBudgetUtilised = clickAttributionHelper.modifyAndGetSupplierWeeklyBudgetUtilised(supplierId, weekStartDate, cpc);
        if (Objects.nonNull(weeklyBudgetUtilisationLimit) && supplierWeeklyBudgetUtilised.compareTo(weeklyBudgetUtilisationLimit) >= 0) {
            clickAttributionHelper.sendSupplierBudgetExhaustedEvent(supplierId, catalogId);
        }

        updatedCampaignCatalogCacheDao.add(Arrays.asList(new CampaignCatalogDate(campaignId, catalogId, eventDate.toString())));

        // publish instrumentation event and metrics
        adInteractionPrismEvent.setStatus(AdInteractionStatus.VALID);
        clickAttributionHelper.publishPrismEvent(adInteractionPrismEvent);

        telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
        int cpcNormalised = cpc.multiply(BigDecimal.valueOf(100)).intValue();
        telegrafMetricsHelper.increment(INTERACTION_EVENT_CPC_KEY, cpcNormalised, INTERACTION_EVENT_CPC_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin());

    }

    private boolean performDedup(BillHandler billHandler, AdWidgetClickEvent adWidgetClickEvent,
                              AdInteractionPrismEvent adInteractionPrismEvent, String userId, Long interactionTime) {
        log.info("perform dedup: {} {} {} {} {}", billHandler, adWidgetClickEvent, adWidgetClickEvent, userId, interactionTime);

        // Populating the ORIGIN and SCREEN as per the product requirements: https://docs.google.com/spreadsheets/d/1WOY4CGfMnn5aGgA8kAYLQfU12t6C8dztpF2UhgGKY2E/edit?usp=sharing
        if(adWidgetClickEvent.getProperties().getWidgetGroupPosition() > 1) {
            adWidgetClickEvent.getProperties().setScreen(Constants.AdWidgets.SCREEN_MID_FEED_SEARCH);
        } else {
            adWidgetClickEvent.getProperties().setScreen(Constants.AdWidgets.SCREEN_TOP_OF_SEARCH);
        }
        adWidgetClickEvent.getProperties().setOrigin(Constants.DefaultRealEstateMetaData.ORIGIN);
        String origin = adWidgetClickEvent.getProperties().getOrigin();
        String screen = adWidgetClickEvent.getProperties().getScreen();
        adInteractionPrismEvent.setOrigin(origin);
        adInteractionPrismEvent.setScreen(screen);

        if (billHandler.performWindowDeDuplication()) {
            Long id = adWidgetClickEvent.getProperties().getCatalogId();
            AdUserInteractionType type = AdUserInteractionType.CATALOG_ID;
                Long previousInteractionTime = userCatalogInteractionCacheDao.get(userId, id, origin, screen, type);
            if (!clickAttributionHelper.checkIfInteractionNeedsToBeConsidered(previousInteractionTime, interactionTime)) {
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
