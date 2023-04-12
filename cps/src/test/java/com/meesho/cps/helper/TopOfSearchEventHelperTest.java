package com.meesho.cps.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.AdWidgets;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent.Properties;
import com.meesho.cps.enums.FeedType;
import java.math.BigDecimal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class TopOfSearchEventHelperTest {

    @InjectMocks
    TopOfSearchEventHelper topOfSearchEventHelper;

    private AdWidgetClickEvent adWidgetClickEvent;

    @Before
    public void setUp() throws JsonProcessingException {
        ReflectionTestUtils.setField(topOfSearchEventHelper, "topOfSearchCpcMultiplier",
            BigDecimal.TEN);
        adWidgetClickEvent = AdWidgetClickEvent.builder().build();
    }

    @Test
    public void testForGetFeedType() {
        Assert.assertEquals(FeedType.TEXT_SEARCH.getValue(), topOfSearchEventHelper.getFeedType());
    }


    @Test
    public void testForGetCpcMultiplier() {
        Assert.assertEquals(BigDecimal.TEN, topOfSearchEventHelper.getCpcMultiplier());
    }

    @Test
    public void testForGetOriginForClick() {
        Assert.assertEquals(Constants.AdWidgets.ORIGIN_SEARCH,
            topOfSearchEventHelper.getOrigin(adWidgetClickEvent));
    }

    @Test
    public void testForGetScreenWhenPositionIsGreaterThanOneForClick() {
        adWidgetClickEvent.setProperties(Properties.builder().widgetGroupPosition(2).build());
        Assert.assertEquals(String.format(Constants.AdWidgets.SCREEN_MID_FEED_SEARCH,
                adWidgetClickEvent.getProperties().getWidgetGroupPosition()),
            topOfSearchEventHelper.getScreen(adWidgetClickEvent));
        adWidgetClickEvent.setProperties(null);
    }

    @Test
    public void testForGetScreenWhenPositionIsLessThanTwoForClick() {
        adWidgetClickEvent.setProperties(Properties.builder().widgetGroupPosition(1).build());
        Assert.assertEquals(AdWidgets.SCREEN_TOP_OF_SEARCH,
            topOfSearchEventHelper.getScreen(adWidgetClickEvent));
        adWidgetClickEvent.setProperties(null);
    }

    @Test
    public void testForGetScreenWhenPositionIsNullForClick() {
        adWidgetClickEvent.setProperties(Properties.builder().build());
        Assert.assertEquals(AdWidgets.SCREEN_TOP_OF_SEARCH,
            topOfSearchEventHelper.getScreen(adWidgetClickEvent));
        adWidgetClickEvent.setProperties(null);
    }

    @Test
    public void testForGetScreenWhenPropertyIsNull() {
        Assert.assertEquals(AdWidgets.SCREEN_TOP_OF_SEARCH,
            topOfSearchEventHelper.getScreen(adWidgetClickEvent));
    }
}
