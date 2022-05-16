package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.meesho.ads.lib.exception.RetryableException;
import com.meesho.ads.lib.listener.kafka.BaseKafkaListener;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.service.DayWisePerformanceMetricsService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
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
public class DayWisePerformanceMetricsListener extends BaseKafkaListener<List<CampaignCatalogDate>> {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DayWisePerformanceMetricsService dayWisePerformanceMetricsService;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Value(ConsumerConstants.DayWisePerformanceEventsConsumer.DEAD_QUEUE_TOPIC)
    String dayWisePerformanceEventsConsumerDeadQueueTopic;

    @Value(ConsumerConstants.DayWisePerformanceEventsConsumer.RETRY_TOPIC)
    String dayWisePerformanceEventsConsumerRetryTopic;

    @Value(ConsumerConstants.DelayedRetryConsumer.TOPIC)
    String delayedRetryConsumerTopic;

    @KafkaListener(id = ConsumerConstants.DayWisePerformanceEventsConsumer.ID, containerFactory =
            ConsumerConstants.CommonKafka.BATCH_CONTAINER_FACTORY, topics =
            {ConsumerConstants.DayWisePerformanceEventsConsumer.TOPIC,
                    ConsumerConstants.DayWisePerformanceEventsConsumer.RETRY_TOPIC}, autoStartup =
            ConsumerConstants.DayWisePerformanceEventsConsumer.AUTO_START, concurrency =
            ConsumerConstants.DayWisePerformanceEventsConsumer.CONCURRENCY, properties = {
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                    ConsumerConstants.DayWisePerformanceEventsConsumer.MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + ConsumerConstants.DayWisePerformanceEventsConsumer.BATCH_SIZE})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=DayWisePerformanceEventListener")
    public void listen(ConsumerRecord<String, String> consumerRecord) {
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
    public KafkaTemplate<String, String> getKafkaTemplate() {
        return kafkaTemplate;
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
    public String getDeadTopic() {
        return dayWisePerformanceEventsConsumerDeadQueueTopic;
    }

    @Override
    public String getRetryTopic() {
        return dayWisePerformanceEventsConsumerRetryTopic;
    }

    @Override
    public String getDelayedRetryTopic() {return delayedRetryConsumerTopic; }

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
