package com.meesho.cps.service;

import com.google.common.collect.Lists;
import com.meesho.ad.client.response.AdViewEventMetadataResponse;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.data.entity.kafka.AdViewEvent;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.service.external.AdService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Service
@Slf4j
public class CatalogViewEventService {

    @Autowired
    private CampaignCatalogDateMetricsRepository campaignCatalogMetricsRepository;

    @Autowired
    private AdService adService;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "class=CatalogViewEventService")
    public Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> getCampaignCatalogMetadataFromAdViewEvents(List<AdViewEvent> adViewEvents) throws ExternalRequestFailedException {

        List<Long> catalogIds = adViewEvents.stream()
                .map(adViewEvent -> adViewEvent.getProperties().getId()).distinct().collect(Collectors.toList());

        List<AdViewEventMetadataResponse.CatalogCampaignMetadata> responses = adService.getCampaignMetadataFromCache(catalogIds);

        if (CollectionUtils.isEmpty(responses)) {
            log.error("No active campaign found for catalogs {}", catalogIds);
            return null;
        }

        return responses.stream().collect(Collectors.toMap(AdViewEventMetadataResponse.CatalogCampaignMetadata::getCatalogId, Function.identity()));
    }

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "className=CatalogViewEventService,method=writeToHbase")
    public void writeToHbase(List<CampaignCatalogViewCount> campaignCatalogViewCountList) {
        List<List<CampaignCatalogViewCount>> campaignCatalogViewCountPartitionedList =
                Lists.partition(campaignCatalogViewCountList, applicationProperties.getIncrementViewHbaseBatchSize());

        for (List<CampaignCatalogViewCount> eachPartitionedList : campaignCatalogViewCountPartitionedList) {
            campaignCatalogMetricsRepository.bulkIncrementViewCount(eachPartitionedList);
        }

        // update redis set for campaign_catalog_date
        List<CampaignCatalogDate> campaignCatalogDates = campaignCatalogViewCountList.stream()
                .map(x -> new CampaignCatalogDate(x.getCampaignId(), x.getCatalogId(), x.getDate().toString()))
                .collect(Collectors.toList());
        updatedCampaignCatalogCacheDao.add(campaignCatalogDates);
    }

}
