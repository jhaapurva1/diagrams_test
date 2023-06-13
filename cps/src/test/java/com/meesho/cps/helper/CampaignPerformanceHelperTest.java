package com.meesho.cps.helper;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

/**
 * @author shubham.aggarwal
 * 18/08/21
 */
@RunWith(MockitoJUnitRunner.class)
public class CampaignPerformanceHelperTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @InjectMocks
    CampaignPerformanceHelper campaignPerformanceHelper;

    @Test
    public void testIsBeforeResetTimeOfDailyBudgetForCampaign() {
        LocalDateTime dateTime = DateTimeUtils.getCurrentLocalDateTimeInIST();
        Mockito.doReturn("00:00:00").when(applicationProperties).getDailyBudgetResetTime();
        Assert.assertFalse(campaignPerformanceHelper.beforeResetTimeOfDailyBudgetForCampaign(dateTime));
    }

}
