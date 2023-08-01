package com.meesho.cps.service;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.mongodb.dao.CampaignCatalogDateMetricsDao;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cps.transformer.PrismEventTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Slf4j
@Service
public class DayWisePerformanceMetricsService {

    @Autowired
    private CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private PrismService prismService;

    public void handleMessage(List<CampaignCatalogDate> messages) throws Exception {
        log.info("Processing campaignCatalogDate event {}", messages);

        // delete keys from updatedCampaignCatalogSet. Deleting before reading from mongo, to avoid race conditions with live updates happening to the set
        updatedCampaignCatalogCacheDao.delete(messages);
        List<CampaignCatalogDateMetrics> documentList = new ArrayList<>();
        for (CampaignCatalogDate campaignCatalogDate : messages) {
            Long campaignId = campaignCatalogDate.getCampaignId();
            Long catalogId = campaignCatalogDate.getCatalogId();
            String date = campaignCatalogDate.getDate();

            CampaignCatalogDateMetrics document = campaignCatalogDateMetricsDao.find(campaignId, catalogId, date);
            documentList.add(document);
        }

        if (!CollectionUtils.isEmpty(documentList)) {
            log.info("day perf events -- " + documentList);
            prismService.publishEvent(Constants.PrismEventNames.DAY_WISE_PERF_EVENTS,
                    PrismEventTransformer.getDayWisePerformancePrismEvent(documentList));
        }
    }

}
