package com.meesho.cps.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.elasticsearch.ESBasePerformanceMetricsDocument;
import com.meesho.cps.data.entity.elasticsearch.ESDailyIndexDocument;
import com.meesho.cps.data.entity.elasticsearch.EsCampaignCatalogAggregateResponse;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.data.internal.PerformancePojo;
import com.meesho.cps.data.presto.CampaignPerformancePrestoData;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.utils.CalculationUtils;
import com.meesho.cpsclient.response.*;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.ml.job.results.Bucket;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 05/08/21
 */
@Service
@Slf4j
public class CampaignPerformanceTransformer {

    @Autowired
    private CampaignPerformanceHelper campaignPerformanceHelper;

    @Autowired
    private ObjectMapper objectMapper;

    public BudgetUtilisedResponse getBudgetUtilisedResponse(List<CampaignMetrics> campaignMetrics,
                                                            List<CampaignDatewiseMetrics> campaignDatewiseMetrics,
                                                            List<SupplierWeekWiseMetrics> supplierWeekWiseMetrics) {
        List<com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails> budgetUtilisedDetails = new ArrayList<>();
        for (CampaignMetrics campaignMetric : campaignMetrics) {
            budgetUtilisedDetails.add(com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails.builder()
                    .campaignId(campaignMetric.getCampaignId())
                    .budgetUtilised(campaignMetric.getBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        for (CampaignDatewiseMetrics campaignDatewiseMetric : campaignDatewiseMetrics) {
            budgetUtilisedDetails.add(com.meesho.cpsclient.response.BudgetUtilisedResponse.BudgetUtilisedDetails.builder()
                    .campaignId(campaignDatewiseMetric.getCampaignId())
                    .budgetUtilised(campaignDatewiseMetric.getBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
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

    public static CampaignCatalogDateMetrics transform(CampaignPerformancePrestoData campaignPerformancePrestoData) {
        return CampaignCatalogDateMetrics.builder()
                .campaignId(campaignPerformancePrestoData.getCampaignId())
                .catalogId(campaignPerformancePrestoData.getCatalogId())
                .date(LocalDate.parse(campaignPerformancePrestoData.getDate()))
                .orders(campaignPerformancePrestoData.getOrderCount())
                .revenue(BigDecimal.valueOf(campaignPerformancePrestoData.getRevenue()))
                .build();
    }

    public SupplierPerformanceResponse getSupplierPerformanceResponse(EsCampaignCatalogAggregateResponse monthWiseResponse,
                                                                      EsCampaignCatalogAggregateResponse dateWiseResponse) {

        PerformancePojo pp = getPerformancePojoFromAggregations(dateWiseResponse.getAggregations(), monthWiseResponse.getAggregations());
        return SupplierPerformanceResponse.builder()
                .budgetUtilised(pp.getTotalBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                .revenue(pp.getTotalRevenue().setScale(2, RoundingMode.HALF_UP))
                .totalViews(pp.getTotalViews())
                .totalClicks(pp.getTotalClicks() + pp.getTotalShares() + pp.getTotalWishlist())
                .orderCount(pp.getTotalOrders())
                .conversionRate(CalculationUtils.getConversionRate(pp.getTotalOrders(), pp.getTotalClicks()))
                .roi(CalculationUtils.getRoi(pp.getTotalRevenue(), pp.getTotalBudgetUtilised()))
                .build();
    }

    public CampaignPerformanceResponse getCampaignPerformanceResponse(EsCampaignCatalogAggregateResponse monthWiseResponse,
                                                                      EsCampaignCatalogAggregateResponse dateWiseResponse,
                                                                      List<Long> campaignIds) {

        Terms dateWiseTerms = Optional.ofNullable(dateWiseResponse.getAggregations())
                .map(ag -> (Terms) ag.get(Constants.ESConstants.BY_CAMPAIGN)).orElse(null);
        Terms monthWiseTerms = Optional.ofNullable(monthWiseResponse.getAggregations())
                .map(ag -> (Terms) ag.get(Constants.ESConstants.BY_CAMPAIGN)).orElse(null);

        List<CampaignPerformanceResponse.CampaignDetails> campaignDetailsList = new ArrayList<>();

        for (Long campaignId : campaignIds) {
            Terms.Bucket campaignLevelAggregationsDateWise = Optional.ofNullable(dateWiseTerms)
                    .map(t -> t.getBucketByKey(campaignId.toString())).orElse(null);
            Terms.Bucket campaignLevelAggregationsMonthWise = Optional.ofNullable(monthWiseTerms)
                    .map(t -> t.getBucketByKey(campaignId.toString())).orElse(null);

            PerformancePojo pp = getPerformancePojoFromAggregations(Optional.ofNullable(campaignLevelAggregationsDateWise)
                    .map(Terms.Bucket::getAggregations).orElse(null), Optional.ofNullable(campaignLevelAggregationsMonthWise)
                    .map(Terms.Bucket::getAggregations).orElse(null));

            campaignDetailsList.add(CampaignPerformanceResponse.CampaignDetails.builder().campaignId(campaignId)
                    .budgetUtilised(pp.getTotalBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                    .totalClicks(pp.getTotalClicks() + pp.getTotalShares() + pp.getTotalWishlist())
                    .totalViews(pp.getTotalViews())
                    .revenue(pp.getTotalRevenue().setScale(2, RoundingMode.HALF_UP))
                    .orderCount(pp.getTotalOrders())
                    .roi(CalculationUtils.getRoi(pp.getTotalRevenue(), pp.getTotalBudgetUtilised()))
                    .conversionRate(CalculationUtils.getConversionRate(pp.getTotalOrders(), pp.getTotalClicks())).build());
        }
        return CampaignPerformanceResponse.builder().campaigns(campaignDetailsList).build();
    }

    public CampaignCatalogPerformanceResponse getCampaignCatalogPerformanceResponse(EsCampaignCatalogAggregateResponse monthWiseResponse,
                                                                                    EsCampaignCatalogAggregateResponse dateWiseResponse,
                                                                                    Long campaignId, List<Long> catalogIds) {

        Terms dateWiseTerms = Optional.ofNullable(dateWiseResponse.getAggregations())
                .map(ag -> (Terms) ag.get(Constants.ESConstants.BY_CATALOG)).orElse(null);
        Terms monthWiseTerms = Optional.ofNullable(monthWiseResponse.getAggregations())
                .map(ag -> (Terms) ag.get(Constants.ESConstants.BY_CATALOG)).orElse(null);
        List<CampaignCatalogPerformanceResponse.CatalogDetails> catalogDetailsList = new ArrayList<>();

        for (Long catalogId : catalogIds) {
            Terms.Bucket campaignLevelAggregationsDateWise = Optional.ofNullable(dateWiseTerms)
                    .map(t -> t.getBucketByKey(catalogId.toString())).orElse(null);
            Terms.Bucket campaignLevelAggregationsMonthWise = Optional.ofNullable(monthWiseTerms)
                    .map(t -> t.getBucketByKey(catalogId.toString())).orElse(null);

            // add only if at least one level aggregation present
            if(campaignLevelAggregationsDateWise == null && campaignLevelAggregationsMonthWise == null) {
                continue;
            }

            PerformancePojo pp = getPerformancePojoFromAggregations(Optional.ofNullable(campaignLevelAggregationsDateWise)
                    .map(Terms.Bucket::getAggregations).orElse(null), Optional.ofNullable(campaignLevelAggregationsMonthWise)
                    .map(Terms.Bucket::getAggregations).orElse(null));

            catalogDetailsList.add(CampaignCatalogPerformanceResponse.CatalogDetails.builder().campaignId(campaignId)
                    .budgetUtilised(pp.getTotalBudgetUtilised().setScale(2, RoundingMode.HALF_UP))
                    .totalClicks(pp.getTotalClicks() + pp.getTotalShares() + pp.getTotalWishlist())
                    .totalViews(pp.getTotalViews())
                    .revenue(pp.getTotalRevenue().setScale(2, RoundingMode.HALF_UP))
                    .orderCount(pp.getTotalOrders())
                    .roi(CalculationUtils.getRoi(pp.getTotalRevenue(), pp.getTotalBudgetUtilised()))
                    .conversionRate(CalculationUtils.getConversionRate(pp.getTotalOrders(), pp.getTotalClicks())).catalogId(catalogId).build());
        }
        return CampaignCatalogPerformanceResponse.builder().catalogs(catalogDetailsList).build();
    }

/*    public CampaignPerformanceDatewiseResponse getCampaignCatalogDatewisePerformanceResponse(
            EsCampaignCatalogAggregateResponse dateWiseResponse,
            Long campaignId, List<Long> catalogIds) {
*//*

        Terms dateWiseTerms = Optional.ofNullable(dateWiseResponse.getAggregations())
                .map(ag -> (Terms) ag.get(Constants.ESConstants.BY_CATALOG)).orElse(null);
        CampaignPerformanceDatewiseResponse.CatalogDetailsDatewise catalogDetailsDatewise =
                CampaignPerformanceDatewiseResponse.CatalogDetailsDatewise.builder().build();

*//*


        return CampaignPerformanceDatewiseResponse.builder()
                .campaignPerfDatewise(catalogDetailsDatewise)
                .campaignId(campaignId)
                .build();
    }*/

    private Double sumAggregates(Aggregations dateWise, Aggregations monthWise, String fieldName) {
        Double totalAggregateDateWise = Optional.ofNullable(dateWise).map(mw -> ((Sum) mw.get(fieldName)).getValue()).orElse(0.0);
        Double totalAggregateMonthWise = Optional.ofNullable(monthWise).map(mw -> ((Sum) mw.get(fieldName)).getValue()).orElse(0.0);
        return totalAggregateDateWise + totalAggregateMonthWise;
    }

    private PerformancePojo getPerformancePojoFromAggregations(Aggregations dateWise, Aggregations monthWise) {
        Long totalShares = sumAggregates(dateWise, monthWise, Constants.ESConstants.TOTAL_SHARES).longValue();
        Long totalViews = sumAggregates(dateWise, monthWise, Constants.ESConstants.TOTAL_VIEWS).longValue();
        Long totalWishlist = sumAggregates(dateWise, monthWise, Constants.ESConstants.TOTAL_WISHLIST).longValue();
        Long totalClicks = sumAggregates(dateWise, monthWise, Constants.ESConstants.TOTAL_CLICKS).longValue();
        BigDecimal totalRevenue = BigDecimal.valueOf(sumAggregates(dateWise, monthWise, Constants.ESConstants.TOTAL_REVENUE));
        BigDecimal totalBudgetUtilised = BigDecimal.valueOf(sumAggregates(dateWise, monthWise, Constants.ESConstants.TOTAL_BUDGET_UTILISED));
        Integer totalOrders = sumAggregates(dateWise, monthWise, Constants.ESConstants.TOTAL_ORDERS).intValue();

        return PerformancePojo.builder().totalOrders(totalOrders).totalShares(totalShares).totalRevenue(totalRevenue)
                .totalWishlist(totalWishlist).totalClicks(totalClicks).totalBudgetUtilised(totalBudgetUtilised)
                .totalViews(totalViews).build();
    }

    public FetchActiveCampaignsResponse getFetchActiveCampaignsResponse(SearchResponse searchResponse) throws JsonProcessingException {

        SearchHits searchHits = searchResponse.getHits();
        String scrollId = searchResponse.getScrollId();

        Map<Long, List<ESDailyIndexDocument>> campaignIdToESDailyIndexDocumentMap = new HashMap<>();
        for(SearchHit searchHit: searchHits.getHits()) {
            ESDailyIndexDocument esDailyIndexDocument = objectMapper.readValue(searchHit.getSourceAsString(), ESDailyIndexDocument.class);
            if(!campaignIdToESDailyIndexDocumentMap.containsKey(esDailyIndexDocument.getCampaignId())) {
                campaignIdToESDailyIndexDocumentMap.put(esDailyIndexDocument.getCampaignId(), new ArrayList<>());
            }
            campaignIdToESDailyIndexDocumentMap.get(esDailyIndexDocument.getCampaignId()).add(esDailyIndexDocument);
        }

        List<FetchActiveCampaignsResponse.CampaignDetails> campaignDetailsList = new ArrayList<>();

        campaignIdToESDailyIndexDocumentMap.forEach((campaignId, esDailyIndexDocumentList) -> {
            FetchActiveCampaignsResponse.CampaignDetails campaignDetails = FetchActiveCampaignsResponse.CampaignDetails.builder()
                    .supplierID(esDailyIndexDocumentList.get(0).getSupplierId())
                    .campaignID(esDailyIndexDocumentList.get(0).getCampaignId())
                    .catalogIds(esDailyIndexDocumentList.stream().map(ESBasePerformanceMetricsDocument::getCatalogId).collect(Collectors.toList()))
                    .build();
            campaignDetailsList.add(campaignDetails);
        });

        if(searchHits.getHits().length == 0) {
            scrollId=null;
        }

        return FetchActiveCampaignsResponse.builder()
                .activeCampaigns(campaignDetailsList)
                .cursor(campaignPerformanceHelper.encodeCursor(scrollId))
                .build();

    }

}
