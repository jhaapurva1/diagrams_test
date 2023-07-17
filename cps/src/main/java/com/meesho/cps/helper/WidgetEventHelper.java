package com.meesho.cps.helper;

import com.meesho.ad.client.constants.FeedType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.AdWidgets;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class WidgetEventHelper {

    @Value(AdWidgets.TOP_OF_SEARCH_CPC_MULTIPLIER)
    private BigDecimal topOfSearchCpcMultiplier;

    @Value(AdWidgets.PDP_CPC_MULTIPLIER)
    private BigDecimal pdpCpcMultiplier;

    @Getter
    private String screen;

    @Getter
    private String origin;

    @Getter
    private BigDecimal cpcMultiplier;

    @Getter
    private String feedType;

    @Getter
    private FeedType nonNativeFeedType;

    public WidgetEventHelper() {
        initMembersWithDefaults();
    }

    public void setContext(AdWidgetClickEvent adWidgetClickEvent) {
        initMembers(adWidgetClickEvent);
    }

    private void initMembers(AdWidgetClickEvent adWidgetClickEvent) {
        String realEstate =
                Objects.nonNull(adWidgetClickEvent.getProperties()) ? adWidgetClickEvent.getProperties()
                        .getSourceScreen() : null;
        if (Boolean.TRUE.equals(isTopOfSearchRealEstate(realEstate))) {
            initMembersForTos(adWidgetClickEvent);
        } else if (Boolean.TRUE.equals(isPdpRealEstate(realEstate))) {
            initMembersForPdp(adWidgetClickEvent);
        } else {
            initMembersWithDefaults();
        }
    }

    private void initMembersForTos(AdWidgetClickEvent adWidgetClickEvent) {
        feedType = FeedType.TEXT_SEARCH.getValue();
        nonNativeFeedType = FeedType.TOP_OF_SEARCH;
        origin = Constants.AdWidgets.ORIGIN_SEARCH;
        cpcMultiplier = topOfSearchCpcMultiplier;
        if (Objects.nonNull(adWidgetClickEvent.getProperties().getWidgetGroupPosition())
                && adWidgetClickEvent.getProperties().getWidgetGroupPosition() > 1) {
            screen = String.format(Constants.AdWidgets.SCREEN_MID_FEED_SEARCH,
                    adWidgetClickEvent.getProperties().getWidgetGroupPosition());
        } else {
            screen = Constants.AdWidgets.SCREEN_TOP_OF_SEARCH;
        }
    }

    private void initMembersForPdp(AdWidgetClickEvent adWidgetClickEvent) {
        feedType = FeedType.PRODUCT_RECO.getValue();
        nonNativeFeedType = FeedType.ADS_ON_PDP;
        origin = AdWidgets.ORIGIN_PDP;
        cpcMultiplier = pdpCpcMultiplier;
        screen = String.format(AdWidgets.SCREEN_PDP, adWidgetClickEvent.getProperties().getWidgetGroupPosition());
    }

    private void initMembersWithDefaults() {
        feedType = null;
        origin = null;
        cpcMultiplier = BigDecimal.ONE;
        screen = null;
        nonNativeFeedType = FeedType.UNKNOWN;
    }

    public static Boolean isTopOfSearchRealEstate(String realEstate) {
        return Objects.nonNull(realEstate) && realEstate.equals(AdWidgetRealEstates.TEXT_SEARCH);
    }

    public static Boolean isPdpRealEstate(String realEstate) {
        return Objects.nonNull(realEstate) && realEstate.equals(AdWidgetRealEstates.PDP);
    }

}
