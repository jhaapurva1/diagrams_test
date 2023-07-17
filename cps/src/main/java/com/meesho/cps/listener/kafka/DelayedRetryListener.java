package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.data.internal.DelayRetryMqMessage;
import com.meesho.ads.lib.utils.ListenerUtils;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.service.KafkaService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.mq.client.annotation.MqListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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

    @MqListener(mqId = ConsumerConstants.DelayedRetryConsumer.MQ_ID, type = String.class)
    public void listen(ConsumerRecord<String, String> consumerRecord) {
        digestLoggerProxy(consumerRecord);
    }

    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=DelayedRetryListener")
    public void digestLoggerProxy(ConsumerRecord<String, String> consumerRecord){
        consume(consumerRecord);
    }

    public void consume(ConsumerRecord<String, String> consumerRecord) {
        try {
            MDC.clear();
            MDC.put(Constants.GUID, UUID.randomUUID().toString());
            String countryCode = ListenerUtils.getCountryCodeFromConsumerRecord(consumerRecord);
            MDC.put(Constants.COUNTRY_CODE, Country.getValueDefaultCountryFromEnv(countryCode).getCountryCode());

            DelayRetryMqMessage delayRetryMqMessage = objectMapper.readValue(
                    consumerRecord.value(),
                    DelayRetryMqMessage.class
            );
            int retryCount = Integer.parseInt(
                    ListenerUtils.getHeader(consumerRecord,Constants.Kafka.RETRY_COUNT_HEADER)
            );

            long waitTime = delayRetryMqMessage.getRetryAfterTimestamp() - System.currentTimeMillis();
            if(waitTime > 0){
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    log.error("InterruptedException while waiting in DelayedRetryListener",e);
                    return;
                }
            }
            kafkaService.sendMessageToMq(
                    delayRetryMqMessage.getRetryMqId(),
                    consumerRecord.key(),
                   delayRetryMqMessage.getMessage(),
                    retryCount
            );
        } catch (Exception e){
            log.error("Exception in DelayedRetryListener ",e);
        } finally {
            MDC.clear();
        }
    }

}
