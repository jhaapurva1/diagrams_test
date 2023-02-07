package com.meesho.cps.listener.kafka;

import com.meesho.ads.lib.listener.kafka.BasePrestoSchedulerEventListener;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PrestoSchedulerEventListener extends BasePrestoSchedulerEventListener {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private ApplicationProperties applicationProperties;
    @Value(ConsumerConstants.PrestoConsumer.DEAD_QUEUE_TOPIC)
    String prestoConsumerDeadQueueTopic;
    @Value(ConsumerConstants.PrestoConsumer.RETRY_TOPIC)
    String prestoConsumerRetryTopic;
    @Value(ConsumerConstants.PrestoConsumer.MAX_IMMEDIATE_RETRIES)
    int maxImmediateRetries;
    @KafkaListener(id = ConsumerConstants.PrestoConsumer.ID,
            containerFactory = ConsumerConstants.PrestoKafka.CONTAINER_FACTORY,
            topics = {ConsumerConstants.PrestoConsumer.TOPIC, ConsumerConstants.PrestoConsumer.RETRY_TOPIC},
            autoStartup = ConsumerConstants.PrestoConsumer.AUTO_START,
            concurrency = ConsumerConstants.PrestoConsumer.CONCURRENCY,
            properties = {ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "="
                                + ConsumerConstants.PrestoConsumer.MAX_POLL_INTERVAL_MS,
                          ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "="
                              + ConsumerConstants.PrestoConsumer.BATCH_SIZE})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=PrestoSchedulerEventListener")
    public void listen(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        super.listen(consumerRecord, acknowledgment, applicationProperties.getSchedulerTypeCountryAndPropertyMap());
    }

    @Override
    public KafkaTemplate<String, String> getKafkaTemplate() {
        return kafkaTemplate;
    }

    @Override
    public int getMaxImmediateRetries() {
        return maxImmediateRetries;
    }

    @Override
    public String getDeadTopic() {
        return prestoConsumerDeadQueueTopic;
    }

    @Override
    public String getRetryTopic() {
        return prestoConsumerRetryTopic;
    }
}