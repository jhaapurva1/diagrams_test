package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.AdWidgets;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.constants.PdpWidgetPosition;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.enums.FeedType;
import java.math.BigDecimal;
import java.util.Objects;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WidgetEventHelper {

    @Value(AdWidgets.TOP_OF_SEARCH_CPC_MULTIPLIER)
    private BigDecimal topOfSearchCpcMultiplier;

    @Value(AdWidgets.PDP_RECO_CPC_MULTIPLIER)
    private BigDecimal pdpRecoCpcMultiplier;

    @Getter
    private String screen;

    @Getter
    private String origin;

    @Getter
    private BigDecimal cpcMultiplier;

    @Getter
    private String feedType;

    public WidgetEventHelper(){
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
        } else if (Boolean.TRUE.equals(isPdpRecoRealEstate(realEstate))) {
            initMembersForPdpReco(adWidgetClickEvent);
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
        PdpWidgetPosition pdpWidgetPosition = null;
        if (Objects.nonNull(adWidgetClickEvent.getProperties().getWidgetGroupPosition())) {
            int widgetGroupPosition = adWidgetClickEvent.getProperties().getWidgetGroupPosition();
            pdpWidgetPosition = PdpWidgetPosition.fromPositionNumber(widgetGroupPosition);
        }
        if (Objects.nonNull(pdpWidgetPosition)) {
            screen = pdpWidgetPosition.positionName();
        } else {
            screen = null;
        }
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
        return Objects.nonNull(realEstate) && realEstate.equals(AdWidgetRealEstates.PDP);
    }
}
