package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.exception.DataValidationException;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.AdViewEvent;
import com.meesho.cps.helper.ValidationHelper;
import com.meesho.cps.service.CatalogViewEventService;
import com.meesho.cps.service.KafkaService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Slf4j
@Component
public class IngestionViewEventListener {

    @Autowired
    CatalogViewEventService catalogViewEventService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private KafkaService kafkaService;

    @KafkaListener(id = ConsumerConstants.IngestionViewEventsConsumer.ID, containerFactory =
            ConsumerConstants.IngestionServiceKafka.CONTAINER_FACTORY, topics = {
            "#{'${ingestion.view.event.consumer.topics}'.split(',')}",
            ConsumerConstants.IngestionViewEventsConsumer.ANONYMOUS_USER_TOPIC}, autoStartup =
            ConsumerConstants.IngestionViewEventsConsumer.AUTO_START, concurrency =
            ConsumerConstants.IngestionViewEventsConsumer.CONCURRENCY, properties = {
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" +
                    ConsumerConstants.IngestionViewEventsConsumer.MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + ConsumerConstants.IngestionViewEventsConsumer.BATCH_SIZE})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=IngestionViewEventListener")
    public void listen(ConsumerRecord<String, GenericRecord> consumerRecord) {
        try {
            MDC.put(Constants.GUID, UUID.randomUUID().toString());
            String countryCode = "IN";
            MDC.put(Constants.COUNTRY_CODE, Country.getValueDefaultCountryFromEnv(countryCode).getCountryCode());

            String value = consumerRecord.value().toString();
            log.info("Ingestion view event received : {}", value);

            AdViewEvent adViewEvent = objectMapper.readValue(value, AdViewEvent.class);

            if (!ValidationHelper.isValidAdViewEvent(adViewEvent)) {
                log.error("Invalid event {}", adViewEvent);
                throw new DataValidationException("Invalid event");
            }
            log.info("Processing view event for catalogId {} userId {} appVersionCode {}",
                    adViewEvent.getProperties().getId(), adViewEvent.getUserId(),
                    adViewEvent.getProperties().getAppVersionCode());
            catalogViewEventService.handle(adViewEvent);
        } catch (Exception e) {
            log.error("Exception while processing ingestion view event {}", consumerRecord, e);
            kafkaService.sendMessage(com.meesho.cps.constants.Constants.INGESTION_VIEW_EVENTS_DEAD_QUEUE_TOPIC,
                    consumerRecord.key(), consumerRecord.value().toString());
        } finally {
            MDC.clear();
        }
    }

}
