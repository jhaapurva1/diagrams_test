package com.meesho.cps.service;

import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.mongodb.collection.SupplierWeekWiseMetrics;
import com.meesho.cps.data.entity.mongodb.projection.CampaignCatalogLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.CampaignLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.DateLevelMetrics;
import com.meesho.cps.data.internal.BasePerformanceMetrics;
import com.meesho.cps.db.mongodb.dao.CampaignCatalogDateMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignDateWiseMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignMetricsDao;
import com.meesho.cps.db.mongodb.dao.SupplierWeekWiseMetricsDao;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cpsclient.request.*;
import com.meesho.cpsclient.response.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.BeanUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;


@RunWith(MockitoJUnitRunner.class)
public class CampaignPerformanceServiceTest {

    @Mock
    private CampaignMetricsDao campaignMetricsDao;

    @Mock
    private CampaignDateWiseMetricsDao campaignDateWiseMetricsDao;

    @Mock
    private SupplierWeekWiseMetricsDao supplierWeekWiseMetricsDao;

    @Mock
    private CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Mock
    private CampaignPerformanceHelper campaignPerformanceHelper;

    @Mock
    private ApplicationProperties applicationProperties;

    @Spy
    private CampaignPerformanceTransformer campaignPerformanceTransformer;

    @InjectMocks
    private CampaignPerformanceService campaignPerformanceService;

