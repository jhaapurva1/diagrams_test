package com.meesho.cps.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.response.AdViewEventMetadataResponse;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.commons.enums.Country;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import com.meesho.cps.helper.AdWidgetValidationHelper;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.instrumentation.metric.statsd.StatsdMetricManager;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;

import static com.meesho.cps.constants.TelegrafConstants.*;

@Slf4j
@Service
public class WidgetViewEventService {

    @Autowired
    CatalogViewEventService catalogViewEventService;

    @Autowired
    KafkaService kafkaService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CampaignPerformanceHelper campaignPerformanceHelper;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    StatsdMetricManager statsdMetricManager;

    private ThreadLocal<Long> startTime = ThreadLocal.withInitial(System::currentTimeMillis);
    private ThreadLocal<Map<String, CampaignCatalogViewCount>> campaignCatalogViewCountMap = ThreadLocal.withInitial(HashMap::new);
    @Value(ConsumerConstants.IngestionViewEventsConsumer.DEAD_QUEUE_TOPIC)
    private String ingestionViewEventsDeadQueueTopic;

    @Value(ConsumerConstants.IngestionViewEventsConsumer.DEAD_QUEUE_MQ_ID)
    private Long ingestionViewEventsDeadQueueMqId;

    public void handle(AdWidgetViewEvent adWidgetViewEvent) {
        log.info("Started porcessing of view event: {}", adWidgetViewEvent);
        String countryCode = "IN";
        org.slf4j.MDC.put(Constants.COUNTRY_CODE, Country.getValueDefaultCountryFromEnv(countryCode).getCountryCode());
        if (Boolean.FALSE.equals(AdWidgetValidationHelper.isValidWidgetRealEstate(adWidgetViewEvent.getProperties().getSourceScreens().get(0)))) {
            log.error("Not a valid event userId {} eventId {} for the real estate {}",
                    adWidgetViewEvent.getUserId(), adWidgetViewEvent.getEventId(), adWidgetViewEvent.getProperties().getSourceScreens().get(0));
            statsdMetricManager.incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(WIDGET_VIEW_EVENT_TAGS,
                    adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getSourceScreens(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), INVALID,
                    AdInteractionInvalidReason.NOT_AD_WIDGET));
            return;
        }

        Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> catalogMetadataMap;
        try {
            catalogMetadataMap =
                catalogViewEventService.getCampaignCatalogMetadataFromCatalogIds(adWidgetViewEvent.getProperties().getCatalogIds().stream().distinct().collect(
                    Collectors.toList()));
        } catch (Exception e) {
            log.error("Exception while processing ingestion view events {}", adWidgetViewEvent, e);
            try {
                kafkaService.sendMessageToMq(
                        ingestionViewEventsDeadQueueMqId,
                        String.valueOf(adWidgetViewEvent.getProperties().getCatalogIds()),
                        objectMapper.writeValueAsString(adWidgetViewEvent)
                );
            } catch (Exception e1) {
                log.error("Failed to push to dead queue event {}", adWidgetViewEvent);
            }
            return;
        }

        if (CollectionUtils.isEmpty(catalogMetadataMap)) {
            statsdMetricManager.incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(WIDGET_VIEW_EVENT_TAGS,
                    adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getSourceScreens(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), INVALID,
                    AdInteractionInvalidReason.CAMPAIGN_INACTIVE));
            return;
        }

        updateCampaignCatalogViewCounts(adWidgetViewEvent, catalogMetadataMap);
        long currentTime = System.currentTimeMillis();

        if (currentTime >= startTime.get() + applicationProperties.getBatchInterval()) {
            List<CampaignCatalogViewCount> campaignCatalogViewCountList = new ArrayList<>(campaignCatalogViewCountMap.get().values());
            log.info("writing to mongo: {}", campaignCatalogViewCountList);
            catalogViewEventService.writeToMongo(campaignCatalogViewCountList);
            campaignCatalogViewCountMap.set(new HashMap<>());
            startTime.set(System.currentTimeMillis());
        }
    }

    private void updateCampaignCatalogViewCounts(AdWidgetViewEvent adWidgetViewEvent,
                                                 Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> catalogMetadataMap) {

        Map<String, CampaignCatalogViewCount> campaignCatalogViewCountMapTillNow = campaignCatalogViewCountMap.get();
        LocalDate eventDate = campaignPerformanceHelper.getLocalDateForDailyCampaignFromLocalDateTime(
                DateTimeUtils.getCurrentLocalDateTimeInIST());

        for (Long catalogId : adWidgetViewEvent.getProperties().getCatalogIds()) {

            AdViewEventMetadataResponse.CatalogCampaignMetadata catalogMetadata = catalogMetadataMap.get(catalogId);
            if(Objects.isNull(catalogMetadata) || Boolean.FALSE.equals(catalogMetadata.getIsCampaignActive())){
                log.warn("No active ad on catalogIds {} userId {} eventId {}",
                        adWidgetViewEvent.getProperties().getCatalogIds(), adWidgetViewEvent.getUserId(), adWidgetViewEvent.getEventId());
                statsdMetricManager.incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(WIDGET_VIEW_EVENT_TAGS,
                        adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getSourceScreens(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), INVALID,
                        AdInteractionInvalidReason.CAMPAIGN_INACTIVE));
                continue;
            }
            Long campaignId = catalogMetadata.getCampaignId();

            log.info("Processing view event for eventId {} catalogIds {} campaignId {} userId {} appVersionCode {}",
                    adWidgetViewEvent.getEventId(), adWidgetViewEvent.getProperties().getCatalogIds(), campaignId, adWidgetViewEvent.getUserId(),
                    adWidgetViewEvent.getProperties().getAppVersionCode());
            statsdMetricManager.incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(WIDGET_VIEW_EVENT_TAGS,
                    adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getSourceScreens(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));
            String campaignCatalogViewCountKey = getCampaignCatalogKey(campaignId, catalogId, eventDate);

            CampaignCatalogViewCount campaignCatalogViewCount =
                    campaignCatalogViewCountMapTillNow.getOrDefault(campaignCatalogViewCountKey, CampaignCatalogViewCount.builder()
                            .campaignId(campaignId).catalogId(catalogId).supplierId(catalogMetadata.getSupplierId()).date(eventDate).count(0).build());
            campaignCatalogViewCount.setCount(campaignCatalogViewCount.getCount() + 1);
            campaignCatalogViewCountMapTillNow.put(campaignCatalogViewCountKey, campaignCatalogViewCount);
        }
        campaignCatalogViewCountMap.set(campaignCatalogViewCountMapTillNow);
    }

    private String getCampaignCatalogKey(Long campaignId, Long catalogId, LocalDate date) {
        return String.valueOf(campaignId).concat("_").concat(String.valueOf(catalogId)).concat("_").concat(date.toString());
    }
}
