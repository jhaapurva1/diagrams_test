package com.meesho.cps.service;

import com.google.common.collect.Lists;
import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.data.entity.kafka.AdViewEvent;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.service.external.AdService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.instrumentation.metric.statsd.StatsdMetricManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import static com.meesho.cps.constants.TelegrafConstants.*;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Service
@Slf4j
public class CatalogViewEventService {

    @Autowired
    private CampaignCatalogMetricsRepository campaignCatalogMetricsRepository;

    @Autowired
    private AdService adService;

    @Autowired
    private StatsdMetricManager statsdMetricManager;

    @Autowired
    private ApplicationProperties applicationProperties;

    private String getCampaignCatalogKey(Long campaignId, Long catalogId) {
        return String.valueOf(campaignId).concat("_").concat(String.valueOf(catalogId));
    }

    private Map<String, CampaignCatalogViewCount> getCampaignCatalogViewCountsFromCatalogMetadata(
            List<AdViewEvent> adViewEvents, Map<Long, CampaignCatalogMetadataResponse.CatalogMetadata> catalogMetadataMap) {

        Map<String, CampaignCatalogViewCount> campaignCatalogViewCountMap = new HashMap<>();

        for (AdViewEvent adViewEvent : adViewEvents) {

            Long catalogId = adViewEvent.getProperties().getId();
            CampaignCatalogMetadataResponse.CatalogMetadata catalogMetadata = catalogMetadataMap.get(catalogId);
            if(Objects.isNull(catalogMetadata) || !catalogMetadata.getCampaignActive()){
                log.error("No active ad on catalogId {} userId {} eventId {}",adViewEvent.getProperties().getId(),adViewEvent.getUserId(),adViewEvent.getEventId());
                statsdMetricManager.incrementCounter(VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                        adViewEvent.getEventName(), adViewEvent.getProperties().getOrigin(), INVALID,
                        AdInteractionInvalidReason.CAMPAIGN_INACTIVE));
                continue;
            }
            Long campaignId = catalogMetadata.getCampaignDetails().getCampaignId();

            log.info("Processing view event for eventId {} catalogId {} campaignId {} userId {} appVersionCode {}",
                    adViewEvent.getEventId(), adViewEvent.getProperties().getId(), campaignId, adViewEvent.getUserId(),
                    adViewEvent.getProperties().getAppVersionCode());
            statsdMetricManager.incrementCounter(VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                    adViewEvent.getEventName(), adViewEvent.getProperties().getOrigin(), VALID, NAN));


            String campaignCatalogViewCountKey = getCampaignCatalogKey(campaignId, catalogId);

            if (campaignCatalogViewCountMap.containsKey(campaignCatalogViewCountKey)) {
                CampaignCatalogViewCount campaignCatalogViewCount =
                        campaignCatalogViewCountMap.get(campaignCatalogViewCountKey);
                campaignCatalogViewCount.setCount(campaignCatalogViewCount.getCount() + 1);
                campaignCatalogViewCountMap.put(campaignCatalogViewCountKey, campaignCatalogViewCount);
            } else {
                campaignCatalogViewCountMap.put(campaignCatalogViewCountKey,
                        CampaignCatalogViewCount.builder().campaignId(campaignId).catalogId(catalogId).count(1).build());
            }
        }

        return campaignCatalogViewCountMap;
    }

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "class=CatalogViewEventService")
    public void handle(List<AdViewEvent> adViewEvents) throws ExternalRequestFailedException {

        List<Long> catalogIds = adViewEvents.stream()
                .map(adViewEvent -> adViewEvent.getProperties().getId()).distinct().collect(Collectors.toList());

        List<CampaignCatalogMetadataResponse.CatalogMetadata> responses = adService.getCampaignMetadataFromCache(catalogIds);

        if (CollectionUtils.isEmpty(responses)) {
            log.error("No active campaign found for catalogs {}", catalogIds);
            return;
        }

        Map<Long, CampaignCatalogMetadataResponse.CatalogMetadata> responsesMap =
                responses.stream().collect(Collectors.toMap(CampaignCatalogMetadataResponse.CatalogMetadata::getCatalogId, Function.identity()));

        List<CampaignCatalogViewCount> campaignCatalogViewCountList =
                new ArrayList<>(getCampaignCatalogViewCountsFromCatalogMetadata(adViewEvents, responsesMap).values());

        List<List<CampaignCatalogViewCount>> campaignCatalogViewCountPartitionedList =
                Lists.partition(campaignCatalogViewCountList, applicationProperties.getIncrementViewHbaseBatchSize());

        for (List<CampaignCatalogViewCount> eachPartitionedList : campaignCatalogViewCountPartitionedList) {
            campaignCatalogMetricsRepository.bulkIncrementViewCount(eachPartitionedList);
        }

    }

}
