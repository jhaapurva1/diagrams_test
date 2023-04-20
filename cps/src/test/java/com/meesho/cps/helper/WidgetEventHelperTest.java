package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.AdWidgets;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent.Properties;
import com.meesho.cps.enums.FeedType;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

@RunWith(MockitoJUnitRunner.class)
class WidgetEventHelperTest {

    @Test
    void testInitMembersForTos() {
        AdWidgetClickEvent adWidgetClickEvent = AdWidgetClickEvent.builder().properties(
                        Properties.builder().sourceScreen(AdWidgetRealEstates.TEXT_SEARCH).build())
                .build();

        WidgetEventHelper widgetEventHelper = new WidgetEventHelper();
        widgetEventHelper.setContext(adWidgetClickEvent);
        assertEquals(FeedType.TEXT_SEARCH.getValue(), widgetEventHelper.getFeedType());
        assertEquals(Constants.AdWidgets.ORIGIN_SEARCH, widgetEventHelper.getOrigin());
        assertEquals(AdWidgets.SCREEN_TOP_OF_SEARCH, widgetEventHelper.getScreen());
    }

    @Test
    void testInitMembersForPdp() {
        AdWidgetClickEvent adWidgetClickEvent = AdWidgetClickEvent.builder().properties(
                Properties.builder().sourceScreen(AdWidgetRealEstates.PDP)
                        .widgetGroupPosition(5).build()).build();

        WidgetEventHelper widgetEventHelper = new WidgetEventHelper();
        widgetEventHelper.setContext(adWidgetClickEvent);
        assertEquals(FeedType.PRODUCT_RECO.getValue(), widgetEventHelper.getFeedType());
        assertEquals(AdWidgets.ORIGIN_PDP, widgetEventHelper.getOrigin());
        assertEquals(String.format(AdWidgets.SCREEN_PDP,
                        adWidgetClickEvent.getProperties().getWidgetGroupPosition()),
                widgetEventHelper.getScreen());
    }
}