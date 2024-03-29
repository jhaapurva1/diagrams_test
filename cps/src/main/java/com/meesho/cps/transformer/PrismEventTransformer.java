package com.meesho.cps.transformer;

import com.meesho.ads.lib.utils.Utils;
import com.meesho.commons.utils.DateUtils;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.DayWisePerformancePrismEvent;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.utils.DateTimeHelper;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public class PrismEventTransformer {

    public static AdInteractionPrismEvent getAdInteractionPrismEvent(AdInteractionEvent adInteractionEvent,
                                                                     String userId, Long catalogId, Long productId) {
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
                .productId(productId)
                .build();
    }

    public static AdInteractionPrismEvent getInteractionEventForWidgetClick(AdWidgetClickEvent adWidgetClickEvent, String userId, Long catalogId) {
        return AdInteractionPrismEvent.builder().eventId(adWidgetClickEvent.getEventId())
                .eventName(adWidgetClickEvent.getEventName())
                .catalogId(catalogId)
                .userId(userId)
                .interactionType("catalog") // Check for the interaction type in the presto db table - check an event in logs
                .eventTimestamp(adWidgetClickEvent.getEventTimestamp())
                .eventTimeIso(adWidgetClickEvent.getEventTimeIso())
                .appVersionCode(adWidgetClickEvent.getProperties().getAppVersionCode())
                .origin(adWidgetClickEvent.getProperties().getOrigin()) // set origin to new value as per product requirements
                .screen(adWidgetClickEvent.getProperties().getScreen()) // set screen to new value as per product requirements
                .currentTimestamp(DateUtils.toIsoString(ZonedDateTime.now(), Utils.getCountry()))
                .build();
    }

    public static List<DayWisePerformancePrismEvent> getDayWisePerformancePrismEvent(
            List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList) {
        List<DayWisePerformancePrismEvent> events = new ArrayList<>();
        campaignCatalogDateMetricsList.forEach(ccd -> {
            events.add(DayWisePerformancePrismEvent.builder()
                    .budgetUtilised(ccd.getBudgetUtilised())
                    .campaignId(ccd.getCampaignId())
                    .clicks(ccd.getClicks())
                    .catalogId(ccd.getCatalogId())
                    .currentTimestamp(LocalDateTime.now().format(DateTimeHelper.dateTimeFormat))
                    .date(ccd.getDate().toString())
                    .orders(ccd.getOrders())
                    .revenue(ccd.getRevenue())
                    .shares(ccd.getShares())
                    .wishlist(ccd.getWishlists())
                    .views(ccd.getViews())
                    .eventId(ccd.getCampaignId() + "_" + ccd.getCatalogId() + "_" + ccd.getDate())
                    .build());
        });
        return events;
    }

}
