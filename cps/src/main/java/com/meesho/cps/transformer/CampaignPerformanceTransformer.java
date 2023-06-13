package com.meesho.cps.transformer;

import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.mongodb.collection.SupplierWeekWiseMetrics;
import com.meesho.cps.data.entity.mongodb.projection.CampaignCatalogLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.DateLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.CampaignLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.SupplierLevelMetrics;
import com.meesho.cps.data.presto.CampaignPerformancePrestoData;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.utils.CalculationUtils;
import com.meesho.cpsclient.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.meesho.cps.utils.DateTimeHelper.MONGO_DATE_FORMAT;

/**
 * @author shubham.aggarwal
 * 05/08/21
 */
@Service
@Slf4j
public class CampaignPerformanceTransformer {

    @Autowired
    private CampaignPerformanceHelper campaignPerformanceHelper;

    public BudgetUtilisedResponse getBudgetUtilisedResponse(List<CampaignMetrics> campaignMetrics,
                                                            List<CampaignDateWiseMetrics> campaignDateWiseMetrics,
                                                            List<SupplierWeekWiseMetrics> supplierWeekWiseMetrics) {
        List<com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails> budgetUtilisedDetails = new ArrayList<>();
        for (CampaignMetrics campaignMetric : campaignMetrics) {
            budgetUtilisedDetails.add(com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails.builder()
                    .campaignId(campaignMetric.getCampaignId())
                    .budgetUtilised(campaignMetric.getBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        for (CampaignDateWiseMetrics campaignDateWiseMetric : campaignDateWiseMetrics) {
            budgetUtilisedDetails.add(com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails.builder()
                    .campaignId(campaignDateWiseMetric.getCampaignId())
                    .budgetUtilised(campaignDateWiseMetric.getBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        List<BudgetUtilisedResponse.SupplierBudgetUtilisedDetails> supplierBudgetUtilisedDetails = null;
        if (supplierWeekWiseMetrics != null) {
            supplierBudgetUtilisedDetails = new ArrayList<>();
            for (SupplierWeekWiseMetrics supplierWeekWiseMetric : supplierWeekWiseMetrics) {
                supplierBudgetUtilisedDetails.add(BudgetUtilisedResponse.SupplierBudgetUtilisedDetails.builder()
                        .supplierId(supplierWeekWiseMetric.getSupplierId())
                        .budgetUtilised(supplierWeekWiseMetric.getBudgetUtilised()).build());
            }
        }

        return BudgetUtilisedResponse.builder().budgetUtilisedDetails(budgetUtilisedDetails)
                .suppliersBudgetUtilisedDetails(supplierBudgetUtilisedDetails).build();
    }

    public static CampaignCatalogDateMetrics transform(CampaignPerformancePrestoData campaignPerformancePrestoData, Long supplierId) {
        return CampaignCatalogDateMetrics.builder()
                .supplierId(supplierId)
                .campaignId(campaignPerformancePrestoData.getCampaignId())
                .catalogId(campaignPerformancePrestoData.getCatalogId())
                .date(LocalDate.parse(campaignPerformancePrestoData.getDate()).toString())
                .orders(campaignPerformancePrestoData.getOrderCount())
                .revenue(BigDecimal.valueOf(campaignPerformancePrestoData.getRevenue()))
                .build();
    }

    public SupplierPerformanceResponse getSupplierPerformanceResponse(SupplierLevelMetrics supplierLevelMetrics) {

        return SupplierPerformanceResponse.builder()
                .budgetUtilised(supplierLevelMetrics.getBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                .revenue(supplierLevelMetrics.getRevenue().setScale(2, RoundingMode.HALF_UP))
                .totalViews(supplierLevelMetrics.getViews())
                .totalClicks(getTotalClicks(supplierLevelMetrics.getClicks() , supplierLevelMetrics.getShares(), supplierLevelMetrics.getWishlists()))
                .orderCount(supplierLevelMetrics.getOrders())
                .conversionRate(CalculationUtils.getConversionRate(supplierLevelMetrics.getOrders(), supplierLevelMetrics.getClicks()))
                .roi(CalculationUtils.getRoi(supplierLevelMetrics.getRevenue(), supplierLevelMetrics.getBudgetUtilised()))
                .build();
    }

    public CampaignPerformanceResponse getCampaignPerformanceResponse(List<CampaignLevelMetrics> campaignLevelMetricsList) {

        List<CampaignPerformanceResponse.CampaignDetails> campaignDetailsList = new ArrayList<>();

        for (CampaignLevelMetrics campaignLevelMetrics : campaignLevelMetricsList) {

            campaignDetailsList.add(CampaignPerformanceResponse.CampaignDetails.builder()
                .campaignId(campaignLevelMetrics.getCampaignId())
                .budgetUtilised(campaignLevelMetrics.getBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                .totalClicks(getTotalClicks(campaignLevelMetrics.getClicks(), campaignLevelMetrics.getShares(), campaignLevelMetrics.getWishlists()))
                .totalViews(campaignLevelMetrics.getViews())
                .revenue(campaignLevelMetrics.getRevenue().setScale(2, RoundingMode.HALF_UP))
                .orderCount(campaignLevelMetrics.getOrders())
                .roi(CalculationUtils.getRoi(campaignLevelMetrics.getRevenue(), campaignLevelMetrics.getBudgetUtilised()))
                .conversionRate(CalculationUtils.getConversionRate(campaignLevelMetrics.getOrders(), campaignLevelMetrics.getClicks()))
                .build());
        }
        return CampaignPerformanceResponse.builder().campaigns(campaignDetailsList).build();
    }

    public CampaignCatalogPerformanceResponse getCampaignCatalogPerformanceResponse(List<CampaignCatalogLevelMetrics> campaignCatalogLevelMetricsList) {

        List<CampaignCatalogPerformanceResponse.CatalogDetails> catalogDetailsList = new ArrayList<>();

        for (CampaignCatalogLevelMetrics campaignCatalogLevelMetrics : campaignCatalogLevelMetricsList) {

            catalogDetailsList.add(CampaignCatalogPerformanceResponse.CatalogDetails.builder()
                .campaignId(campaignCatalogLevelMetrics.getCampaignIdCatalogId().getCampaignId())
                .catalogId(campaignCatalogLevelMetrics.getCampaignIdCatalogId().getCatalogId())
                .budgetUtilised(campaignCatalogLevelMetrics.getBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                .totalClicks(getTotalClicks(campaignCatalogLevelMetrics.getClicks(), campaignCatalogLevelMetrics.getShares(), campaignCatalogLevelMetrics.getWishlists()))
                .totalViews(campaignCatalogLevelMetrics.getViews())
                .revenue(campaignCatalogLevelMetrics.getRevenue().setScale(2, RoundingMode.HALF_UP))
                .orderCount(campaignCatalogLevelMetrics.getOrders())
                .roi(CalculationUtils.getRoi(campaignCatalogLevelMetrics.getRevenue(), campaignCatalogLevelMetrics.getBudgetUtilised()))
                .conversionRate(CalculationUtils.getConversionRate(campaignCatalogLevelMetrics.getOrders(), campaignCatalogLevelMetrics.getClicks()))
                .build());
        }
        return CampaignCatalogPerformanceResponse.builder().catalogs(catalogDetailsList).build();
    }

    private static Long getTotalClicks(Long clicks, Long shares, Long wishlists) {
        return (clicks == null ? 0 : clicks) + (shares == null ? 0 : shares) + (wishlists == null ? 0 : wishlists);
    }

    public CampaignPerformanceDatewiseResponse getCampaignPerformanceDateWiseResponse(
            List<DateLevelMetrics> dateLevelMetricsList, Long campaignId) {

        Map<LocalDate, CampaignPerformanceDatewiseResponse.GraphDetails> dateWiseMetricsMap = new HashMap<>();
        for (DateLevelMetrics dateLevelMetrics : dateLevelMetricsList) {
            dateWiseMetricsMap.put(LocalDate.parse(dateLevelMetrics.getDate(), DateTimeFormatter.ofPattern(MONGO_DATE_FORMAT)),
                    CampaignPerformanceDatewiseResponse.GraphDetails.builder()
                            .clicks(getTotalClicks(dateLevelMetrics.getClicks(), dateLevelMetrics.getShares(), dateLevelMetrics.getWishlists()))
                            .views(dateLevelMetrics.getViews())
                            .orders(dateLevelMetrics.getOrders())
                            .build());
        }

        return CampaignPerformanceDatewiseResponse.builder()
                .campaignId(campaignId)
                .dateCatalogsMap(dateWiseMetricsMap)
                .build();
    }

    public FetchActiveCampaignsResponse getFetchActiveCampaignsResponse(List<CampaignCatalogDateMetrics> documentList) {

        if(documentList == null || documentList.isEmpty()) {
            return FetchActiveCampaignsResponse.builder()
                    .activeCampaigns(new ArrayList<>())
                    .cursor(campaignPerformanceHelper.encodeCursor(null))
                    .build();
        }

        Map<Long, List<CampaignCatalogDateMetrics>> campaignIdToDocumentMap = documentList.stream()
                .collect(Collectors.groupingBy(CampaignCatalogDateMetrics::getCampaignId));

        String lastProcessedId = documentList.get(documentList.size() - 1).getId().toString();

        List<FetchActiveCampaignsResponse.CampaignDetails> campaignDetails = new ArrayList<>();
        for (Map.Entry<Long, List<CampaignCatalogDateMetrics>> entry : campaignIdToDocumentMap.entrySet()) {
            campaignDetails.add(FetchActiveCampaignsResponse.CampaignDetails.builder()
                            .campaignID(entry.getKey())
                            .catalogIds(entry.getValue().stream().map(CampaignCatalogDateMetrics::getCatalogId)
                                    .collect(Collectors.toList()))
                            .supplierID(entry.getValue().get(0).getSupplierId()).build());
        }

        return FetchActiveCampaignsResponse.builder()
                .activeCampaigns(campaignDetails)
                .cursor(campaignPerformanceHelper.encodeCursor(lastProcessedId))
                .build();

    }

}
