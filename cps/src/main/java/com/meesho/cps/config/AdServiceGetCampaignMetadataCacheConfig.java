package com.meesho.cps.config;

import com.github.benmanes.caffeine.cache.Caffeine;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@Configuration
@EnableCaching
public class AdServiceGetCampaignMetadataCacheConfig {

    @Value("${cache_maximum_elements}")
    private Integer cacheMaximumElements;

    @Value("${cache_eviction_in_seconds}")
    private Integer cacheEvictionInSeconds;

    @Bean("caffineAdServiceGetCampaignCatalog")
    public Caffeine caffeineConfig() {
        return Caffeine.newBuilder()
                .maximumSize(cacheMaximumElements)
                .expireAfterWrite(cacheEvictionInSeconds, TimeUnit.SECONDS);
    }

    @Bean("adServiceGetCampaignCatalogMetadataCacheManager")
    public CacheManager cacheManager(@Qualifier("caffineAdServiceGetCampaignCatalog") Caffeine caffeine) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeine);
        return cacheManager;
    }

}
