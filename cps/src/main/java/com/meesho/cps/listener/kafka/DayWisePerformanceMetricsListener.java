package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.meesho.ads.lib.exception.RetryableException;
import com.meesho.ads.lib.listener.kafka.BaseKafkaListener;
import com.meesho.ads.lib.listener.kafka.BaseKafkaListenerForMq;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.service.DayWisePerformanceMetricsService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.mq.client.annotation.MqListener;
import com.meesho.mq.client.service.MqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DayWisePerformanceMetricsListener extends BaseKafkaListenerForMq<List<CampaignCatalogDate>> {

    @Autowired
    private MqService mqService;

    @Autowired
    private DayWisePerformanceMetricsService dayWisePerformanceMetricsService;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Value(ConsumerConstants.DayWisePerformanceEventsConsumer.DEAD_QUEUE_MQ_ID)
    Long dayWisePerformanceEventsConsumerDeadQueueMqId;

    @Value(ConsumerConstants.DayWisePerformanceEventsConsumer.RETRY_MQ_ID)
    Long dayWisePerformanceEventsConsumerRetryMqId;

    @Value(ConsumerConstants.DelayedRetryConsumer.MQ_ID)
    Long delayedRetryConsumerMqId;

    @MqListener(mqId = ConsumerConstants.DayWisePerformanceEventsConsumer.MQ_ID, type = String.class)
    public void listen(ConsumerRecord<String, String> consumerRecord) {
        digestLoggerProxy(consumerRecord);
    }

    @MqListener(mqId = ConsumerConstants.DayWisePerformanceEventsConsumer.RETRY_MQ_ID, type = String.class)
    public void listenRetry(ConsumerRecord<String, String> consumerRecord) {
        digestLoggerProxy(consumerRecord);
    }

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=DayWisePerformanceEventListener")
    public void digestLoggerProxy(ConsumerRecord<String, String> consumerRecord){
        super.listen(consumerRecord);
    }

    @Override
    public void consume(List<CampaignCatalogDate> message) throws RuntimeException {
        try {
            dayWisePerformanceMetricsService.handleMessage(message);
        } catch (Exception e) {
            log.error("Error in handling Day wise performance event");
            throw new RetryableException(e.getMessage());
        }
    }

    @Override
    public MqService getMqService() {
        return mqService;
    }

    @Override
    public int getMaxImmediateRetries() {
        return 1;
    }

    @Override
    public int getMaxRetries() {
        return 3;
    }

    @Override
    public Long getDeadQueueMqId() {
        return dayWisePerformanceEventsConsumerDeadQueueMqId;
    }

    @Override
    public Long getRetryMqId() {
        return dayWisePerformanceEventsConsumerRetryMqId;
    }

    @Override
    public Long getDelayedRetryMqId() {
        return delayedRetryConsumerMqId;
    }


    @Override
    public TypeReference<List<CampaignCatalogDate>> getTypeReference() {
        return new TypeReference<List<CampaignCatalogDate>>() {
        };
    }

    @Override
    public void onRetriesExhausted(String key, String message, List<CampaignCatalogDate> campaignCatalogDateList) {
        super.onRetriesExhausted(key,message,campaignCatalogDateList);
        //Adding back to updatedCampaignCatalog so that it will be picked in next scheduler run
        updatedCampaignCatalogCacheDao.add(campaignCatalogDateList);
    }
}
