package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.data.entity.kafka.AdViewEvent;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.service.external.AdService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.instrumentation.metric.statsd.StatsdMetricManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

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

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "class=CatalogViewEventService")
    public void handle(AdViewEvent adViewEvent) {
        Long catalogId = adViewEvent.getProperties().getId();
        CampaignCatalogMetadataResponse.CatalogMetadata response = adService.getCampaignMetadataFromCache(catalogId);
        if (Objects.nonNull(response) && Objects.nonNull(response.getCampaignDetails())) {
            campaignCatalogMetricsRepository.incrementViewCount(response.getCampaignDetails().getCampaignId(),
                    catalogId);
            statsdMetricManager.incrementCounter(VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                    adViewEvent.getEventName(), adViewEvent.getProperties().getOrigin(), VALID, NAN));
        } else {
            log.error("No active campaign found for catalog {}", catalogId);
            statsdMetricManager.incrementCounter(VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                    adViewEvent.getEventName(), adViewEvent.getProperties().getOrigin(), INVALID,
                    AdInteractionInvalidReason.CAMPAIGN_INACTIVE));
        }
    }

}
