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
public class PdpRecoEventHelperTest {

    @InjectMocks
    PdpRecoEventHelper pdpRecoEventHelper;

    private AdWidgetClickEvent adWidgetClickEvent;

    @Before
    public void setUp() throws JsonProcessingException {
        ReflectionTestUtils.setField(pdpRecoEventHelper, "pdpRecoCpcMultiplier",
            BigDecimal.TEN);
        adWidgetClickEvent = AdWidgetClickEvent.builder().build();
    }

    @Test
    public void testForGetFeedType() {
        Assert.assertEquals(FeedType.PRODUCT_RECO.getValue(), pdpRecoEventHelper.getFeedType());
    }


    @Test
    public void testForGetCpcMultiplier() {
        Assert.assertEquals(BigDecimal.TEN, pdpRecoEventHelper.getCpcMultiplier());
    }

    @Test
    public void testForGetOriginForClick() {
        Assert.assertEquals(AdWidgets.ORIGIN_PDP_RECO,
            pdpRecoEventHelper.getOrigin(adWidgetClickEvent));
    }

    @Test
    public void testForGetScreenForClick() {
        Assert.assertEquals(AdWidgets.SCREEN_PDP_RECO,
            pdpRecoEventHelper.getScreen(adWidgetClickEvent));
    }
}
