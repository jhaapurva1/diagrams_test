package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.AdWidgets;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.enums.FeedType;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;


@Slf4j
@Getter
@Setter
public class WidgetEventHelper {

    @Value(AdWidgets.TOP_OF_SEARCH_CPC_MULTIPLIER)
    private BigDecimal topOfSearchCpcMultiplier;

    @Value(AdWidgets.PDP_RECO_CPC_MULTIPLIER)
    private BigDecimal pdpRecoCpcMultiplier;

    private String screen;
    private String origin;
    private BigDecimal cpcMultiplier;
    private String feedType;

    public WidgetEventHelper(AdWidgetClickEvent adWidgetClickEvent) {
        initMembers(adWidgetClickEvent);
    }

    private void initMembers(AdWidgetClickEvent adWidgetClickEvent) {
        if (Objects.nonNull(adWidgetClickEvent.getProperties())) {
            String realEstate = adWidgetClickEvent.getProperties().getPrimaryRealEstate();
            if (Boolean.TRUE.equals(isTopOfSearchRealEstate(realEstate))) {
                initMembersForTos(adWidgetClickEvent);
            } else if (Boolean.TRUE.equals(isPdpRecoRealEstate(realEstate))) {
                initMembersForPdpReco(adWidgetClickEvent);
            }
        } else {
            initMembersWithDefaults();
        }
    }

    private void initMembersForTos(AdWidgetClickEvent adWidgetClickEvent) {
        feedType = FeedType.TEXT_SEARCH.getValue();
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

    private void initMembersForPdpReco(AdWidgetClickEvent adWidgetClickEvent) {
        feedType = FeedType.PRODUCT_RECO.getValue();
        origin = AdWidgets.ORIGIN_PDP_RECO;
        cpcMultiplier = pdpRecoCpcMultiplier;
        screen = AdWidgets.SCREEN_PDP_RECO;
    }

    private void initMembersWithDefaults() {
        feedType = null;
        origin = null;
        cpcMultiplier = BigDecimal.ONE;
        screen = null;
    }

    public static Boolean isTopOfSearchRealEstate(String realEstate) {
        return Objects.nonNull(realEstate) && realEstate.equals(AdWidgetRealEstates.TEXT_SEARCH);
    }

    public static Boolean isPdpRecoRealEstate(String realEstate) {
        return Objects.nonNull(realEstate) && realEstate.equals(AdWidgetRealEstates.PDP_RECO);
    }
}
