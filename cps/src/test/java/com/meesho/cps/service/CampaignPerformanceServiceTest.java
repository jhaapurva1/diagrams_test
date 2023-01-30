package com.meesho.cps.service;

import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import com.meesho.cps.data.internal.PerformancePojo;
import com.meesho.cps.db.elasticsearch.ElasticSearchRepository;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cpsclient.request.*;
import com.meesho.cpsclient.response.*;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;


@RunWith(MockitoJUnitRunner.class)
public class CampaignPerformanceServiceTest {

    @Mock
    private CampaignMetricsRepository campaignMetricsRepository;

    @Mock
    private CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Mock
    private SupplierWeekWiseMetricsRepository supplierWeekWiseMetricsRepository;

    @Mock
    private CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Mock
    private CampaignPerformanceHelper campaignPerformanceHelper;

    @Mock
    private ElasticSearchRepository elasticSearchRepository;

    @Spy
    private CampaignPerformanceTransformer campaignPerformanceTransformer;

    @InjectMocks
    private CampaignPerformanceService campaignPerformanceService;

    @Test
    public void testGetCampaignPerformanceMetrics() throws IOException {
        Mockito.doReturn(Collections.singletonList(LocalDate.now())).when(campaignPerformanceHelper).getDatesForHBaseQuery(any(), any());
        Mockito.doReturn(Arrays.asList(getSampleCampaignCatalogDateMetrics(9L,1L, LocalDate.now()),
                                       getSampleCampaignCatalogDateMetrics(9L, 2L, LocalDate.now()),
                                       getSampleCampaignCatalogDateMetrics(8L, 3L, LocalDate.now()),
                                       getSampleCampaignCatalogDateMetrics(8L,4L, LocalDate.now())))
               .when(campaignCatalogDateMetricsRepository).get(anyMap(), anyList());
        Mockito.doReturn(getSamplePerformancePojoForCampaignPerformanceMetrics()).when(campaignPerformanceHelper).getAggregatedCampaignCatalogDateMetrics(anyList());

        CampaignPerformanceResponse actualResponse = campaignPerformanceService.getCampaignPerformanceMetrics(getSampleCampaignPerformanceRequest());
        Assert.assertEquals(getExpectedCampaignPerformanceResponse(), actualResponse);
    }

    @Test
    public void testGetCampaignCatalogPerformanceMetrics() throws IOException {
        Mockito.doReturn(Collections.singletonList(LocalDate.now())).when(campaignPerformanceHelper).getDatesForHBaseQuery(any(), any());
        Mockito.doReturn(Arrays.asList(getSampleCampaignCatalogDateMetrics(9L,1L, LocalDate.now()),
                                       getSampleCampaignCatalogDateMetrics(9L, 2L, LocalDate.now())))
                .when(campaignCatalogDateMetricsRepository).get(anyMap(), anyList());
        Mockito.doReturn(getSamplePerformancePojoForCampaignCatalogPerformanceMetrics()).when(campaignPerformanceHelper).getAggregatedCampaignCatalogDateMetrics(anyList());

        CampaignCatalogPerformanceResponse actualResponse = campaignPerformanceService.getCampaignCatalogPerformanceMetrics(getSampleCampaignCatalogPerformanceRequest());
        Assert.assertEquals(getExpectedCampaignCatalogPerformanceResponse(), actualResponse);
    }

    @Test
    public void testGetBudgetUtilised() {
        Mockito.doReturn(new ArrayList<>()).when(campaignMetricsRepository).getAll(any());
        Mockito.doReturn(new ArrayList<>()).when(campaignDatewiseMetricsRepository).getAll(any(), any());
        Mockito.doReturn(getSampleSupplierWeekWiseMetrics()).when(supplierWeekWiseMetricsRepository).getAll(any(), any());
        Mockito.doReturn(null).when(campaignPerformanceHelper).getLocalDateForDailyCampaignFromLocalDateTime(any());

        BudgetUtilisedResponse actualResponse = campaignPerformanceService.getBudgetUtilised(getSampleBudgetUtilisedRequest());
        Assert.assertEquals(getExpectedBudgetUtilisedResponse(), actualResponse);

    }

