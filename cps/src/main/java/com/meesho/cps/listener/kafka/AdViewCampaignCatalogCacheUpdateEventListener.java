package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.meesho.ads.lib.exception.DataValidationException;
import com.meesho.ads.lib.listener.kafka.BaseKafkaListener;
import com.meesho.ads.lib.listener.kafka.BaseManualAcknowledgeKafkaListener;
import com.meesho.ads.lib.listener.kafka.BatchKafkaListener;
import com.meesho.ads.lib.listener.kafka.BatchKafkaListenerForMq;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.AdInteractionStatus;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdViewCampaignCatalogCacheUpdateEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.helper.AdWidgetValidationHelper;
import com.meesho.cps.service.AdViewCampaignCatalogCacheUpdateService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.mq.client.annotation.MqListener;
import com.meesho.mq.client.service.MqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.meesho.cps.constants.TelegrafConstants.*;
import static com.meesho.cps.constants.TelegrafConstants.NAN;

@Slf4j
@Component
public class AdViewCampaignCatalogCacheUpdateEventListener extends BatchKafkaListenerForMq<AdViewCampaignCatalogCacheUpdateEvent> {

    @Autowired
    private MqService mqService;

    @Autowired
    private AdViewCampaignCatalogCacheUpdateService adViewCampaignCatalogCacheUpdateService;

    @Value(ConsumerConstants.AdViewCampaignCatalogCacheUpdateEventConsumer.DEAD_QUEUE_MQ_ID)
    private Long adViewCampaignCatalogCacheUpdateEventConsumerDeadQueueMqId;

    @MqListener(mqId = ConsumerConstants.AdViewCampaignCatalogCacheUpdateEventConsumer.MQ_ID, type = String.class)
    public void listen(List<ConsumerRecord<String, String>> consumerRecord) {
        super.listen(consumerRecord);
    }

    @Override
    public void consume(List<AdViewCampaignCatalogCacheUpdateEvent> events) throws DataValidationException {
        try {
            adViewCampaignCatalogCacheUpdateService.digestLoggerProxy(events);
        } catch (Exception e) {
            log.error("Error processing adViewCampaignCatalogCacheUpdateEvent {}", events);
            throw new RuntimeException(e);
        }
    }

    @Override
    public MqService getMqService() {
        return mqService;
    }

    @Override
    public Long getDeadQueueMqId() {
        return adViewCampaignCatalogCacheUpdateEventConsumerDeadQueueMqId;
    }

    @Override
    public TypeReference<AdViewCampaignCatalogCacheUpdateEvent> getTypeReference() {
        return new TypeReference<AdViewCampaignCatalogCacheUpdateEvent>() {};
    }

    @Override
    public Country getCountry() {
        return Country.IN;
    }
}