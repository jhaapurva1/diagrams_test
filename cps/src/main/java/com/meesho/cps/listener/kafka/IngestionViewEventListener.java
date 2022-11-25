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
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Slf4j
//@Component
public class IngestionViewEventListener {

/*    IngestionConfluentKafkaViewEventsListener ingestionConfluentKafkaViewEventsListener;

    @KafkaListener(id = ConsumerConstants.IngestionViewEventsConsumer.ID, containerFactory =
            ConsumerConstants.IngestionServiceKafka.BATCH_CONTAINER_FACTORY, topics = {
            "#{'${kafka.ingestion.view.event.consumer.topics}'.split(',')}"}, autoStartup =
            ConsumerConstants.IngestionViewEventsConsumer.AUTO_START, concurrency =
            ConsumerConstants.IngestionViewEventsConsumer.CONCURRENCY, properties = {
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                    ConsumerConstants.IngestionViewEventsConsumer.MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + ConsumerConstants.IngestionViewEventsConsumer.BATCH_SIZE})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=IngestionViewEventListener")
    public void listen(@Payload List<ConsumerRecord<String, GenericRecord>> consumerRecords, Acknowledgment ack) {
        ingestionConfluentKafkaViewEventsListener.handleIngestionViewEvent(consumerRecords, ack);
    }*/

}
