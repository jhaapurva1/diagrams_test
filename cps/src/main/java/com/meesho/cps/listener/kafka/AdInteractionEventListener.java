package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.meesho.ads.lib.exception.DataValidationException;
import com.meesho.ads.lib.listener.kafka.BaseKafkaListener;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.helper.ValidationHelper;
import com.meesho.cps.service.CatalogInteractionEventService;
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
 * 03/08/21
 */
@Slf4j
@Component
public class AdInteractionEventListener extends BaseKafkaListener<AdInteractionEvent> {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private CatalogInteractionEventService catalogInteractionEventService;

    @KafkaListener(id = ConsumerConstants.InteractionEventsConsumer.ID, containerFactory =
            ConsumerConstants.CommonKafka.CONTAINER_FACTORY, topics =
            ConsumerConstants.InteractionEventsConsumer.TOPIC, autoStartup =
            ConsumerConstants.InteractionEventsConsumer.AUTO_START, concurrency =
            ConsumerConstants.InteractionEventsConsumer.CONCURRENCY, properties = {
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                    ConsumerConstants.InteractionEventsConsumer.MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + ConsumerConstants.InteractionEventsConsumer.BATCH_SIZE})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "AdInteractionEventListener")
    public void listen(ConsumerRecord<String, String> consumerRecord) {
        super.listen(consumerRecord);
    }

    @Override
    public void consume(AdInteractionEvent adInteractionEvent) throws DataValidationException {
        if (!ValidationHelper.isValidAdInteractionEvent(adInteractionEvent)) {
            log.error("Invalid event {}", adInteractionEvent);
            throw new DataValidationException("Invalid event");
        }
        log.info("Processing interaction event for userId {}, catalogId {}, " + "appVersionCode : {}",
                adInteractionEvent.getUserId(), adInteractionEvent.getProperties().getId(),
                adInteractionEvent.getProperties().getAppVersionCode());
        catalogInteractionEventService.handle(adInteractionEvent);
    }

    @Override
    public KafkaTemplate<String, String> getKafkaTemplate() {
        return kafkaTemplate;
    }

    @Override
    public int getMaxImmediateRetries() {
        return 0;
    }

    @Override
    public int getMaxRetries() {
        return 0;
    }

    @Override
    public String getDeadTopic() {
        return ConsumerConstants.InteractionEventsConsumer.DEAD_QUEUE_TOPIC;
    }

    @Override
    public String getRetryTopic() {
        return null;
    }

    @Override
    public String getDelayedRetryTopic() {
        return ConsumerConstants.DelayedRetryConsumer.TOPIC;
    }

    @Override
    public TypeReference<AdInteractionEvent> getTypeReference() {
        return new TypeReference<AdInteractionEvent>() {
        };
    }

}
