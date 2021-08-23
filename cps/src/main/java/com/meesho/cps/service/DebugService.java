package com.meesho.cps.service;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.ads.lib.utils.HbaseUtils;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.request.CampaignCatalogMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignDatewiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.transformer.DebugTransformer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@Service
public class DebugService {

    @Autowired
    CampaignMetricsRepository campaignMetricsRepository;

    @Autowired
    CampaignCatalogMetricsRepository campaignCatalogMetricsRepository;

    @Autowired
    CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    public CampaignCatalogMetrics saveCampaignCatalogMetrics(
            CampaignCatalogMetricsSaveRequest campaignCatalogMetricsSaveRequest) throws Exception {
        CampaignCatalogMetrics campaignCatalogMetrics =
                campaignCatalogMetricsRepository.get(campaignCatalogMetricsSaveRequest.getCampaignId(),
                        campaignCatalogMetricsSaveRequest.getCatalogId());
        campaignCatalogMetrics =
                DebugTransformer.getCampaignCatalogMetrics(campaignCatalogMetricsSaveRequest, campaignCatalogMetrics);
        campaignCatalogMetricsRepository.put(campaignCatalogMetrics);
        return campaignCatalogMetrics;
    }

    public CampaignMetrics saveCampaignMetrics(CampaignMetricsSaveRequest campaignMetricsSaveRequest) throws Exception {
        CampaignMetrics campaignMetrics = DebugTransformer.transform(campaignMetricsSaveRequest);
        campaignMetricsRepository.put(campaignMetrics);
        return campaignMetrics;
    }

    public CampaignDatewiseMetrics saveCampaignDatewiseMetrics(
            CampaignDatewiseMetricsSaveRequest campaignDateWiseMetricsSaveRequest) throws Exception {
        CampaignDatewiseMetrics campaignDatewiseMetrics =
                DebugTransformer.transform(campaignDateWiseMetricsSaveRequest);
        campaignDatewiseMetricsRepository.put(campaignDatewiseMetrics);
        return campaignDatewiseMetrics;
    }

    public CampaignDatewiseMetrics getCampaignDatewiseMetrics(Long campaignId, String date) {
        return campaignDatewiseMetricsRepository.get(campaignId,
                DateTimeUtils.getLocalDate(date, HbaseUtils.HBASE_DATE_FORMAT));
    }

    public CampaignMetrics getCampaignMetrics(Long campaignId) {
        return campaignMetricsRepository.get(campaignId);
    }

    public CampaignCatalogMetrics getCampaignCatalogMetrics(Long campaignId, Long catalogId) {
        return campaignCatalogMetricsRepository.get(campaignId, catalogId);
    }

}
