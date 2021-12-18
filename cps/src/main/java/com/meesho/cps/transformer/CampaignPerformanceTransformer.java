package com.meesho.cps.transformer;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.elasticsearch.EsCampaignCatalogAggregateResponse;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.internal.PerformancePojo;
import com.meesho.cps.data.redshift.CampaignPerformanceRedshift;
import com.meesho.cps.utils.CalculationUtils;
import com.meesho.cpsclient.response.BudgetUtilisedResponse;
import com.meesho.cpsclient.response.CampaignCatalogPerformanceResponse;
import com.meesho.cpsclient.response.CampaignPerformanceResponse;
import com.meesho.cpsclient.response.SupplierPerformanceResponse;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author shubham.aggarwal
 * 05/08/21
 */
@Service
@Slf4j
public class CampaignPerformanceTransformer {

    public BudgetUtilisedResponse getBudgetUtilisedResponse(List<CampaignMetrics> campaignMetrics,
                                                            List<CampaignDatewiseMetrics> campaignDatewiseMetrics) {
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

        return BudgetUtilisedResponse.builder().budgetUtilisedDetails(budgetUtilisedDetails).build();
    }

    public static CampaignCatalogDateMetrics transform(CampaignPerformanceRedshift campaignPerformanceRedshift) {
        return CampaignCatalogDateMetrics.builder()
                .campaignId(campaignPerformanceRedshift.getCampaignId())
                .catalogId(campaignPerformanceRedshift.getCatalogId())
                .date(LocalDate.parse(campaignPerformanceRedshift.getDate()))
                .orders(campaignPerformanceRedshift.getOrderCount())
                .revenue(campaignPerformanceRedshift.getRevenue())
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
}