    private static final Long VIEW_COUNTS = 100L;
    private static final Long CLICK_COUNTS = 10L;
    private static final BigDecimal BUDGET_UTILISED = BigDecimal.valueOf(5).setScale(2, RoundingMode.HALF_UP);
    private static final Integer ORDER_COUNTS = 1;
    private static final BigDecimal REVENUE = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);

    @Before
    public void setUp() {
        Mockito.doReturn(LocalDate.now()).when(applicationProperties).getCampaignDatewiseMetricsReferenceDate();
        Mockito.doReturn(Arrays.asList(getSampleCampaignLevelMetrics(1L),
                getSampleCampaignLevelMetrics(2L))).when(campaignCatalogDateMetricsDao).getCampaignLevelMetrics(any(), any(), any());
        Mockito.doReturn(Arrays.asList(getSampleCampaignCatalogLevelMetrics(1L),
                getSampleCampaignCatalogLevelMetrics(2L))).when(campaignCatalogDateMetricsDao).getCampaignCatalogLevelMetrics(any(), anyList(), any(), any());
    }

    @Test
    public void testGetCampaignPerformanceMetrics() {
        CampaignPerformanceResponse actualResponse = campaignPerformanceService.getCampaignPerformanceMetrics(getSampleCampaignPerformanceRequest(LocalDate.now(), LocalDate.now()));
        Assert.assertEquals(getExpectedCampaignPerformanceResponse(), actualResponse);
    }

    @Test
    public void testGetCampaignPerformanceMetricsWhenDateRangeIsNotProvided() {
        CampaignPerformanceResponse actualResponse = campaignPerformanceService.getCampaignPerformanceMetrics(getSampleCampaignPerformanceRequest(null, null));
        Assert.assertEquals(getExpectedCampaignPerformanceResponse(), actualResponse);
    }

    @Test
    public void testGetCampaignCatalogPerformanceMetrics() throws IOException {
        CampaignCatalogPerformanceResponse actualResponse = campaignPerformanceService.getCampaignCatalogPerformanceMetrics(getSampleCampaignCatalogPerformanceRequest(LocalDate.now(), LocalDate.now()));
        Assert.assertEquals(getExpectedCampaignCatalogPerformanceResponse(), actualResponse);
    }

    @Test
    public void testGetCampaignCatalogPerformanceMetricsWhenDateRangeIsNotProvided() throws IOException {
        CampaignCatalogPerformanceResponse actualResponse = campaignPerformanceService.getCampaignCatalogPerformanceMetrics(getSampleCampaignCatalogPerformanceRequest(null, null));
        Assert.assertEquals(getExpectedCampaignCatalogPerformanceResponse(), actualResponse);
    }

    @Test
    public void testGetBudgetUtilised() {
        Mockito.doReturn(new ArrayList<>()).when(campaignMetricsDao).findAllByCampaignIdsIn(any());
        Mockito.doReturn(new ArrayList<>()).when(campaignDateWiseMetricsDao).findAllByCampaignIdsInAndDate(any(), any());
        Mockito.doReturn(getSampleSupplierWeekWiseMetrics()).when(supplierWeekWiseMetricsDao).findAllBySupplierIdAndWeekStartDate(any(), any());
        Mockito.doReturn(LocalDate.now()).when(campaignPerformanceHelper).getLocalDateForDailyCampaignFromLocalDateTime(any());

        BudgetUtilisedResponse actualResponse = campaignPerformanceService.getBudgetUtilised(getSampleBudgetUtilisedRequest());
        Assert.assertEquals(getExpectedBudgetUtilisedResponse(), actualResponse);

    }

    @Test
    public void testDateLevelBudgetUtilised() {
        Mockito.doReturn(getSampleMasterPerformanceDocument(1L, 1L, LocalDate.now())).when(campaignCatalogDateMetricsDao).find(any(), any(), any());
        CampaignCatalogDateLevelBudgetUtilisedResponse actualResponse =
                campaignPerformanceService.getDateLevelBudgetUtilised(getDateLevelBudgetUtilisedRequest());
        CampaignCatalogDateLevelBudgetUtilisedResponse expectedResponse = getExpectedDateLevelBudgetUtilisedResponse();
        Assert.assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void testGetCampaignCatalogPerfDateWise() throws IOException {
        Mockito.doReturn(Collections.singletonList(getSampleDateLevelMetrics(LocalDate.now()))).when(campaignCatalogDateMetricsDao).getDateLevelMetrics(any(), any(), any());
        CampaignPerformanceDatewiseResponse actualResponse = campaignPerformanceService.getCampaignCatalogPerfDateWise(getSampleCampaignCatalogPerfDateWiseRequest());
        Assert.assertEquals(getExpectedCampaignPerformanceDateWiseResponse(), actualResponse);
    }

    private CampaignPerformanceRequest getSampleCampaignPerformanceRequest(LocalDate startDate, LocalDate endDate) {
        return CampaignPerformanceRequest.builder()
                .campaignDetails(Arrays.asList(
                        CampaignPerformanceRequest.CampaignDetails.builder().campaignId(1L).build(),
                        CampaignPerformanceRequest.CampaignDetails.builder().campaignId(2L).build()))
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private CampaignCatalogPerformanceRequest getSampleCampaignCatalogPerformanceRequest(LocalDate startDate, LocalDate endDate) {
        return CampaignCatalogPerformanceRequest.builder()
                .campaignId(1L)
                .catalogIds(Arrays.asList(1L, 2L))
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private SupplierPerformanceRequest getSampleSupplierPerformanceRequest(LocalDate startDate, LocalDate endDate) {
        return SupplierPerformanceRequest.builder()
                .supplierId(1L)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private CampaignCatalogDateMetrics getSampleMasterPerformanceDocument(Long campaignId, Long catalogId, LocalDate date) {
        return CampaignCatalogDateMetrics.builder()
                .campaignId(campaignId)
                .catalogId(catalogId)
                .date(date.toString())
                .revenue(REVENUE)
                .budgetUtilised(BUDGET_UTILISED)
                .orders(10)
                .build();
    }

    private BudgetUtilisedRequest getSampleBudgetUtilisedRequest() {
        return BudgetUtilisedRequest.builder()
                .campaignDataList(new ArrayList<>())
                .suppliersIdList(Arrays.asList(1L, 2L))
                .build();
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

    private CampaignPerformanceResponse getExpectedCampaignPerformanceResponse() {
        return CampaignPerformanceResponse.builder()
                .campaigns(Arrays.asList(
                    CampaignPerformanceResponse.CampaignDetails.builder()
                        .campaignId(1L)
                        .budgetUtilised(BUDGET_UTILISED)
                        .totalViews(VIEW_COUNTS)
                        .totalClicks(CLICK_COUNTS)
                        .orderCount(ORDER_COUNTS)
                        .revenue(REVENUE)
                        .roi(REVENUE.divide(BUDGET_UTILISED, RoundingMode.HALF_UP))
                        .conversionRate(100.0*CLICK_COUNTS/VIEW_COUNTS)
                        .build(),
                    CampaignPerformanceResponse.CampaignDetails.builder()
                        .campaignId(2L)
                        .budgetUtilised(BUDGET_UTILISED)
                        .totalViews(VIEW_COUNTS)
                        .totalClicks(CLICK_COUNTS)
                        .orderCount(ORDER_COUNTS)
                        .revenue(REVENUE)
                        .roi(REVENUE.divide(BUDGET_UTILISED, RoundingMode.HALF_UP))
                        .conversionRate(100.0*CLICK_COUNTS/VIEW_COUNTS)
                        .build()))
                .build();
    }

    private CampaignCatalogPerformanceResponse getExpectedCampaignCatalogPerformanceResponse() {
        return CampaignCatalogPerformanceResponse.builder()
                .catalogs(Arrays.asList(
                    CampaignCatalogPerformanceResponse.CatalogDetails.builder()
                        .campaignId(1L)
                        .catalogId(1L)
                        .budgetUtilised(BUDGET_UTILISED)
                        .totalViews(VIEW_COUNTS)
                        .totalClicks(CLICK_COUNTS)
                        .orderCount(ORDER_COUNTS)
                        .revenue(REVENUE)
                        .roi(REVENUE.divide(BUDGET_UTILISED, RoundingMode.HALF_UP))
                        .conversionRate(100.0*CLICK_COUNTS/VIEW_COUNTS)
                        .build(),
                    CampaignCatalogPerformanceResponse.CatalogDetails.builder()
                        .campaignId(1L)
                        .catalogId(2L)
                        .budgetUtilised(BUDGET_UTILISED)
                        .totalViews(VIEW_COUNTS)
                        .totalClicks(CLICK_COUNTS)
                        .orderCount(ORDER_COUNTS)
                        .revenue(REVENUE)
                        .roi(REVENUE.divide(BUDGET_UTILISED, RoundingMode.HALF_UP))
                        .conversionRate(100.0*CLICK_COUNTS/VIEW_COUNTS)
                        .build()))
                .build();
    }

    private CampaignPerformanceDatewiseResponse getExpectedCampaignPerformanceDateWiseResponse() {
        return CampaignPerformanceDatewiseResponse.builder()
                .campaignId(1L)
                .dateCatalogsMap(Collections.singletonMap(LocalDate.now(),
                    CampaignPerformanceDatewiseResponse.GraphDetails.builder()
                    .orders(ORDER_COUNTS)
                    .views(VIEW_COUNTS)
                    .clicks(CLICK_COUNTS)
                    .build()))
                .build();
    }

    private BudgetUtilisedResponse getExpectedBudgetUtilisedResponse() {
        List<BudgetUtilisedResponse.SupplierBudgetUtilisedDetails> supplierBudgetUtilisedDetails = new ArrayList<>();
        supplierBudgetUtilisedDetails.add(BudgetUtilisedResponse.SupplierBudgetUtilisedDetails.builder().supplierId(1L)
                .budgetUtilised(BUDGET_UTILISED).build());
        supplierBudgetUtilisedDetails.add(BudgetUtilisedResponse.SupplierBudgetUtilisedDetails.builder().supplierId(2L)
                .budgetUtilised(BUDGET_UTILISED).build());

        return BudgetUtilisedResponse.builder().budgetUtilisedDetails(new ArrayList<>()).suppliersBudgetUtilisedDetails(supplierBudgetUtilisedDetails).build();
    }

    private List<SupplierWeekWiseMetrics> getSampleSupplierWeekWiseMetrics() {
        List<SupplierWeekWiseMetrics> supplierWeekWiseMetrics = new ArrayList<>();
        supplierWeekWiseMetrics.add(SupplierWeekWiseMetrics.builder()
                .supplierId(1L).budgetUtilised(BUDGET_UTILISED)
                .build());
        supplierWeekWiseMetrics.add(SupplierWeekWiseMetrics.builder()
                .supplierId(2L).budgetUtilised(BUDGET_UTILISED)
                .build());

        return supplierWeekWiseMetrics;
    }

    private CampaignCatalogDateLevelBudgetUtilisedResponse getExpectedDateLevelBudgetUtilisedResponse() {
        CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails catalogDetails =
                CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails.builder().catalogId(1L)
                        .budgetUtilised(BUDGET_UTILISED).build();
        CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails campaignDetails =
                CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.builder().campaignId(1L)
                        .date(LocalDate.now()).catalogDetails(Collections.singletonList(catalogDetails)).build();
        return CampaignCatalogDateLevelBudgetUtilisedResponse.builder()
                .campaignDetails(Collections.singletonList(campaignDetails)).build();
    }

    private CampaignCatalogPerfDatawiseRequest getSampleCampaignCatalogPerfDateWiseRequest() {
        return CampaignCatalogPerfDatawiseRequest.builder()
                .campaignId(1L)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();
    }

    private CampaignLevelMetrics getSampleCampaignLevelMetrics(Long campaignId) {
        CampaignLevelMetrics campaignLevelMetrics = new CampaignLevelMetrics();
        BeanUtils.copyProperties(getSampleBasePerformanceMetrics(),campaignLevelMetrics);
        campaignLevelMetrics.setCampaignId(campaignId);
        return campaignLevelMetrics;
    }

    private CampaignCatalogLevelMetrics getSampleCampaignCatalogLevelMetrics(Long catalogId) {
        CampaignCatalogLevelMetrics campaignCatalogLevelMetrics = new CampaignCatalogLevelMetrics();
        BeanUtils.copyProperties(getSampleBasePerformanceMetrics(), campaignCatalogLevelMetrics);
        campaignCatalogLevelMetrics.setCampaignIdCatalogId(CampaignCatalogLevelMetrics.CampaignIdCatalogId.builder().campaignId(1L).catalogId(catalogId).build());
        return campaignCatalogLevelMetrics;
    }

    private DateLevelMetrics getSampleDateLevelMetrics(LocalDate date) {
        DateLevelMetrics dateLevelMetrics = new DateLevelMetrics();
        BeanUtils.copyProperties(getSampleBasePerformanceMetrics(),dateLevelMetrics);
        dateLevelMetrics.setDate(date.toString());
        return dateLevelMetrics;
    }

    private BasePerformanceMetrics getSampleBasePerformanceMetrics() {
        BasePerformanceMetrics basePerformanceMetrics = new BasePerformanceMetrics();
        basePerformanceMetrics.setClicks(CLICK_COUNTS);
        basePerformanceMetrics.setViews(VIEW_COUNTS);
        basePerformanceMetrics.setBudgetUtilised(BUDGET_UTILISED);
        basePerformanceMetrics.setOrders(ORDER_COUNTS);
        basePerformanceMetrics.setRevenue(REVENUE);
        return basePerformanceMetrics;
    }

}