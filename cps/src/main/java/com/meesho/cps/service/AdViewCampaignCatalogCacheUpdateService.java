package com.meesho.cps.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.meesho.ad.client.response.AdViewEventMetadataResponse;
import com.meesho.cps.data.entity.kafka.AdViewCampaignCatalogCacheUpdateEvent;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.service.external.AdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdViewCampaignCatalogCacheUpdateService {

    @Autowired
    private AdService adService;

    private final Cache<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> adViewCampaignCatalogCache;

    @Autowired
    public AdViewCampaignCatalogCacheUpdateService(
            @Qualifier("adServiceViewCampaignCatalogCache") Cache<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> adViewCampaignCatalogCache) {
        this.adViewCampaignCatalogCache = adViewCampaignCatalogCache;
    }

    public void handle(List<AdViewCampaignCatalogCacheUpdateEvent> events) throws ExternalRequestFailedException {
        for(AdViewCampaignCatalogCacheUpdateEvent event : events) {
            AdViewEventMetadataResponse campaignCatalogMetadataResponse =
                    adService.getAdViewEventCatalogCampaignStatus(event.getCatalogIds());
            List<AdViewEventMetadataResponse.CatalogCampaignMetadata> catalogCampaignMetadata =
                    campaignCatalogMetadataResponse.getCampaignStatusList();
            Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> missedCampaignCatalogMap = catalogCampaignMetadata.stream()
                    .collect(Collectors.toMap(AdViewEventMetadataResponse.CatalogCampaignMetadata::getCatalogId, Function.identity()));
            adViewCampaignCatalogCache.putAll(missedCampaignCatalogMap);
            log.info("Set missed catalogIds in local cache {}", catalogCampaignMetadata);
        }
    }


}
