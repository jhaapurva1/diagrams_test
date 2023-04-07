package com.meesho.cps.helper;

import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import java.math.BigDecimal;


public class WidgetEventHelperDummy implements WidgetEventHelper {

    @Override
    public String getFeedType() {
        return null;
    }

    @Override
    public String getScreen(AdWidgetClickEvent adWidgetClickEvent) {
        return null;
    }

    @Override
    public String getScreen(AdWidgetViewEvent adWidgetViewEvent) {
        return null;
    }

    @Override
    public String getOrigin(AdWidgetClickEvent adWidgetClickEvent) {
        return null;
    }

    @Override
    public String getOrigin(AdWidgetViewEvent adWidgetViewEvent) {
        return null;
    }

    @Override
    public BigDecimal getCpcMultiplier() {
        return null;
    }
}
