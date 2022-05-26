package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdViewEvent;
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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.meesho.cps.constants.TelegrafConstants.*;
import static com.meesho.cps.constants.TelegrafConstants.NAN;

@Slf4j
@Component
public class IngestionConfluentKafkaViewEventsListener {

    @Autowired
    CatalogViewEventService catalogViewEventService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KafkaService kafkaService;
    @Autowired
    private StatsdMetricManager statsdMetricManager;

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
    public void listen(@Payload List<ConsumerRecord<String, GenericRecord>> consumerRecords) {
        handleIngestionViewEvent(consumerRecords);
    }

    protected void handleIngestionViewEvent(@Payload List<ConsumerRecord<String, GenericRecord>> consumerRecords) {
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

        try {
            catalogViewEventService.handle(adViewEvents);
        } catch (Exception e) {
            log.error("Exception while processing ingestion view events {}", adViewEvents, e);
            for (AdViewEvent adViewEvent : adViewEvents) {
                try {
                    kafkaService.sendMessage(
                            ingestionViewEventsDeadQueueTopic,
                            String.valueOf(adViewEvent.getProperties().getId()),
                            objectMapper.writeValueAsString(adViewEvent)
                    );
                } catch (JsonProcessingException e1) {
                    // TODO silent failure needs to be handled
                    log.error("Failed to push to dead queue event {}", adViewEvent);
                }
            }
        }
        MDC.clear();
    }
}