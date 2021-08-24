package com.meesho.cps.transformer;

import com.meesho.ads.lib.utils.Utils;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.data.entity.mysql.projection.CampaignOverallPerformanceView;
import com.meesho.cps.data.entity.mysql.projection.SupplierOverallPerformanceView;
import com.meesho.cps.utils.CalculationUtils;
import com.meesho.cpsclient.request.CreateCampaignPerformanceRequest;
import com.meesho.cpsclient.response.BudgetUtilisedResponse;
import com.meesho.cpsclient.response.CampaignCatalogPerformanceResponse;
import com.meesho.cpsclient.response.CampaignPerformanceResponse;
import com.meesho.cpsclient.response.SupplierPerformanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * @author shubham.aggarwal
 * 05/08/21
 */
@Service
@Slf4j
public class CampaignPerformanceTransformer {

    public CampaignPerformance getEntityFromRequest(CreateCampaignPerformanceRequest request) {
        return CampaignPerformance.builder()
                .campaignId(request.getCampaignId())
                .catalogId(request.getCatalogId())
                .supplierId(request.getSupplierId())
                .build();
    }

    public SupplierPerformanceResponse getSupplierPerformanceResponse(SupplierOverallPerformanceView view) {
        return SupplierPerformanceResponse.builder()
                .budgetUtilised(view.getTotalBudgetUtilized())
                .revenue(view.getTotalRevenue())
                .totalClicks(view.getTotalClicks())
                .totalViews(view.getTotalViews())
                .orderCount(view.getTotalOrders())
                .conversionRate(CalculationUtils.getConversionRate(view.getTotalOrders(),view.getTotalClicks()))
                .roi(CalculationUtils.getRoi(view.getTotalRevenue(), view.getTotalBudgetUtilized()))
                .build();
    }

    public CampaignPerformanceResponse getCampaignPerformanceResponse(List<CampaignOverallPerformanceView> views) {
        List<CampaignPerformanceResponse.CampaignDetails> performanceDetailsList = new ArrayList<>();
        for (CampaignOverallPerformanceView view : views) {
            performanceDetailsList.add(CampaignPerformanceResponse.CampaignDetails.builder()
                    .campaignId(view.getCampaignId())
                    .budgetUtilised(view.getTotalBudgetUtilized())
                    .revenue(view.getTotalRevenue())
                    .totalClicks(view.getTotalClicks())
                    .totalViews(view.getTotalViews())
                    .orderCount(view.getTotalOrders())
                    .conversionRate(CalculationUtils.getConversionRate(view.getTotalOrders(), view.getTotalClicks()))
                    .roi(CalculationUtils.getRoi(view.getTotalRevenue(), view.getTotalBudgetUtilized()))
                    .build()
            );
        }
        return CampaignPerformanceResponse.builder().campaigns(performanceDetailsList).build();
    }

    public CampaignCatalogPerformanceResponse getCampaignCatalogPerformanceResponse(
            List<CampaignPerformance> campaignPerformanceList) {
        List<CampaignCatalogPerformanceResponse.CatalogDetails> performanceDetailsList = new ArrayList<>();
        for (CampaignPerformance campaignPerformance : campaignPerformanceList) {
            performanceDetailsList.add(CampaignCatalogPerformanceResponse.CatalogDetails.builder()
                    .catalogId(campaignPerformance.getCatalogId())
                    .campaignId(campaignPerformance.getCampaignId())
                    .budgetUtilised(campaignPerformance.getBudgetUtilised())
                    .revenue(campaignPerformance.getRevenue())
                    .totalClicks(campaignPerformance.getTotalClicks())
                    .totalViews(campaignPerformance.getTotalViews())
                    .orderCount(campaignPerformance.getOrderCount())
                    .conversionRate(CalculationUtils.getConversionRate(
                            campaignPerformance.getOrderCount(), campaignPerformance.getTotalClicks()))
                    .roi(CalculationUtils.getRoi(
                            campaignPerformance.getRevenue(), campaignPerformance.getBudgetUtilised()))
                    .build());
            log.info("performance details: {}", performanceDetailsList);
        }
        return CampaignCatalogPerformanceResponse.builder().catalogs(performanceDetailsList).build();
    }

    public BudgetUtilisedResponse getBudgetUtilisedResponse(List<CampaignMetrics> campaignMetrics,
                                                                                          List<CampaignDatewiseMetrics> campaignDatewiseMetrics) {
        List<com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails> budgetUtilisedDetails = new ArrayList<>();
        for (CampaignMetrics campaignMetric : campaignMetrics) {
            budgetUtilisedDetails.add(com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails.builder()
                    .campaignId(campaignMetric.getCampaignId())
                    .budgetUtilised(campaignMetric.getBudgetUtilised())
                    .build());
        }

        for (CampaignDatewiseMetrics campaignDatewiseMetric : campaignDatewiseMetrics) {
            budgetUtilisedDetails.add(com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails.builder()
                    .campaignId(campaignDatewiseMetric.getCampaignId())
                    .budgetUtilised(campaignDatewiseMetric.getBudgetUtilised())
                    .build());
        }

        return BudgetUtilisedResponse.builder().budgetUtilisedDetails(budgetUtilisedDetails).build();
    }

    public static CampaignCatalogMetrics getCampaignCatalogMetricsFromRequest(Long campaignId, Long catalogId) {
        return CampaignCatalogMetrics.builder()
                .campaignId(campaignId)
                .catalogId(catalogId)
                .viewCount(0L)
                .weightedClickCount(BigDecimal.ZERO)
                .weightedSharesCount(BigDecimal.ZERO)
                .weightedWishlistCount(BigDecimal.ZERO)
                .budgetUtilised(BigDecimal.ZERO)
                .originWiseClickCount(new HashMap<>())
                .country(Utils.getCountry())
                .build();
    }

    public static CampaignCatalogMetrics getCampaignCatalogMetricsFromExistingEntity(Long campaignId, Long catalogId,
                                                                              CampaignCatalogMetrics existingEntity) {
        return CampaignCatalogMetrics.builder()
                .campaignId(campaignId)
                .catalogId(catalogId)
                .weightedClickCount(BigDecimal.ZERO)
                .weightedSharesCount(BigDecimal.ZERO)
                .weightedWishlistCount(BigDecimal.ZERO)
                .budgetUtilised(BigDecimal.ZERO)
                .originWiseClickCount(new HashMap<>())
                .country(Utils.getCountry())
                .build();
    }

}
