package com.meesho.cps.transformer;

import com.meesho.ad.client.constants.FeedType;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CatalogCPCDiscount;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.presto.CampaignCatalogReconciledMetricsPrestoData;
import com.meesho.cps.data.request.CampaignDateWiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;
import com.meesho.cps.data.request.CatalogCPCDiscountSaveRequest;
import com.meesho.cps.data.request.CampaignCatalogDateMetricsSaveRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
public class DebugTransformer {

    public static CampaignCatalogDateMetrics getCampaignCatalogMetrics(
            CampaignCatalogDateMetricsSaveRequest campaignCatalogMetricsSaveRequest,
            CampaignCatalogDateMetrics campaignCatalogDateMetrics) {
        if (Objects.isNull(campaignCatalogDateMetrics)) {
            campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
            campaignCatalogDateMetrics.setCampaignId(campaignCatalogMetricsSaveRequest.getCampaignId());
            campaignCatalogDateMetrics.setCatalogId(campaignCatalogMetricsSaveRequest.getCatalogId());
            campaignCatalogDateMetrics.setSupplierId(campaignCatalogMetricsSaveRequest.getSupplierId());
            campaignCatalogDateMetrics.setDate(campaignCatalogMetricsSaveRequest.getDate().toString());
        }
        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getClickCount()))
            campaignCatalogDateMetrics.setClicks(campaignCatalogMetricsSaveRequest.getClickCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getSharesCount()))
            campaignCatalogDateMetrics.setShares(campaignCatalogMetricsSaveRequest.getSharesCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getWishlistCount()))
            campaignCatalogDateMetrics.setWishlists(
                    campaignCatalogMetricsSaveRequest.getWishlistCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getViewCount()))
            campaignCatalogDateMetrics.setViews(campaignCatalogMetricsSaveRequest.getViewCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getOrders()))
            campaignCatalogDateMetrics.setOrders(campaignCatalogMetricsSaveRequest.getOrders());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getRevenue()))
            campaignCatalogDateMetrics.setRevenue(campaignCatalogMetricsSaveRequest.getRevenue());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getBudgetUtilised()))
            campaignCatalogDateMetrics.setBudgetUtilised(campaignCatalogMetricsSaveRequest.getBudgetUtilised());

        return campaignCatalogDateMetrics;
    }


    public static CampaignCatalogDateMetrics convertCampaignCatalogMetricsFromCampaignCatalogPrestoMetrics(
            CampaignCatalogReconciledMetricsPrestoData campaignCatalogMetricsSaveRequest,
            CampaignCatalogDateMetrics campaignCatalogDateMetrics, Long supplierId) {
        if (Objects.isNull(campaignCatalogDateMetrics)) {
            campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
            campaignCatalogDateMetrics.setCampaignId(campaignCatalogMetricsSaveRequest.getCampaignId());
            campaignCatalogDateMetrics.setCatalogId(campaignCatalogMetricsSaveRequest.getCatalogId());
        }
        campaignCatalogDateMetrics.setSupplierId(supplierId);
        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getClickCount()))
            campaignCatalogDateMetrics.setClicks(campaignCatalogMetricsSaveRequest.getClickCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getSharesCount()))
            campaignCatalogDateMetrics.setShares(campaignCatalogMetricsSaveRequest.getSharesCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getWishlistCount()))
            campaignCatalogDateMetrics.setWishlists(campaignCatalogMetricsSaveRequest.getWishlistCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getEventDate()))
            campaignCatalogDateMetrics.setDate(LocalDate.parse(campaignCatalogMetricsSaveRequest.getEventDate()).toString());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getBudgetUtilised()))
            campaignCatalogDateMetrics.setBudgetUtilised(BigDecimal.valueOf(campaignCatalogMetricsSaveRequest.getBudgetUtilised()));

        return campaignCatalogDateMetrics;
    }

    public static CatalogCPCDiscount transform(CatalogCPCDiscountSaveRequest request, CatalogCPCDiscount catalogCPCDiscount) {
        if (Objects.isNull(catalogCPCDiscount)) {
            catalogCPCDiscount = new CatalogCPCDiscount();
        }

        catalogCPCDiscount.setCatalogId(request.getCatalogId());
        catalogCPCDiscount.setDiscount(request.getDiscount());

        return catalogCPCDiscount;
    }

    public static CampaignMetrics transform(CampaignMetricsSaveRequest request) {
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        for(CampaignMetricsSaveRequest.RealEstateBudgetUtilised realEstateBudgetUtilised :
                request.getRealEstateBudgetUtilisedList()){
            realEstateBudgetUtilisedMap.put(realEstateBudgetUtilised.getRealEstate(), realEstateBudgetUtilised.getBudgetUtilised());
        }
        return CampaignMetrics.builder()
                .campaignId(request.getCampaignId())
                .budgetUtilised(request.getBudgetUtilised())
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();
    }

    public static CampaignDateWiseMetrics transform(CampaignDateWiseMetricsSaveRequest request) {
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        for(CampaignDateWiseMetricsSaveRequest.RealEstateBudgetUtilised realEstateBudgetUtilised :
                request.getRealEstateBudgetUtilisedList()){
            realEstateBudgetUtilisedMap.put(realEstateBudgetUtilised.getRealEstate(), realEstateBudgetUtilised.getBudgetUtilised());
        }
        return CampaignDateWiseMetrics.builder()
                .campaignId(request.getCampaignId())
                .budgetUtilised(request.getBudgetUtilised())
                .date(DateTimeUtils.getCurrentLocalDateTimeInIST().toLocalDate().toString())
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();
    }

    public static CatalogCPCDiscount transform(CatalogCPCDiscountSaveRequest request) {
        return CatalogCPCDiscount.builder()
                .catalogId(request.getCatalogId())
                .discount(request.getDiscount())
                .build();
    }

}
