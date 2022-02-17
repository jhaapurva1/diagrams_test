package com.meesho.cps.transformer;

import com.meesho.ads.lib.utils.Utils;
import com.meesho.commons.utils.DateUtils;
import com.meesho.cps.data.entity.elasticsearch.ESDailyIndexDocument;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.kafka.DayWisePerformancePrismEvent;
import com.meesho.cps.utils.DateTimeHelper;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public class PrismEventTransformer {

    public static AdInteractionPrismEvent getAdInteractionPrismEvent(AdInteractionEvent adInteractionEvent,
                                                                     String userId, Long catalogId) {
        return AdInteractionPrismEvent.builder()
                .eventId(adInteractionEvent.getEventId())
                .eventName(adInteractionEvent.getEventName())
                .catalogId(catalogId)
                .userId(userId)
                .interactionType(adInteractionEvent.getProperties().getType())
                .eventTimestamp(adInteractionEvent.getEventTimestamp())
                .eventTimeIso(adInteractionEvent.getEventTimeIso())
                .appVersionCode(adInteractionEvent.getProperties().getAppVersionCode())
                .origin(adInteractionEvent.getProperties().getOrigin())
                .screen(adInteractionEvent.getProperties().getScreen())
                .currentTimestamp(DateUtils.toIsoString(ZonedDateTime.now(), Utils.getCountry()))
                .build();
    }

    public static List<DayWisePerformancePrismEvent> getDayWisePerformanceEvent(List<ESDailyIndexDocument> documents) {
        List<DayWisePerformancePrismEvent> events = new ArrayList<>();
        documents.forEach(document -> {
            events.add(DayWisePerformancePrismEvent.builder()
                    .budgetUtilised(document.getBudgetUtilised())
                    .campaignId(document.getCampaignId())
                    .clicks(document.getClicks())
                    .catalogId(document.getCatalogId())
                    .currentTimestamp(LocalDateTime.now().format(DateTimeHelper.dateTimeFormat))
                    .date(document.getDate())
                    .eventId(document.getId())
                    .orders(document.getOrders())
                    .revenue(document.getRevenue())
                    .shares(document.getShares())
                    .wishlist(document.getWishlist())
                    .views(document.getViews())
                    .build());
        });
        return events;
    }

    public static List<DayWisePerformancePrismEvent> getDayWisePerformancePrismEvent(
            List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList) {
        List<DayWisePerformancePrismEvent> events = new ArrayList<>();
        campaignCatalogDateMetricsList.forEach(ccd -> {
            events.add(DayWisePerformancePrismEvent.builder()
                    .budgetUtilised(ccd.getBudgetUtilised())
                    .campaignId(ccd.getCampaignId())
                    .clicks(ccd.getClickCount())
                    .catalogId(ccd.getCatalogId())
                    .currentTimestamp(LocalDateTime.now().format(DateTimeHelper.dateTimeFormat))
                    .date(ccd.getDate().toString())
                    .orders(ccd.getOrders())
                    .revenue(ccd.getRevenue())
                    .shares(ccd.getSharesCount())
                    .wishlist(ccd.getWishlistCount())
                    .views(ccd.getViewCount())
                    .eventId(ccd.getCampaignId() + "_" + ccd.getCatalogId() + "_" + ccd.getDate())
                    .build());
        });
        return events;
    }

}
