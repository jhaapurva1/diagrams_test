package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.response.AdViewEventMetadataResponse;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.commons.enums.Country;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdViewEvent;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.helper.ValidationHelper;
import com.meesho.cps.service.CatalogViewEventService;
import com.meesho.cps.service.KafkaService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.instrumentation.metric.statsd.StatsdMetricManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.event.ConsumerStoppedEvent;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.*;

import static com.meesho.cps.constants.TelegrafConstants.*;
import static com.meesho.cps.constants.TelegrafConstants.NAN;

@Slf4j
@Component
public class IngestionConfluentKafkaViewEventsListener implements ApplicationListener<ConsumerStoppedEvent> {

    @Autowired
    CatalogViewEventService catalogViewEventService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private StatsdMetricManager statsdMetricManager;
    @Autowired
    private CampaignPerformanceHelper campaignPerformanceHelper;
    @Autowired
    ApplicationProperties applicationProperties;
    private ThreadLocal<Long> startTime = ThreadLocal.withInitial(System::currentTimeMillis);
    private ThreadLocal<Map<String, CampaignCatalogViewCount>> campaignCatalogViewCountMap = ThreadLocal.withInitial(HashMap::new);

    @Value(ConsumerConstants.IngestionViewEventsConsumer.DEAD_QUEUE_TOPIC)
    private String ingestionViewEventsDeadQueueTopic;

