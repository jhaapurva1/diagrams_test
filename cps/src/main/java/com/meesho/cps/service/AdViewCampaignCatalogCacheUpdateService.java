package com.meesho.cps.service;

import com.meesho.cps.data.entity.kafka.AdViewCampaignCatalogCacheUpdateEvent;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AdViewCampaignCatalogCacheUpdateService {

    @Autowired
    private RedisPublisherService redisPublisherService;

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "className=AdViewCampaignCatalogCacheUpdateEventListener")
    public void digestLoggerProxy(List<AdViewCampaignCatalogCacheUpdateEvent> events){
        handle(events);
        }
    public void handle(List<AdViewCampaignCatalogCacheUpdateEvent> events) {
        log.debug("AdViewCampaignCatalogCacheUpdateEvents received : {}", events);
        for(AdViewCampaignCatalogCacheUpdateEvent event : events) {
            // set catalogs in local cache
            redisPublisherService.publishAdViewCampaignCatalogRefreshEvent(event.getCatalogIds());
        }
    }

}
