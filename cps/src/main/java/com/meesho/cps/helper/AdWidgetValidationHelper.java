package com.meesho.cps.helper;

import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import org.apache.commons.collections.CollectionUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AdWidgetValidationHelper {

    private static final Set<String> VALID_REAL_ESTATES = new HashSet<>(
        Arrays.asList(AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP_RECO));

    public static Boolean isValidAdWidgetViewEvent(AdWidgetViewEvent adWidgetViewEvent) {
        return Objects.nonNull(adWidgetViewEvent.getEventName()) &&
                Objects.nonNull(adWidgetViewEvent.getProperties()) &&
                Objects.nonNull(adWidgetViewEvent.getEventId()) &&
                Objects.nonNull(adWidgetViewEvent.getEventTimestamp()) &&
                Objects.nonNull(adWidgetViewEvent.getUserId()) &&
                CollectionUtils.isNotEmpty(adWidgetViewEvent.getProperties().getCampaignIds()) &&
                CollectionUtils.isNotEmpty(adWidgetViewEvent.getProperties().getCatalogIds()) &&
                Objects.nonNull(adWidgetViewEvent.getProperties().getAppVersionCode()) &&
                CollectionUtils.isNotEmpty(adWidgetViewEvent.getProperties().getPrimaryRealEstates());
    }

    public static Boolean isValidWidgetRealEstate(String primaryRealEstate) {
        return VALID_REAL_ESTATES.contains(primaryRealEstate);
    }

    public static Boolean isValidAdWidgetClickEvent(AdWidgetClickEvent adWidgetClickEvent) {
        return Objects.nonNull(adWidgetClickEvent.getEventName()) &&
                Objects.nonNull(adWidgetClickEvent.getProperties()) &&
                Objects.nonNull(adWidgetClickEvent.getEventId()) &&
                Objects.nonNull(adWidgetClickEvent.getEventTimestamp()) &&
                Objects.nonNull(adWidgetClickEvent.getUserId()) &&
                Objects.nonNull(adWidgetClickEvent.getProperties().getCampaignId()) &&
                Objects.nonNull(adWidgetClickEvent.getProperties().getCatalogId()) &&
                Objects.nonNull(adWidgetClickEvent.getProperties().getAppVersionCode()) &&
                Objects.nonNull(adWidgetClickEvent.getProperties().getIsAdWidget()) &&
                Objects.nonNull(adWidgetClickEvent.getProperties().getPrimaryRealEstate());
    }
}
