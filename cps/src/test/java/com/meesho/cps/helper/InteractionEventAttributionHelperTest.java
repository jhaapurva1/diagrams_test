package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants.CpcData;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent.Properties;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class InteractionEventAttributionHelperTest {

    @InjectMocks
    private InteractionEventAttributionHelper interactionEventAttributionHelper;

    @Test
    public void testForGetMultipliedCpcDataWhenChargeableCpcIsNull() {
        HashMap<String, BigDecimal> expectedMultipliedCpcData = new HashMap<>();
        expectedMultipliedCpcData.put(CpcData.MULTIPLIED_CPC, null);
        expectedMultipliedCpcData.put(CpcData.MULTIPLIER, null);
        HashMap<String, BigDecimal> multipliedCpcData = interactionEventAttributionHelper.getMultipliedCpcData(
                null, null, null);
        Assert.assertEquals(expectedMultipliedCpcData, multipliedCpcData);
    }

    @Test
    public void testForGetMultipliedCpcDataWhenWidgetEventHelperIsNull() {
        HashMap<String, BigDecimal> expectedMultipliedCpcData = new HashMap<>();
        expectedMultipliedCpcData.put(CpcData.MULTIPLIED_CPC, BigDecimal.TEN);
        expectedMultipliedCpcData.put(CpcData.MULTIPLIER, BigDecimal.ONE);
        HashMap<String, BigDecimal> multipliedCpcData = interactionEventAttributionHelper.getMultipliedCpcData(
                BigDecimal.TEN, null, null);
        Assert.assertEquals(expectedMultipliedCpcData, multipliedCpcData);
    }

    @Test
    public void testForGetMultipliedCpcDataWhenWidgetEventHelperIsNotNull() {

        HashMap<String, BigDecimal> expectedMultipliedCpcData = new HashMap<>();
        expectedMultipliedCpcData.put(CpcData.MULTIPLIED_CPC, new BigDecimal(100));
        expectedMultipliedCpcData.put(CpcData.MULTIPLIER, BigDecimal.TEN);
        WidgetEventHelper widgetEventHelper = new WidgetEventHelper();
        widgetEventHelper.setContext(AdWidgetClickEvent.builder()
                .properties(Properties.builder().sourceScreen(ConsumerConstants.AdWidgetRealEstates.PDP).build()).build());
        ReflectionTestUtils.setField(widgetEventHelper, "cpcMultiplier", BigDecimal.TEN);
        HashMap<String, BigDecimal> multipliedCpcData = interactionEventAttributionHelper.getMultipliedCpcData(
                BigDecimal.TEN, null, widgetEventHelper);
        Assert.assertEquals(expectedMultipliedCpcData, multipliedCpcData);
    }
}
