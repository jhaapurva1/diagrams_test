package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.meesho.ads.lib.exception.RetryableException;
import com.meesho.ads.lib.listener.kafka.BaseKafkaListener;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.CampaignCatalogUpdateEvent;
import com.meesho.cps.helper.CampaignCatalogUpdateEventHelper;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Slf4j
@Component
public class CampaignCatalogUpdateEventListener extends BaseKafkaListener<CampaignCatalogUpdateEvent> {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    CampaignCatalogUpdateEventHelper campaignCatalogUpdateEventHelper;

    @KafkaListener(id = ConsumerConstants.CampaignUpdateConsumer.ID, containerFactory =
            ConsumerConstants.CommonKafka.CONTAINER_FACTORY, topics = {
            ConsumerConstants.CampaignUpdateConsumer.TOPIC,
            ConsumerConstants.CampaignUpdateConsumer.RETRY_TOPIC}, autoStartup =
            ConsumerConstants.CampaignUpdateConsumer.AUTO_START, concurrency =
            ConsumerConstants.CampaignUpdateConsumer.CONCURRENCY, properties = {
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                    ConsumerConstants.CampaignUpdateConsumer.MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + ConsumerConstants.CampaignUpdateConsumer.BATCH_SIZE})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "CampaignCatalogUpdateEventListener")
    @Override
    public void listen(ConsumerRecord<String, String> consumerRecord) {
        super.listen(consumerRecord);
    }

    @Override
    public void consume(CampaignCatalogUpdateEvent campaignCatalogUpdateEvent) {
        try {
            for (CampaignCatalogUpdateEvent.CatalogData catalogData : campaignCatalogUpdateEvent.getCatalogs()) {
                campaignCatalogUpdateEventHelper.onCampaignCatalogUpdate(catalogData);
            }
        } catch (Exception e) {
            log.error("Exception in handling CampaignCatalogUpdateEvent", e);
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
        return ConsumerConstants.CampaignUpdateConsumer.DEAD_TOPIC;
    }

    @Override
    public String getRetryTopic() {
        return ConsumerConstants.CampaignUpdateConsumer.RETRY_TOPIC;
    }

    @Override
    public String getDelayedRetryTopic() {
        return ConsumerConstants.DelayedRetryConsumer.TOPIC;
    }

    @Override
    public TypeReference<CampaignCatalogUpdateEvent> getTypeReference() {
        return new TypeReference<CampaignCatalogUpdateEvent>() {
        };
    }

}
