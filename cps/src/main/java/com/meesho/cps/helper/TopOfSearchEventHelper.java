package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.AdWidgets;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.enums.FeedType;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;

public class TopOfSearchEventHelper implements WidgetEventHelper {

    @Value(AdWidgets.TOP_OF_SEARCH_CPC_MULTIPLIER)
    private BigDecimal topOfSearchCpcMultiplier;

    @Override
    public String getFeedType() {
        return FeedType.TEXT_SEARCH.getValue();
    }

    // Getting the ORIGIN and SCREEN as per the product requirements:
    // https://docs.google.com/spreadsheets/d/1WOY4CGfMnn5aGgA8kAYLQfU12t6C8dztpF2UhgGKY2E/edit?usp=sharing
    @Override
    public String getScreen(AdWidgetClickEvent adWidgetClickEvent) {
        String screen = null;
        if (Objects.nonNull(adWidgetClickEvent.getProperties().getWidgetGroupPosition())
            && adWidgetClickEvent.getProperties().getWidgetGroupPosition() > 1) {
            screen = String.format(Constants.AdWidgets.SCREEN_MID_FEED_SEARCH,
                adWidgetClickEvent.getProperties().getWidgetGroupPosition());
        } else {
            screen = Constants.AdWidgets.SCREEN_TOP_OF_SEARCH;
        }
        return screen;
    }

    @Override
    public String getOrigin(AdWidgetClickEvent adWidgetClickEvent) {
        return Constants.AdWidgets.ORIGIN_SEARCH;
    }

    @Override
    public BigDecimal getCpcMultiplier() {
        return topOfSearchCpcMultiplier;
    }
}