    @Test
    public void testDateLevelBudgetUtilised() {
        Mockito.doReturn(CampaignCatalogDateMetrics.builder().catalogId(1L).budgetUtilised(BigDecimal.valueOf(1000)).build())
                .when(campaignCatalogDateMetricsRepository).get(any(), (Long) any(), any());
        CampaignCatalogDateLevelBudgetUtilisedResponse actualResponse =
                campaignPerformanceService.getDateLevelBudgetUtilised(getDateLevelBudgetUtilisedRequest());
        CampaignCatalogDateLevelBudgetUtilisedResponse expectedResponse = getExpectedDateLevelBudgetUtilisedResponse();
        Assert.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testGetCampaignCatalogPerfDateWise() throws IOException {
        Mockito.doReturn(Collections.singletonList(LocalDate.now())).when(campaignPerformanceHelper).getDatesForHBaseQuery(any(), any());
        Mockito.doReturn(Arrays.asList(getSampleCampaignCatalogDateMetrics(9L,1L, LocalDate.now()),
                                       getSampleCampaignCatalogDateMetrics(9L, 2L, LocalDate.now())))
                .when(campaignCatalogDateMetricsRepository).get(anyMap(), anyList());
        Mockito.doReturn(getSamplePerformancePojoForCampaignCatalogPerfDateWise()).when(campaignPerformanceHelper).getAggregatedCampaignCatalogDateMetrics(anyList());

        CampaignPerformanceDatewiseResponse actualResponse = campaignPerformanceService.getCampaignCatalogPerfDateWise(getSampleCampaignCatalogPerfDatawiseRequest());
        Assert.assertEquals(getExpectedCampaignPerformanceDatewiseResponse(), actualResponse);
    }

    private CampaignPerformanceRequest getSampleCampaignPerformanceRequest() {
        return CampaignPerformanceRequest.builder()
                .campaignDetails(Arrays.asList(
                    CampaignPerformanceRequest.CampaignDetails.builder()
                        .campaignId(9L)
                        .catalogIds(Arrays.asList(1L, 2L))
                        .build(),
                    CampaignPerformanceRequest.CampaignDetails.builder()
                        .campaignId(8L)
                        .catalogIds(Arrays.asList(3L, 4L))
                        .build()))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();
    }

    private CampaignCatalogPerformanceRequest getSampleCampaignCatalogPerformanceRequest() {
        return CampaignCatalogPerformanceRequest.builder()
                .campaignId(9L)
                .catalogIds(Arrays.asList(1L, 2L))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();
    }

    private CampaignPerformanceResponse getExpectedCampaignPerformanceResponse() {
        return CampaignPerformanceResponse.builder()
                .campaigns(Arrays.asList(
                    CampaignPerformanceResponse.CampaignDetails.builder()
                        .campaignId(9L)
                        .revenue(BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP))
                        .budgetUtilised(BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP))
                        .roi(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP))
                        .orderCount(20)
                        .totalViews(0L)
                        .totalClicks(0L)
                        .build(),
                    CampaignPerformanceResponse.CampaignDetails.builder()
                        .campaignId(8L)
                        .revenue(BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP))
                        .budgetUtilised(BigDecimal.valueOf(20).setScale(2, RoundingMode.HALF_UP))
                        .roi(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP))
                        .orderCount(20)
                        .totalViews(0L)
                        .totalClicks(0L)
                        .build()))
                .build();
    }

    private CampaignCatalogPerformanceResponse getExpectedCampaignCatalogPerformanceResponse() {
        return CampaignCatalogPerformanceResponse.builder()
                .catalogs(Arrays.asList(
                    CampaignCatalogPerformanceResponse.CatalogDetails.builder()
                        .campaignId(9L)
                        .catalogId(1L)
                        .revenue(BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP))
                        .budgetUtilised(BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP))
                        .roi(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP))
                        .orderCount(10)
                        .totalViews(0L)
                        .totalClicks(0L)
                        .build(),
                    CampaignCatalogPerformanceResponse.CatalogDetails.builder()
                        .campaignId(9L)
                        .catalogId(2L)
                        .revenue(BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP))
                        .budgetUtilised(BigDecimal.TEN.setScale(2, RoundingMode.HALF_UP))
                        .roi(BigDecimal.ONE.setScale(2, RoundingMode.HALF_UP))
                        .orderCount(10)
                        .totalViews(0L)
                        .totalClicks(0L)
                        .build()))
                .build();
    }

    private CampaignPerformanceDatewiseResponse getExpectedCampaignPerformanceDatewiseResponse() {
        return CampaignPerformanceDatewiseResponse.builder()
                .campaignId(9L)
                .dateCatalogsMap(Collections.singletonMap(LocalDate.now(),
                    CampaignPerformanceDatewiseResponse.GraphDetails.builder()
                    .orders(20)
                    .views(0L)
                    .clicks(0L)
                    .build()))
                .build();
    }

    private PerformancePojo getSamplePerformancePojoForCampaignPerformanceMetrics() {
        return PerformancePojo.builder()
                .totalOrders(20)
                .totalShares(0L)
                .totalRevenue(BigDecimal.valueOf(20))
                .totalWishlist(0L)
                .totalClicks(0L)
                .totalBudgetUtilised(BigDecimal.valueOf(20))
                .totalViews(0L)
                .build();
    }

    private PerformancePojo getSamplePerformancePojoForCampaignCatalogPerformanceMetrics() {
        return PerformancePojo.builder()
                .totalOrders(10)
                .totalShares(0L)
                .totalRevenue(BigDecimal.TEN)
                .totalWishlist(0L)
                .totalClicks(0L)
                .totalBudgetUtilised(BigDecimal.TEN)
                .totalViews(0L)
                .build();
    }

    private PerformancePojo getSamplePerformancePojoForCampaignCatalogPerfDateWise() {
        return PerformancePojo.builder()
                .totalOrders(20)
                .totalShares(0L)
                .totalRevenue(BigDecimal.valueOf(20))
                .totalWishlist(0L)
                .totalClicks(0L)
                .totalBudgetUtilised(BigDecimal.valueOf(20))
                .totalViews(0L)
                .build();
    }

    private CampaignCatalogDateMetrics getSampleCampaignCatalogDateMetrics(Long campaignId, Long catalogId, LocalDate date) {
        return CampaignCatalogDateMetrics.builder()
                .campaignId(campaignId)
                .catalogId(catalogId)
                .date(date)
                .revenue(BigDecimal.TEN)
                .budgetUtilised(BigDecimal.TEN)
                .orders(10)
                .build();
    }

    private BudgetUtilisedRequest getSampleBudgetUtilisedRequest() {
        return BudgetUtilisedRequest.builder()
                .campaignDataList(new ArrayList<>())
                .suppliersIdList(Arrays.asList(1L, 2L))
                .build();
    }

    private BudgetUtilisedResponse getExpectedBudgetUtilisedResponse() {
        List<BudgetUtilisedResponse.SupplierBudgetUtilisedDetails> supplierBudgetUtilisedDetails = new ArrayList<>();
        supplierBudgetUtilisedDetails.add(BudgetUtilisedResponse.SupplierBudgetUtilisedDetails.builder().supplierId(1L)
                .budgetUtilised(BigDecimal.valueOf(100)).build());
        supplierBudgetUtilisedDetails.add(BudgetUtilisedResponse.SupplierBudgetUtilisedDetails.builder().supplierId(2L)
                .budgetUtilised(BigDecimal.valueOf(200)).build());

        return BudgetUtilisedResponse.builder().budgetUtilisedDetails(new ArrayList<>()).suppliersBudgetUtilisedDetails(supplierBudgetUtilisedDetails).build();
    }

    private List<SupplierWeekWiseMetrics> getSampleSupplierWeekWiseMetrics() {
        List<SupplierWeekWiseMetrics> supplierWeekWiseMetrics = new ArrayList<>();
        supplierWeekWiseMetrics.add(SupplierWeekWiseMetrics.builder()
                .supplierId(1L).budgetUtilised(BigDecimal.valueOf(100))
                .build());
        supplierWeekWiseMetrics.add(SupplierWeekWiseMetrics.builder()
                .supplierId(2L).budgetUtilised(BigDecimal.valueOf(200))
                .build());

        return supplierWeekWiseMetrics;
    }


    private CampaignCatalogDateLevelBudgetUtilisedRequest getDateLevelBudgetUtilisedRequest() {
        CampaignCatalogDateLevelBudgetUtilisedRequest.CampaignDetails.CatalogDetails catalogDetails =
                CampaignCatalogDateLevelBudgetUtilisedRequest.CampaignDetails.CatalogDetails.builder().catalogId(1L).build();
        CampaignCatalogDateLevelBudgetUtilisedRequest.CampaignDetails campaignDetails =
                CampaignCatalogDateLevelBudgetUtilisedRequest.CampaignDetails.builder().campaignId(1L)
                        .date(LocalDate.now()).catalogDetails(Collections.singletonList(catalogDetails)).build();
        return CampaignCatalogDateLevelBudgetUtilisedRequest.builder()
                .campaignDetails(Collections.singletonList(campaignDetails)).build();
    }

    private CampaignCatalogDateLevelBudgetUtilisedResponse getExpectedDateLevelBudgetUtilisedResponse() {
        CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails catalogDetails =
                CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails.builder().catalogId(1L)
                        .budgetUtilised(BigDecimal.valueOf(1000)).build();
        CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails campaignDetails =
                CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.builder().campaignId(1L)
                        .date(LocalDate.now()).catalogDetails(Collections.singletonList(catalogDetails)).build();
        return CampaignCatalogDateLevelBudgetUtilisedResponse.builder()
                .campaignDetails(Collections.singletonList(campaignDetails)).build();
    }

    private CampaignCatalogPerfDatawiseRequest getSampleCampaignCatalogPerfDatawiseRequest() {
        return CampaignCatalogPerfDatawiseRequest.builder()
                .campaignId(9L)
                .catalogIds(Lists.newArrayList(1L, 2L))
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();
    }

}