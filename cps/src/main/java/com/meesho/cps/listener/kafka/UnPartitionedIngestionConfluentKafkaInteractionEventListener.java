package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.service.KafkaService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author shubham.aggarwal
 * 03/08/21
 * <p>
 * This consumer gets the events from Ingestion service (confluent kafka cluster) and
 * pushes them to interaction consumers with partition key as campaignId.
 * This is done to avoid race conditions in interaction event consumers
 */
@Slf4j
@Component
public class UnPartitionedIngestionConfluentKafkaInteractionEventListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaService kafkaService;

    @Value(ConsumerConstants.IngestionInteractionEventsConsumer.DEAD_QUEUE_TOPIC)
    String ingestionInteractionEventsDeadQueueTopic;

    @Value(ConsumerConstants.InteractionEventsConsumer.TOPIC)
    String interactionEventsConsumerTopic;

    @Value(com.meesho.cps.constants.Constants.Kafka.INTERACTION_EVENT_MQ_ID)
    Long interactionEventMqID;

    @KafkaListener(id = ConsumerConstants.IngestionInteractionEventsConsumer.CONFLUENT_CONSUMER_ID, containerFactory =
            ConsumerConstants.IngestionServiceConfluentKafka.CONTAINER_FACTORY, topics = {
            "#{'${kafka.ingestion.interaction.event.consumer.topics}'.split(',')}"}, autoStartup =
            ConsumerConstants.IngestionInteractionEventsConsumer.AUTO_START, concurrency =
            ConsumerConstants.IngestionInteractionEventsConsumer.CONCURRENCY, properties = {
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                    ConsumerConstants.IngestionInteractionEventsConsumer.MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" +
                    ConsumerConstants.IngestionInteractionEventsConsumer.BATCH_SIZE})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=IngestionConfluentInteractionEventListener")
    public void listen(ConsumerRecord<String, GenericRecord> consumerRecord) {
        handleIngestionInteractionEvent(consumerRecord);
    }

    public void handleIngestionInteractionEvent(ConsumerRecord<String, GenericRecord> consumerRecord){
        try {
            MDC.put(Constants.GUID, UUID.randomUUID().toString());
            String value = consumerRecord.value().toString();
            log.info("Ingestion interaction event received : {}", value);

            String countryCode = "IN";
            MDC.put(Constants.COUNTRY_CODE, Country.getValueDefaultCountryFromEnv(countryCode).getCountryCode());

            AdInteractionEvent adInteractionEvent = objectMapper.readValue(value, AdInteractionEvent.class);

            kafkaService.sendMessageToMq(interactionEventMqID,
                    adInteractionEvent.getProperties().getId().toString(),
                    objectMapper.writeValueAsString(adInteractionEvent));

        } catch (Exception e) {
            log.error("Exception while processing ingestion interaction event {}", consumerRecord, e);
            kafkaService.sendMessage(ingestionInteractionEventsDeadQueueTopic,
                    consumerRecord.key(), consumerRecord.value().toString());
        } finally {
            MDC.clear();
        }
    }

}
