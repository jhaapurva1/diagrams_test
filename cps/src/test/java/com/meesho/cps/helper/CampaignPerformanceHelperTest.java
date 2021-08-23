package com.meesho.cps.helper;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.service.ClickBillHandlerImpl;
import com.meesho.cps.service.InteractionBillHandlerImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

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
    private CampaignCatalogMetricsRepository campaignCatalogMetricsRepository;

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

    public List<CampaignCatalogMetrics> getCampaignCatalogMetricsList() {
        List<CampaignCatalogMetrics> campaignCatalogMetricsList = new ArrayList<>();
        CampaignCatalogMetrics campaignCatalogMetrics = new CampaignCatalogMetrics();
        campaignCatalogMetrics.setWeightedWishlistCount(new BigDecimal(2));
        campaignCatalogMetrics.setWeightedSharesCount(new BigDecimal(3));
        campaignCatalogMetrics.setWeightedClickCount(new BigDecimal(20));
        campaignCatalogMetrics.setOriginWiseClickCount(new HashMap<>());
        campaignCatalogMetrics.setCampaignId(2L);
        campaignCatalogMetrics.setCatalogId(3L);
        campaignCatalogMetrics.setViewCount(10L);
        campaignCatalogMetrics.setBudgetUtilised(new BigDecimal(50));
        campaignCatalogMetricsList.add(campaignCatalogMetrics);
        return campaignCatalogMetricsList;
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
    public void updateCampaignPerformanceFromHbaseForBillVersionOne() {
        List<CampaignPerformance> campaignPerformances = getSampleCampaignPerformanceDataList();
        Mockito.doReturn(getCampaignCatalogMetricsList())
                .when(campaignCatalogMetricsRepository)
                .getAll(Mockito.anyList());
        Mockito.doReturn(new ClickBillHandlerImpl())
                .when(adBillFactory)
                .getBillHandlerForBillVersion(any());
        campaignPerformanceHelper.updateCampaignPerformanceFromHbase(
                campaignPerformances, getSampleCampaignIdAndCatalogDetailsMap());
        for(CampaignPerformance campaignPerformance : campaignPerformances) {
            if (campaignPerformance.getCampaignId() == 2L) {
                Assert.assertEquals(new Long(20), campaignPerformance.getTotalClicks());
            }
        }
    }

    @Test
    public void updateCampaignPerformanceFromHbaseForBillVersionTwo() {
        List<CampaignPerformance> campaignPerformances = getSampleCampaignPerformanceDataList();
        Mockito.doReturn(getCampaignCatalogMetricsList())
                .when(campaignCatalogMetricsRepository)
                .getAll(Mockito.anyList());
        Mockito.doReturn(new InteractionBillHandlerImpl())
                .when(adBillFactory)
                .getBillHandlerForBillVersion(any());
        campaignPerformanceHelper.updateCampaignPerformanceFromHbase(
                campaignPerformances, getSampleCampaignIdAndCatalogDetailsMap());
        for(CampaignPerformance campaignPerformance : campaignPerformances) {
            if (campaignPerformance.getCampaignId() == 2L) {
                Assert.assertEquals(new Long(25), campaignPerformance.getTotalClicks());
            }
        }
    }

    @Test
    public void testIsBeforeResetTimeOfDailyBudgetForCampaign() {
        LocalDateTime dateTime = DateTimeUtils.getCurrentLocalDateTimeInIST();
        Mockito.doReturn("00:00:00").when(applicationProperties).getDailyBudgetResetTime();
        Assert.assertFalse(campaignPerformanceHelper.beforeResetTimeOfDailyBudgetForCampaign(dateTime));
    }

}
