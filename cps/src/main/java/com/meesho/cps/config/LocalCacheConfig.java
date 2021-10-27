package com.meesho.cps.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;


@Configuration
@EnableCaching
public class LocalCacheConfig {

    @Value("${cache.ads.campaign.catalog.elements}")
    private Integer adsCampaignCatalogElements;

    @Value("${cache.ads.campaign.catalog.seconds}")
    private Integer adsCampaignCatalogSeconds;

    @Bean("adServiceCampaignCatalogCache")
    public Cache<Long, CampaignCatalogMetadataResponse.CatalogMetadata> adsCampaignCatalogCache() {
        return Caffeine.newBuilder()
                .maximumSize(adsCampaignCatalogElements)
                .expireAfterWrite(adsCampaignCatalogSeconds, TimeUnit.SECONDS)
                .build();
    }

}
