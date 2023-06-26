package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.meesho.ads.lib.exception.DataValidationException;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.listener.kafka.BaseKafkaListener;
import com.meesho.cps.constants.AdInteractionStatus;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.helper.ValidationHelper;
import com.meesho.cps.service.CatalogInteractionEventService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.mq.client.annotation.MqListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static com.meesho.cps.constants.TelegrafConstants.*;

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
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Autowired
    private CatalogInteractionEventService catalogInteractionEventService;

    @Value(ConsumerConstants.InteractionEventsConsumer.DEAD_QUEUE_TOPIC)
    String interactionEventsConsumerDeadQueueTopic;

    @Value(ConsumerConstants.DelayedRetryConsumer.TOPIC)
    String delayedRetryConsumerTopic;

    @MqListener(mqId = ConsumerConstants.InteractionEventsConsumer.MQ_ID, type = String.class)
    public void listen(ConsumerRecord<String, String> consumerRecord) {
        digestLoggerProxy(consumerRecord);
    }

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=AdInteractionEventListener")
    public void digestLoggerProxy(ConsumerRecord<String, String> consumerRecord){
        super.listen(consumerRecord);
    }

    @Override
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=AdInteractionEventListener")
    public void consume(AdInteractionEvent adInteractionEvent) throws DataValidationException {
        if (!ValidationHelper.isValidAdInteractionEvent(adInteractionEvent)) {
            log.warn("Invalid event {}", adInteractionEvent);
            telegrafMetricsHelper.increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS, adInteractionEvent.getEventName(), NAN,NAN,
                    AdInteractionStatus.INVALID.name(), NAN);
            throw new DataValidationException("Invalid event");
        }
        log.info("Processing interaction event for userId {}, catalogId {}, " + "appVersionCode : {}",
                adInteractionEvent.getUserId(), adInteractionEvent.getProperties().getId(),
                adInteractionEvent.getProperties().getAppVersionCode());
        try {
            catalogInteractionEventService.handle(adInteractionEvent);
        } catch (Exception e){
            log.error("Exception in handling interaction event {}",e);
            throw new RuntimeException(e.getMessage());
        }

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
        return interactionEventsConsumerDeadQueueTopic;
    }

    @Override
    public String getRetryTopic() {
        return null;
    }

    @Override
    public String getDelayedRetryTopic() {
        return delayedRetryConsumerTopic;
    }

    @Override
    public TypeReference<AdInteractionEvent> getTypeReference() {
        return new TypeReference<AdInteractionEvent>() {
        };
    }

}
