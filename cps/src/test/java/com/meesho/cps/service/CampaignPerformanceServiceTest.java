package com.meesho.cps.service;

import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cpsclient.request.BudgetUtilisedRequest;
import com.meesho.cpsclient.response.BudgetUtilisedResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;


@RunWith(MockitoJUnitRunner.class)
public class CampaignPerformanceServiceTest {

    @Mock
    private CampaignMetricsRepository campaignMetricsRepository;

    @Mock
    private CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Mock
    private SupplierWeekWiseMetricsRepository supplierWeekWiseMetricsRepository;

    @Mock
    private CampaignPerformanceHelper campaignPerformanceHelper;

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

}