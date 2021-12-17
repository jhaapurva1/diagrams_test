package com.meesho.cps.transformer;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.request.CampaignCatalogDateMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignDatewiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;

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
        }
        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getClickCount()))
            campaignCatalogDateMetrics.setClickCount(campaignCatalogMetricsSaveRequest.getClickCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getSharesCount()))
            campaignCatalogDateMetrics.setSharesCount(campaignCatalogMetricsSaveRequest.getSharesCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getWishlistCount()))
            campaignCatalogDateMetrics.setWishlistCount(
                    campaignCatalogMetricsSaveRequest.getWishlistCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getViewCount()))
            campaignCatalogDateMetrics.setViewCount(campaignCatalogMetricsSaveRequest.getViewCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getDate()))
            campaignCatalogDateMetrics.setDate(campaignCatalogMetricsSaveRequest.getDate());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getOrders()))
            campaignCatalogDateMetrics.setOrders(campaignCatalogMetricsSaveRequest.getOrders());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getRevenue()))
            campaignCatalogDateMetrics.setRevenue(campaignCatalogMetricsSaveRequest.getRevenue());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getBudgetUtilised()))
            campaignCatalogDateMetrics.setBudgetUtilised(campaignCatalogMetricsSaveRequest.getBudgetUtilised());

        return campaignCatalogDateMetrics;
    }

    public static CampaignMetrics transform(CampaignMetricsSaveRequest request) {
        return CampaignMetrics.builder()
                .campaignId(request.getCampaignId())
                .budgetUtilised(request.getBudgetUtilised())
                .build();
    }

    public static CampaignDatewiseMetrics transform(CampaignDatewiseMetricsSaveRequest request) {
        return CampaignDatewiseMetrics.builder()
                .campaignId(request.getCampaignId())
                .budgetUtilised(request.getBudgetUtilised())
                .date(DateTimeUtils.getCurrentLocalDateTimeInIST().toLocalDate())
                .build();
    }

}
