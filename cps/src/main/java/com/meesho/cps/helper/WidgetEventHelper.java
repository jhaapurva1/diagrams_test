package com.meesho.cps.helper;

import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import java.math.BigDecimal;

public interface WidgetEventHelper {

    String getFeedType();

    String getScreen(AdWidgetClickEvent adWidgetClickEvent);

    String getScreen(AdWidgetViewEvent adWidgetViewEvent);

    String getOrigin(AdWidgetClickEvent adWidgetClickEvent);

    String getOrigin(AdWidgetViewEvent adWidgetViewEvent);

    BigDecimal getCpcMultiplier();

}
