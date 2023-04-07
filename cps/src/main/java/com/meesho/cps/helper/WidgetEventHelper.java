package com.meesho.cps.helper;

import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import java.math.BigDecimal;

public interface WidgetEventHelper {

    default String getFeedType() {
        return null;
    }

    default String getScreen(AdWidgetClickEvent adWidgetClickEvent) {
        return null;
    }

    default String getScreen(AdWidgetViewEvent adWidgetViewEvent) {
        return null;
    }

    default String getOrigin(AdWidgetClickEvent adWidgetClickEvent) {
        return null;
    }

    default String getOrigin(AdWidgetViewEvent adWidgetViewEvent) {
        return null;
    }

    default BigDecimal getCpcMultiplier() {
        return BigDecimal.ONE;
    }

}