    @KafkaListener(
            id = ConsumerConstants.IngestionViewEventsConsumer.CONFLUENT_CONSUMER_ID,
            containerFactory = ConsumerConstants.IngestionServiceConfluentKafka.BATCH_CONTAINER_FACTORY,
            topics = {
                    "#{'${kafka.ingestion.view.event.consumer.topics}'.split(',')}"
            },
            autoStartup = ConsumerConstants.IngestionViewEventsConsumer.AUTO_START,
            concurrency = ConsumerConstants.IngestionViewEventsConsumer.CONCURRENCY,
            properties = {
                    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" + ConsumerConstants.IngestionViewEventsConsumer.MAX_POLL_INTERVAL_MS,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + ConsumerConstants.IngestionViewEventsConsumer.BATCH_SIZE
            }
    )
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "className=ingestionConfluentViewEventsConsumer")
    public void listen(@Payload List<ConsumerRecord<String, GenericRecord>> consumerRecords, Acknowledgment ack) {
        handleIngestionViewEvent(consumerRecords, ack);
    }

    @Override
    public void onApplicationEvent(ConsumerStoppedEvent event) {
        // remove the threadLocal instances
        startTime.remove();
        campaignCatalogViewCountMap.remove();
    }

    protected void handleIngestionViewEvent(@Payload List<ConsumerRecord<String, GenericRecord>> consumerRecords, Acknowledgment ack) {
        List<AdViewEvent> adViewEvents = new ArrayList<>();
        MDC.put(com.meesho.ads.lib.constants.Constants.GUID, UUID.randomUUID().toString());
        String countryCode = "IN";
        org.slf4j.MDC.put(Constants.COUNTRY_CODE, Country.getValueDefaultCountryFromEnv(countryCode).getCountryCode());

        for (ConsumerRecord<String, GenericRecord> consumerRecord : consumerRecords) {

            String value = consumerRecord.value().toString();
            log.info("Ingestion view event received : {}", value);

            AdViewEvent adViewEvent = null;
            try {
                adViewEvent = objectMapper.readValue(value, AdViewEvent.class);
            } catch (JsonProcessingException e) {
                log.error("JsonProcessingException event : {}", value,e);
            }

            if (Objects.isNull(adViewEvent) || !ValidationHelper.isValidAdViewEvent(adViewEvent)) {
                log.error("Invalid event {}", adViewEvent);
                statsdMetricManager.incrementCounter(VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS, NAN, NAN, NAN, INVALID,
                        NAN));
                kafkaService.sendMessage(ingestionViewEventsDeadQueueTopic,
                        consumerRecord.key(), consumerRecord.value().toString());
                continue;
            }

            adViewEvents.add(adViewEvent);
        }

        Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> catalogMetadataMap;
        try {
            catalogMetadataMap = catalogViewEventService.getCampaignCatalogMetadataFromAdViewEvents(adViewEvents);
        } catch (Exception e) {
            log.error("Exception while processing ingestion view events {}", adViewEvents, e);
            for (AdViewEvent eachAdViewEvent : adViewEvents) {
                try {
                    kafkaService.sendMessage(
                            ingestionViewEventsDeadQueueTopic,
                            String.valueOf(eachAdViewEvent.getProperties().getId()),
                            objectMapper.writeValueAsString(eachAdViewEvent)
                    );
                } catch (JsonProcessingException e1) {
                    // TODO silent failure needs to be handled
                    log.error("Failed to push to dead queue event {}", eachAdViewEvent);
                }
            }
            return;
        }

        if (CollectionUtils.isEmpty(catalogMetadataMap)) {
            // No active campaign found for catalogs
            return;
        }

        updateCampaignCatalogViewCounts(adViewEvents, catalogMetadataMap);
        long currentTime = System.currentTimeMillis();

        if (currentTime >= startTime.get() + applicationProperties.getBatchInterval()) {
            ack.acknowledge();
            List<CampaignCatalogViewCount> campaignCatalogViewCountList = new ArrayList<>(campaignCatalogViewCountMap.get().values());
            catalogViewEventService.writeToHbase(campaignCatalogViewCountList);
            campaignCatalogViewCountMap.set(new HashMap<>());
            startTime.set(System.currentTimeMillis());
        }
        MDC.clear();
    }

    private String getCampaignCatalogKey(Long campaignId, Long catalogId, LocalDate date) {
        return String.valueOf(campaignId).concat("_").concat(String.valueOf(catalogId)).concat("_").concat(date.toString());
    }

    private void updateCampaignCatalogViewCounts(List<AdViewEvent> adViewEvents,
                                                 Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> catalogMetadataMap) {

        Map<String, CampaignCatalogViewCount> campaignCatalogViewCountMapTillNow = campaignCatalogViewCountMap.get();
        LocalDate eventDate = campaignPerformanceHelper.getLocalDateForDailyCampaignFromLocalDateTime(
                DateTimeUtils.getCurrentLocalDateTimeInIST());

        for (AdViewEvent adViewEvent : adViewEvents) {

            Long catalogId = adViewEvent.getProperties().getId();
            AdViewEventMetadataResponse.CatalogCampaignMetadata catalogMetadata = catalogMetadataMap.get(catalogId);
            if(Objects.isNull(catalogMetadata) || !catalogMetadata.getIsCampaignActive()){
                log.error("No active ad on catalogId {} userId {} eventId {}",adViewEvent.getProperties().getId(),adViewEvent.getUserId(),adViewEvent.getEventId());
                statsdMetricManager.incrementCounter(VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                        adViewEvent.getEventName(), adViewEvent.getProperties().getScreen(), adViewEvent.getProperties().getOrigin(), INVALID,
                        AdInteractionInvalidReason.CAMPAIGN_INACTIVE));
                continue;
            }
            Long campaignId = catalogMetadata.getCampaignId();

            log.info("Processing view event for eventId {} catalogId {} campaignId {} userId {} appVersionCode {}",
                    adViewEvent.getEventId(), adViewEvent.getProperties().getId(), campaignId, adViewEvent.getUserId(),
                    adViewEvent.getProperties().getAppVersionCode());
            statsdMetricManager.incrementCounter(VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                    adViewEvent.getEventName(), adViewEvent.getProperties().getScreen(), adViewEvent.getProperties().getOrigin(), VALID, NAN));


            String campaignCatalogViewCountKey = getCampaignCatalogKey(campaignId, catalogId, eventDate);

            CampaignCatalogViewCount campaignCatalogViewCount =
                    campaignCatalogViewCountMapTillNow.getOrDefault(campaignCatalogViewCountKey, CampaignCatalogViewCount.builder()
                            .campaignId(campaignId).catalogId(catalogId).date(eventDate).count(0).build());
            campaignCatalogViewCount.setCount(campaignCatalogViewCount.getCount() + 1);
            campaignCatalogViewCountMapTillNow.put(campaignCatalogViewCountKey, campaignCatalogViewCount);
        }
        campaignCatalogViewCountMap.set(campaignCatalogViewCountMapTillNow);
    }
}
