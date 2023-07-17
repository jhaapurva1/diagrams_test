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

        Map<Long, Long> campaignIdToSupplierIdMap = getCampaignIdToSupplierIdMap(campaignPerformancePrestoDataList);

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
        addUpdatedKeysToCache(documentList);
    }

    private Map<Long, Long> getCampaignIdToSupplierIdMap(List<CampaignPerformancePrestoData> campaignPerformancePrestoDataList) throws ExternalRequestFailedException {
        List<Long> campaignIds = campaignPerformancePrestoDataList.stream().map(CampaignPerformancePrestoData::getCampaignId).collect(Collectors.toList());

        List<CampaignDetails> campaignDetails = adService.getCampaignMetadata(campaignIds);
        if (Objects.isNull(campaignDetails)) {
            log.error("campaign metadata request failed for campaignIds - {}", campaignIds);
            throw new ExternalRequestFailedException("Failed to fetch campaign metadata from ads-admin");
        }
        return campaignDetails.stream().collect(Collectors.toMap(CampaignDetails::getCampaignId, CampaignDetails::getSupplierId));
    }

    private void addUpdatedKeysToCache(List<CampaignCatalogDateMetrics> documentList) {
        List<CampaignCatalogDate> campaignCatalogDates = documentList.stream()
                .map(x -> new CampaignCatalogDate(x.getCampaignId(), x.getCatalogId(), x.getDate()))
                .collect(Collectors.toList());
        updatedCampaignCatalogCacheDao.add(campaignCatalogDates);
    }

    public String getUniqueKey(CampaignPerformancePrestoData entity) {
        return entity.getCampaignId() + "_" +  entity.getCatalogId() + "_" +entity.getDate();
    }


}
