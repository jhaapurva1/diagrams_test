package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.data.internal.DelayedRetryMessage;
import com.meesho.ads.lib.utils.ListenerUtils;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.service.KafkaService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * @author shubham.aggarwal
 * 16/08/21
 */
@Slf4j
@Component
public class DelayedRetryListener {

    @Autowired
    private KafkaService kafkaService;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            id = ConsumerConstants.DelayedRetryConsumer.ID,
            containerFactory = ConsumerConstants.CommonKafka.CONTAINER_FACTORY,
            topics = ConsumerConstants.DelayedRetryConsumer.TOPIC,
            autoStartup = ConsumerConstants.DelayedRetryConsumer.AUTO_START,
            concurrency = ConsumerConstants.DelayedRetryConsumer.CONCURRENCY,
            properties= {
                    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" + ConsumerConstants.DelayedRetryConsumer.MAX_POLL_INTERVAL_MS,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + ConsumerConstants.DelayedRetryConsumer.BATCH_SIZE
            }
    )
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=DelayedRetryListener")
    public void listen(ConsumerRecord<String, String> consumerRecord) {
        try {
            MDC.clear();
            MDC.put(Constants.GUID, UUID.randomUUID().toString());
            String countryCode = ListenerUtils.getCountryCodeFromConsumerRecord(consumerRecord);
            MDC.put(Constants.COUNTRY_CODE, Country.getValueDefaultCountryFromEnv(countryCode).getCountryCode());

            DelayedRetryMessage delayedRetryMessage = objectMapper.readValue(
                    consumerRecord.value(),
                    DelayedRetryMessage.class
            );
            int retryCount = Integer.parseInt(
                    ListenerUtils.getHeader(consumerRecord,Constants.Kafka.RETRY_COUNT_HEADER)
            );

            long waitTime = delayedRetryMessage.getRetryAfterTimestamp() - System.currentTimeMillis();
            if(waitTime > 0){
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    log.error("InterruptedException while waiting in DelayedRetryListener",e);
                    return;
                }
            }
            kafkaService.sendMessage(
                    delayedRetryMessage.getRetryTopic(),
                    consumerRecord.key(),
                    delayedRetryMessage.getMessage(),
                    retryCount
            );
        } catch (Exception e){
            log.error("Exception in DelayedRetryListener ",e);
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(
            id = "DelayedRetryConsumer.ID",
            groupId = ConsumerConstants.DelayedRetryConsumer.ID,
            containerFactory = ConsumerConstants.AdServiceKafka.CONTAINER_FACTORY,
            topics = ConsumerConstants.DelayedRetryConsumer.TOPIC,
            autoStartup = ConsumerConstants.DelayedRetryConsumer.AUTO_START,
            concurrency = ConsumerConstants.DelayedRetryConsumer.CONCURRENCY,
            properties= {
                    ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" + ConsumerConstants.DelayedRetryConsumer.MAX_POLL_INTERVAL_MS,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + ConsumerConstants.DelayedRetryConsumer.BATCH_SIZE
            }
    )
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=DelayedRetryListener")
    public void listen_adServiceKafka(ConsumerRecord<String, String> consumerRecord) {
        try {
            MDC.clear();
            MDC.put(Constants.GUID, UUID.randomUUID().toString());
            String countryCode = ListenerUtils.getCountryCodeFromConsumerRecord(consumerRecord);
            MDC.put(Constants.COUNTRY_CODE, Country.getValueDefaultCountryFromEnv(countryCode).getCountryCode());

            DelayedRetryMessage delayedRetryMessage = objectMapper.readValue(
                    consumerRecord.value(),
                    DelayedRetryMessage.class
            );
            int retryCount = Integer.parseInt(
                    ListenerUtils.getHeader(consumerRecord,Constants.Kafka.RETRY_COUNT_HEADER)
            );

            long waitTime = delayedRetryMessage.getRetryAfterTimestamp() - System.currentTimeMillis();
            if(waitTime > 0){
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    log.error("InterruptedException while waiting in DelayedRetryListener",e);
                    return;
                }
            }
            kafkaService.sendMessage(
                    delayedRetryMessage.getRetryTopic(),
                    consumerRecord.key(),
                    delayedRetryMessage.getMessage(),
                    retryCount
            );
        } catch (Exception e){
            log.error("Exception in DelayedRetryListener ",e);
        } finally {
            MDC.clear();
        }
    }

}
