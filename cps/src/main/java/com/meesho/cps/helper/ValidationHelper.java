package com.meesho.cps.helper;

import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.AdViewEvent;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;

import java.util.Objects;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
public class ValidationHelper {

    public static Boolean isValidAdInteractionEvent(AdInteractionEvent adInteractionEvent) {
        return Objects.nonNull(adInteractionEvent.getUserId()) && Objects.nonNull(adInteractionEvent.getProperties()) &&
                Objects.nonNull(adInteractionEvent.getProperties().getId()) &&
                Objects.nonNull(adInteractionEvent.getEventTimestamp()) &&
                Objects.nonNull(adInteractionEvent.getProperties().getType());
    }

    public static Boolean isValidAdViewEvent(AdViewEvent adViewEvent) {
        return Objects.nonNull(adViewEvent.getUserId()) && Objects.nonNull(adViewEvent.getProperties()) &&
                Objects.nonNull(adViewEvent.getProperties().getId());
    }

    public static boolean isValidCampaignCatalogDateMetricsDocument(CampaignCatalogDateMetrics document) {
        return Objects.nonNull(document) && Objects.nonNull(document.getSupplierId()) && Objects.nonNull(document.getCampaignId())
                && Objects.nonNull(document.getCatalogId()) && Objects.nonNull(document.getDate());
    }

    public static boolean isValidViewCountIncrement(CampaignCatalogViewCount viewCount) {
        return Objects.nonNull(viewCount) && Objects.nonNull(viewCount.getSupplierId())
                && Objects.nonNull(viewCount.getCampaignId()) && Objects.nonNull(viewCount.getCatalogId())
                && Objects.nonNull(viewCount.getDate());
    }

    public static boolean isValidOrdersAndRevenueUpdate(CampaignCatalogDateMetrics document) {
        return isValidCampaignCatalogDateMetricsDocument(document) && Objects.nonNull(document.getOrders())
                && Objects.nonNull(document.getRevenue());
    }

}
