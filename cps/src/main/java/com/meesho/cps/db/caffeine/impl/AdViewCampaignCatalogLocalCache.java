package com.meesho.cps.db.caffeine.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.meesho.ad.client.response.AdViewEventMetadataResponse;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.constants.MessageIntent;
import com.meesho.cps.db.caffeine.BaseLocalCache;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.service.external.AdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AdViewCampaignCatalogLocalCache extends BaseLocalCache<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> {

    @Autowired
    public AdViewCampaignCatalogLocalCache(
            @Qualifier("adViewCampaignCatalogCacheManager") CacheManager adViewCampaignCatalogCacheManager) {
        super((Cache<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata>)
                adViewCampaignCatalogCacheManager.getCache("adViewCampaignCatalogCache").getNativeCache());
    }

    @Autowired
    private AdService adService;

    @Override
    public MessageIntent getMessageIntentType() {
        return MessageIntent.UPDATE_AD_VIEW_CAMPAIGN_CATALOG;
    }

    @Override
    public List<Long> transformEvent(String catalogIdsString) {
        if (StringUtils.isEmpty(catalogIdsString)) {
            return Collections.emptyList();
        }
        return Arrays.stream(catalogIdsString.split(ConsumerConstants.GenericRedisNotificationsConsumer.DELIMITER_USED))
                .map(String::trim)
                .filter(StringUtils::isNotEmpty)
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> fetchRecordsFromDataSource(List<Long> catalogIds) {
        try {
            AdViewEventMetadataResponse adViewEventMetadataResponse = adService.getAdViewEventCatalogCampaignStatus(catalogIds);
            return adViewEventMetadataResponse.getCampaignStatusList().stream()
                    .collect(Collectors.toMap(AdViewEventMetadataResponse.CatalogCampaignMetadata::getCatalogId, Function.identity()));
        } catch (ExternalRequestFailedException e) {
            log.error("getAdViewEventCatalogCampaignStatus call failed for catalogs : {}", catalogIds, e);
            return new HashMap<>();
        }
    }
}
