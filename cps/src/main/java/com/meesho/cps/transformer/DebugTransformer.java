package com.meesho.cps.transformer;

import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.commons.enums.Country;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.request.CampaignCatalogMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignDatewiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;

import org.slf4j.MDC;

import java.util.Objects;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
public class DebugTransformer {

    public static CampaignCatalogMetrics getCampaignCatalogMetrics(
            CampaignCatalogMetricsSaveRequest campaignCatalogMetricsSaveRequest,
            CampaignCatalogMetrics campaignCatalogMetrics) {
        if (Objects.isNull(campaignCatalogMetrics)) {
            campaignCatalogMetrics = new CampaignCatalogMetrics();
            campaignCatalogMetrics.setCampaignId(campaignCatalogMetricsSaveRequest.getCampaignId());
            campaignCatalogMetrics.setCatalogId(campaignCatalogMetricsSaveRequest.getCatalogId());
        }
        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getWeightedClickCount()))
            campaignCatalogMetrics.setWeightedClickCount(campaignCatalogMetricsSaveRequest.getWeightedClickCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getWeightedSharesCount()))
            campaignCatalogMetrics.setWeightedSharesCount(campaignCatalogMetricsSaveRequest.getWeightedSharesCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getWeightedWishlistCount()))
            campaignCatalogMetrics.setWeightedWishlistCount(
                    campaignCatalogMetricsSaveRequest.getWeightedWishlistCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getOriginWiseClickCount()))
            campaignCatalogMetrics.setOriginWiseClickCount(campaignCatalogMetricsSaveRequest.getOriginWiseClickCount());

        if (Objects.nonNull(campaignCatalogMetricsSaveRequest.getViewCount()))
            campaignCatalogMetrics.setViewCount(campaignCatalogMetricsSaveRequest.getViewCount());

        campaignCatalogMetrics.setCountry(Country.getValue(MDC.get(Constants.COUNTRY_CODE)));
        return campaignCatalogMetrics;
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
