package com.meesho.cps.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.AdWidgets;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.constants.PdpWidgetPosition;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent.Properties;
import com.meesho.cps.enums.FeedType;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
class WidgetEventHelperTest {

    @Test
    void testInitMembersForTos() {
        AdWidgetClickEvent adWidgetClickEvent = AdWidgetClickEvent.builder().properties(
                Properties.builder().primaryRealEstate(AdWidgetRealEstates.TEXT_SEARCH).build())
            .build();

        WidgetEventHelper widgetEventHelper = new WidgetEventHelper(adWidgetClickEvent);

        assertEquals(FeedType.TEXT_SEARCH.getValue(), widgetEventHelper.getFeedType());
        assertEquals(Constants.AdWidgets.ORIGIN_SEARCH, widgetEventHelper.getOrigin());
        assertEquals(AdWidgets.SCREEN_TOP_OF_SEARCH, widgetEventHelper.getScreen());
    }

    @Test
    void testInitMembersForPdpForValidWidgetPosition() {
        AdWidgetClickEvent adWidgetClickEvent = AdWidgetClickEvent.builder().properties(
            Properties.builder().primaryRealEstate(AdWidgetRealEstates.PDP_RECO)
                .widgetGroupPosition(6).build()).build();

        WidgetEventHelper widgetEventHelper = new WidgetEventHelper(adWidgetClickEvent);
        assertEquals(FeedType.PRODUCT_RECO.getValue(), widgetEventHelper.getFeedType());
        assertEquals(AdWidgets.ORIGIN_PDP_RECO, widgetEventHelper.getOrigin());
        assertEquals(PdpWidgetPosition.AFTER_PRODUCT_DETAILS.positionName(),
            widgetEventHelper.getScreen());
    }

    @Test
    void testInitMembersForPdpForInvalidWidgetPosition() {
        AdWidgetClickEvent adWidgetClickEvent = AdWidgetClickEvent.builder().properties(
            Properties.builder().primaryRealEstate(AdWidgetRealEstates.PDP_RECO)
                .widgetGroupPosition(6).build()).build();

        WidgetEventHelper widgetEventHelper = new WidgetEventHelper(adWidgetClickEvent);
        assertEquals(FeedType.PRODUCT_RECO.getValue(), widgetEventHelper.getFeedType());
        assertEquals(AdWidgets.ORIGIN_PDP_RECO, widgetEventHelper.getOrigin());
        assertEquals(AdWidgets.SCREEN_PDP_RECO, widgetEventHelper.getScreen());
    }
}