package com.meesho.cps.listener.kafka;

import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author shubham.aggarwal
 * 03/08/21
 * <p>
 * This consumer gets the events from Ingestion service and
 * pushes them to interaction consumers with partition key as campaignId.
 * This is done to avoid race conditions in interaction event consumers
 */
@Slf4j
@Component
public class UnPartitionedIngestionInteractionEventListener {

    @Autowired
    private UnPartitionedIngestionConfluentKafkaInteractionEventListener unPartitionedIngestionConfluentKafkaInteractionEventListener;

    @KafkaListener(id = ConsumerConstants.IngestionInteractionEventsConsumer.ID, containerFactory =
            ConsumerConstants.IngestionServiceKafka.CONTAINER_FACTORY, topics = {
            "#{'${kafka.ingestion.interaction.event.consumer.topics}'.split(',')}"}, autoStartup =
            ConsumerConstants.IngestionInteractionEventsConsumer.AUTO_START, concurrency =
            ConsumerConstants.IngestionInteractionEventsConsumer.CONCURRENCY, properties = {
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                    ConsumerConstants.IngestionInteractionEventsConsumer.MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" +
                    ConsumerConstants.IngestionInteractionEventsConsumer.BATCH_SIZE})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=IngestionInteractionEventListener")
    public void listen(ConsumerRecord<String, GenericRecord> consumerRecord) {
        unPartitionedIngestionConfluentKafkaInteractionEventListener.handleIngestionInteractionEvent(consumerRecord);
    }

}
