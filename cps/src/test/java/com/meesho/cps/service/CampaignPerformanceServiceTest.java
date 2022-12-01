package com.meesho.cps.service;

import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import com.meesho.cps.db.elasticsearch.ElasticSearchRepository;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cpsclient.request.BudgetUtilisedRequest;
import com.meesho.cpsclient.request.CampaignCatalogDateLevelBudgetUtilisedRequest;
import com.meesho.cpsclient.request.CampaignCatalogPerfDatawiseRequest;
import com.meesho.cpsclient.response.BudgetUtilisedResponse;
import com.meesho.cpsclient.response.CampaignCatalogDateLevelBudgetUtilisedResponse;
import com.meesho.cpsclient.response.CampaignPerformanceDatewiseResponse;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;


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
                .when(campaignCatalogDateMetricsRepository).get(any(), any(), any());
        CampaignCatalogDateLevelBudgetUtilisedResponse actualResponse =
                campaignPerformanceService.getDateLevelBudgetUtilised(getDateLevelBudgetUtilisedRequest());
        CampaignCatalogDateLevelBudgetUtilisedResponse expectedResponse = getExpectedDateLevelBudgetUtilisedResponse();
        Assert.assertEquals(expectedResponse, actualResponse);
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
                .campaignId(1L)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .build();
    }

}