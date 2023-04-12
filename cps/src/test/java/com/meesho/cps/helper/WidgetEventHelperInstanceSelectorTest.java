package com.meesho.cps.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent.Properties;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class WidgetEventHelperInstanceSelectorTest {

    @InjectMocks
    private WidgetEventHelperInstanceSelector widgetEventHelperInstanceSelector;

    @Mock
    private PdpRecoEventHelper pdpRecoEventHelper;
    @Mock
    private TopOfSearchEventHelper topOfSearchEventHelper;
    @Mock
    private WidgetEventHelperDummy widgetEventHelperDummy;

    @Before
    public void setUp() throws JsonProcessingException {
        ReflectionTestUtils.setField(widgetEventHelperInstanceSelector, "topOfSearchEventHelper",
            topOfSearchEventHelper);
        ReflectionTestUtils.setField(widgetEventHelperInstanceSelector, "pdpRecoEventHelper",
            pdpRecoEventHelper);
        ReflectionTestUtils.setField(widgetEventHelperInstanceSelector, "widgetEventHelperDummy",
            widgetEventHelperDummy);
    }

    @Test
    public void testForGetWidgetEventHelperInstanceForPdpRecoClick() {
        AdWidgetClickEvent adWidgetClickEvent = AdWidgetClickEvent.builder().properties(
            Properties.builder().primaryRealEstate(AdWidgetRealEstates.PDP_RECO).build()).build();
        WidgetEventHelper widgetEventHelper = widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(
            adWidgetClickEvent);
        Assert.assertTrue(widgetEventHelper instanceof PdpRecoEventHelper);
    }

    @Test
    public void testForGetWidgetEventHelperInstanceForTosClick() {
        AdWidgetClickEvent adWidgetClickEvent = AdWidgetClickEvent.builder().properties(
                Properties.builder().primaryRealEstate(AdWidgetRealEstates.TEXT_SEARCH).build())
            .build();
        WidgetEventHelper widgetEventHelper = widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(
            adWidgetClickEvent);
        Assert.assertTrue(widgetEventHelper instanceof TopOfSearchEventHelper);
    }

    @Test
    public void testForGetWidgetEventHelperInstanceForNoMatchClick() {
        AdWidgetClickEvent adWidgetClickEvent = AdWidgetClickEvent.builder().properties(null)
            .build();
        WidgetEventHelper widgetEventHelper = widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(
            adWidgetClickEvent);
        Assert.assertTrue(widgetEventHelper instanceof WidgetEventHelperDummy);
    }

    @Test
    public void testForGetWidgetEventHelperInstanceForPdpRecoView() {
        AdWidgetViewEvent adWidgetViewEvent = AdWidgetViewEvent.builder().properties(
            AdWidgetViewEvent.Properties.builder()
                .primaryRealEstates(Collections.singletonList(AdWidgetRealEstates.PDP_RECO))
                .build()).build();
        widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(adWidgetViewEvent);
        WidgetEventHelper widgetEventHelper = widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(
            adWidgetViewEvent);
        Assert.assertTrue(widgetEventHelper instanceof PdpRecoEventHelper);
    }

    @Test
    public void testForGetWidgetEventHelperInstanceForTosView() {
        AdWidgetViewEvent adWidgetViewEvent = AdWidgetViewEvent.builder().properties(
            AdWidgetViewEvent.Properties.builder()
                .primaryRealEstates(Collections.singletonList(AdWidgetRealEstates.TEXT_SEARCH))
                .build()).build();
        widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(adWidgetViewEvent);
        WidgetEventHelper widgetEventHelper = widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(
            adWidgetViewEvent);
        Assert.assertTrue(widgetEventHelper instanceof TopOfSearchEventHelper);

    }

    @Test
    public void testForGetWidgetEventHelperInstanceForNoMatchView() {
        AdWidgetViewEvent adWidgetViewEvent = AdWidgetViewEvent.builder()
            .properties(AdWidgetViewEvent.Properties.builder().primaryRealEstates(null).build())
            .build();
        widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(adWidgetViewEvent);
        WidgetEventHelper widgetEventHelper = widgetEventHelperInstanceSelector.getWidgetEventHelperInstance(
            adWidgetViewEvent);
        Assert.assertTrue(widgetEventHelper instanceof WidgetEventHelperDummy);

    }
}
