package com.meesho.cps.service.presto;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.data.presto.CampaignPerformancePrestoData;
import com.meesho.cps.db.mongodb.dao.CampaignCatalogDateMetricsDao;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
@Slf4j
@Service
public class CampaignPerformanceHandler {

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Autowired
    private AdService adService;

    public void handle(List<CampaignPerformancePrestoData> campaignPerformancePrestoDataList) throws ExternalRequestFailedException {
        List<String> keys = campaignPerformancePrestoDataList.stream()
                .map(this::getUniqueKey)
                .collect(Collectors.toList());
        List<Long> campaignIds = campaignPerformancePrestoDataList.stream().map(CampaignPerformancePrestoData::getCampaignId).collect(Collectors.toList());

        List<CampaignDetails> campaignDetails = adService.getCampaignMetadata(campaignIds);
        if (Objects.isNull(campaignDetails)) {
            log.error("campaign metadata request failed for campaignIds - {}", campaignIds);
            throw new ExternalRequestFailedException("Failed to fetch campaign metadata from ads-admin");
        }
        Map<Long, Long> campaignIdToSupplierIdMap = campaignDetails.stream().collect(Collectors.toMap(CampaignDetails::getCampaignId, CampaignDetails::getSupplierId));

        List<CampaignCatalogDateMetrics> documentList = new ArrayList<>();

        for (CampaignPerformancePrestoData prestoData : campaignPerformancePrestoDataList) {
            if (Objects.isNull(campaignIdToSupplierIdMap.get(prestoData.getCampaignId()))) {
                log.error("Could not fetch campaign metadata for campaignId - {}", prestoData.getCampaignId());
                throw new ExternalRequestFailedException("Failed to fetch campaign metadata from ads-admin");
            }
            CampaignCatalogDateMetrics document = CampaignPerformanceTransformer.transform(prestoData, campaignIdToSupplierIdMap.get(prestoData.getCampaignId()));
            documentList.add(document);
        }
        campaignCatalogDateMetricsDao.bulkWriteOrderAndRevenue(documentList);

        //update redis set
        List<CampaignCatalogDate> campaignCatalogDates = documentList.stream()
                .map(x -> new CampaignCatalogDate(x.getCampaignId(), x.getCatalogId(), x.getDate()))
                .collect(Collectors.toList());
        updatedCampaignCatalogCacheDao.add(campaignCatalogDates);
        log.info("CampaignPerformance scheduler processed result set for campaign_catalog_date {} ", keys);

    }

    public String getUniqueKey(CampaignPerformancePrestoData entity) {
        return entity.getCampaignId() + "_" +  entity.getCatalogId() + "_" +entity.getDate();
    }


}
