package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.meesho.ads.lib.exception.DataValidationException;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.listener.kafka.BaseManualAcknowledgeKafkaListener;
import com.meesho.cps.constants.AdInteractionStatus;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.helper.AdWidgetValidationHelper;
import com.meesho.cps.service.CatalogInteractionEventService;
import com.meesho.cps.service.WidgetClickEventService;
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
import static com.meesho.cps.constants.TelegrafConstants.NAN;

@Slf4j
@Component
public class AdWidgetClickEventListener extends BaseManualAcknowledgeKafkaListener<AdWidgetClickEvent> {

    @Autowired
    TelegrafMetricsHelper telegrafMetricsHelper;

    @Autowired
    CatalogInteractionEventService catalogInteractionEventService;

    @Autowired
    WidgetClickEventService widgetClickEventService;

    @KafkaListener(id = ConsumerConstants.AdWidgetClickEventConsumer.CONSUMER_ID,
            containerFactory = ConsumerConstants.IngestionServiceConfluentKafka.MANUAL_ACK_CONTAINER_FACTORY,
            topics = ConsumerConstants.AdWidgetClickEventConsumer.TOPIC,
            autoStartup = ConsumerConstants.AdWidgetClickEventConsumer.AUTO_START,
            concurrency = ConsumerConstants.AdWidgetClickEventConsumer.CONCURRENCY,
            properties = {
                    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                            ConsumerConstants.AdWidgetClickEventConsumer.MAX_POLL_INTERVAL_MS,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" +
                            ConsumerConstants.AdWidgetClickEventConsumer.BATCH_SIZE
            })
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=AdWidgetClickEventListener")
    public void listenGenericRecord(ConsumerRecord<String, GenericRecord> consumerRecord, Acknowledgment acknowledgment) {
        super.listenGenericRecord(consumerRecord, acknowledgment);
    }

    @Override
    public void consume(AdWidgetClickEvent adWidgetClickEvent) throws DataValidationException {
        log.info("Consuming ad widget click event: {}", adWidgetClickEvent);
        if (!AdWidgetValidationHelper.isValidAdWidgetClickEvent(adWidgetClickEvent)) {
            log.warn("Invalid event {}", adWidgetClickEvent);
            telegrafMetricsHelper.increment(WIDGET_CLICK_EVENT_KEY, INTERACTION_EVENT_TAGS, adWidgetClickEvent.getEventName(), NAN,NAN,
                    AdInteractionStatus.INVALID.name(), NAN);
            return;
        }
        log.info("Processing interaction event for userId {}, catalogId {}, " + "appVersionCode : {}",
                adWidgetClickEvent.getUserId(), adWidgetClickEvent.getProperties().getCatalogId(),
                adWidgetClickEvent.getProperties().getAppVersionCode());
        try {
            widgetClickEventService.handle(adWidgetClickEvent);
        } catch (Exception e){
            log.error("Exception in handling interaction event {}",e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public TypeReference<AdWidgetClickEvent> getTypeReference() {
        return new TypeReference<AdWidgetClickEvent>() {};
    }

}
