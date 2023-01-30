package com.meesho.cps.helper;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.factory.AdBillFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author shubham.aggarwal
 * 18/08/21
 */
@RunWith(MockitoJUnitRunner.class)
public class CampaignPerformanceHelperTest {

    @Mock
    private AdBillFactory adBillFactory;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private CampaignCatalogDateMetricsRepository campaignCatalogMetricsRepository;

    @InjectMocks
    CampaignPerformanceHelper campaignPerformanceHelper;

    public List<CampaignPerformance> getSampleCampaignPerformanceDataList() {
        List<CampaignPerformance> entitiesToBeUpdated = new ArrayList<>();
        CampaignPerformance campaignPerformance = new CampaignPerformance();
        campaignPerformance.setId(1L);
        campaignPerformance.setTotalClicks(4L);
        campaignPerformance.setCampaignId(2L);
        campaignPerformance.setCatalogId(3L);
        entitiesToBeUpdated.add(campaignPerformance);
        return entitiesToBeUpdated;
    }

    public List<CampaignCatalogDateMetrics> getCampaignCatalogMetricsList() {
        List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = new ArrayList<>();
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
        campaignCatalogDateMetrics.setWishlistCount(2L);
        campaignCatalogDateMetrics.setSharesCount(3L);
        campaignCatalogDateMetrics.setClickCount(20L);
        campaignCatalogDateMetrics.setCampaignId(2L);
        campaignCatalogDateMetrics.setCatalogId(3L);
        campaignCatalogDateMetrics.setViewCount(10L);
        campaignCatalogDateMetrics.setBudgetUtilised(new BigDecimal(50));
        campaignCatalogDateMetricsList.add(campaignCatalogDateMetrics);
        return campaignCatalogDateMetricsList;
    }

    public Map<Long, CampaignDetails> getSampleCampaignIdAndCatalogDetailsMap() {
        Map<Long, CampaignDetails> map = new HashMap<>();
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignId(2L)
                .budget(new BigDecimal(100))
                .billVersion(1)
                .build();
        map.put(2L, campaignDetails);
        return map;
    }

    @Test
    public void testIsBeforeResetTimeOfDailyBudgetForCampaign() {
        LocalDateTime dateTime = DateTimeUtils.getCurrentLocalDateTimeInIST();
        Mockito.doReturn("00:00:00").when(applicationProperties).getDailyBudgetResetTime();
        Assert.assertFalse(campaignPerformanceHelper.beforeResetTimeOfDailyBudgetForCampaign(dateTime));
    }

    @Test
    public void testForWhenDifferenceBetweenCurrentDateAndRequestEndDateEqualToZeroAndStartDateEqualToEndDate() {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now();
        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper.getDatesForHBaseQuery(startDate, endDate);

        Assert.assertEquals(1, hBaseQueryDates.size());
        Assert.assertTrue(hBaseQueryDates.contains(endDate));
    }

    @Test
    public void testForWhenDifferenceBetweenCurrentDateAndRequestEndDateEqualToZeroAndStartDateNotEqualToEndDateAndCurrentTimeIsLessThanOrEqualToMinutesToQueryPreviousDayDataFromHbase() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        Mockito.doReturn(getCurrentMinutesOfTheDay() + 10).when(applicationProperties).getMinutesToQueryPreviousDayDataFromHbase();
        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper.getDatesForHBaseQuery(startDate, endDate);

        Assert.assertEquals(2, hBaseQueryDates.size());
        Assert.assertTrue(hBaseQueryDates.contains(endDate));
        Assert.assertTrue(hBaseQueryDates.contains(endDate.minusDays(1)));
    }

    @Test
    public void testForWhenDifferenceBetweenCurrentDateAndRequestEndDateEqualToZeroAndStartDateNotEqualToEndDateAndCurrentTimeGreaterThanMinutesToQueryPreviousDayDataFromHbase() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        Mockito.doReturn(getCurrentMinutesOfTheDay() - 10).when(applicationProperties).getMinutesToQueryPreviousDayDataFromHbase();
        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper.getDatesForHBaseQuery(startDate, endDate);

        Assert.assertEquals(1, hBaseQueryDates.size());
        Assert.assertTrue(hBaseQueryDates.contains(endDate));
    }

    @Test
    public void testForWhenDifferenceBetweenCurrentDateAndRequestEndDateEqualToOneAndCurrentTimeLessThanOrEqualToMinutesToQueryPreviousDayDataFromHbase() {
        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now().minusDays(1);

        Mockito.doReturn(getCurrentMinutesOfTheDay() + 10).when(applicationProperties).getMinutesToQueryPreviousDayDataFromHbase();
        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper.getDatesForHBaseQuery(startDate, endDate);

        Assert.assertEquals(1, hBaseQueryDates.size());
        Assert.assertTrue(hBaseQueryDates.contains(endDate));
    }

    @Test
    public void testForWhenDifferenceBetweenCurrentDateAndRequestEndDateEqualToOneAndCurrentTimeGreaterThanMinutesToQueryPreviousDayDataFromHbase() {
        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now().minusDays(1);

        Mockito.doReturn(getCurrentMinutesOfTheDay() - 10).when(applicationProperties).getMinutesToQueryPreviousDayDataFromHbase();
        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper.getDatesForHBaseQuery(startDate, endDate);

        Assert.assertEquals(0, hBaseQueryDates.size());
    }

    @Test
    public void testForWhenDifferenceBetweenCurrentDateAndRequestEndDateGreaterThanOne() {
        LocalDate startDate = LocalDate.now().minusDays(3);
        LocalDate endDate = LocalDate.now().minusDays(2);

        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper.getDatesForHBaseQuery(startDate, endDate);

        Assert.assertEquals(0, hBaseQueryDates.size());
    }

    private int getCurrentMinutesOfTheDay() {
        Calendar now = Calendar.getInstance();
        return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
    }
}
