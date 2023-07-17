package com.meesho.cps.listener.kafka;

import com.meesho.ads.lib.listener.kafka.BasePrestoSchedulerEventListenerForMq;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.mq.client.annotation.MqListener;
import com.meesho.mq.client.service.MqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PrestoSchedulerEventListener extends BasePrestoSchedulerEventListenerForMq {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private MqService mqService;

    @Value(ConsumerConstants.PrestoConsumer.DEAD_QUEUE_MQ_ID)
    private Long prestoConsumerDeadQueueMqId;
    @Value(ConsumerConstants.PrestoConsumer.RETRY_MQ_ID)
    private Long prestoConsumerRetryMqId;
    @Value(ConsumerConstants.PrestoConsumer.MAX_IMMEDIATE_RETRIES)
    int maxImmediateRetries;

    @MqListener(mqId = ConsumerConstants.PrestoConsumer.MQ_ID, type = String.class)
    public void listen(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        digestLoggerProxy(consumerRecord, acknowledgment);
    }

    @MqListener(mqId = ConsumerConstants.PrestoConsumer.RETRY_MQ_ID, type = String.class)
    public void listenRetry(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        digestLoggerProxy(consumerRecord, acknowledgment);
    }

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=PrestoSchedulerEventListener")
    public void digestLoggerProxy(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment){
        super.listen(consumerRecord, acknowledgment, applicationProperties.getSchedulerTypeCountryAndPropertyMap());
    }

    @Override
    public int getMaxImmediateRetries() {
        return maxImmediateRetries;
    }

    @Override
    public Long getDeadQueueMqId() {
        return prestoConsumerDeadQueueMqId;
    }

    @Override
    public Long getRetryMqId() {
        return prestoConsumerRetryMqId;
    }

    @Override
    public MqService getMqService() {
        return mqService;
    }

}