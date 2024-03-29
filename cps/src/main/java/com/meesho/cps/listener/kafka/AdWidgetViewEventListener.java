package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.exception.DataValidationException;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.listener.kafka.BaseManualAcknowledgeKafkaListener;
import com.meesho.cps.constants.AdInteractionStatus;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import com.meesho.cps.helper.AdWidgetValidationHelper;
import com.meesho.cps.service.WidgetViewEventService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import static com.meesho.cps.constants.TelegrafConstants.*;

@Slf4j
@Component
public class AdWidgetViewEventListener extends BaseManualAcknowledgeKafkaListener<AdWidgetViewEvent> {

    @Autowired
    TelegrafMetricsHelper telegrafMetricsHelper;

    @Autowired
    WidgetViewEventService widgetViewEventService;

    @Autowired
    ObjectMapper objectMapper;

    @KafkaListener(id = ConsumerConstants.AdWidgetViewEventConsumer.CONSUMER_ID,
            containerFactory = ConsumerConstants.IngestionServiceConfluentKafka.MANUAL_ACK_CONTAINER_FACTORY,
            topics = ConsumerConstants.AdWidgetViewEventConsumer.TOPIC,
            autoStartup = ConsumerConstants.AdWidgetViewEventConsumer.AUTO_START,
            concurrency = ConsumerConstants.AdWidgetViewEventConsumer.CONCURRENCY,
            properties = {
                ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                    ConsumerConstants.AdWidgetViewEventConsumer.MAX_POLL_INTERVAL_MS,
                ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" +
                        ConsumerConstants.AdWidgetViewEventConsumer.BATCH_SIZE
            })
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=AdWidgetViewEventListener")
    public void listenGenericRecord(ConsumerRecord<String, GenericRecord> consumerRecord, Acknowledgment acknowledgment) {
        super.listenGenericRecord(consumerRecord, acknowledgment);
    }

    @Override
    public void consume(AdWidgetViewEvent adWidgetViewEvent) throws DataValidationException {
        log.info("Consuming ad widget view event: {}", adWidgetViewEvent);
        if (Boolean.FALSE.equals(AdWidgetValidationHelper.isValidAdWidgetViewEvent(adWidgetViewEvent))) {
            log.warn("Invalid event {}", adWidgetViewEvent);
            telegrafMetricsHelper.increment(WIDGET_VIEW_EVENT_KEY, WIDGET_VIEW_EVENT_TAGS, adWidgetViewEvent.getEventName(), NAN, NAN, NAN,
                    AdInteractionStatus.INVALID.name(), NAN);
            return;
        }
        try {
            log.info("Processing ad widget view event: {}", objectMapper.writeValueAsString(adWidgetViewEvent));
            widgetViewEventService.handle(adWidgetViewEvent);
        } catch (Exception e) {
            log.error("Exception in consuming ad widget view event", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public TypeReference<AdWidgetViewEvent> getTypeReference() {
        return new TypeReference<AdWidgetViewEvent>() {};
    }
}