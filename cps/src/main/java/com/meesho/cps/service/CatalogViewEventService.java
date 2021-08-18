package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.cps.data.entity.kafka.AdViewEvent;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.service.external.AdService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

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

    @Transactional
    public void handle(AdViewEvent adViewEvent) {
        Long catalogId = adViewEvent.getProperties().getId();
        CampaignCatalogMetadataResponse.CatalogMetadata response = adService.getCampaignMetadataFromCache(catalogId);
        if (Objects.nonNull(response) && Objects.nonNull(response.getCampaignDetails())) {
            campaignCatalogMetricsRepository.incrementViewCount(response.getCampaignDetails().getCampaignId(),
                    catalogId);
        } else {
            log.error("No active campaign found for catalog {}", catalogId);
        }
    }

}
