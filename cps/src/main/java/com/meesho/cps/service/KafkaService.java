package com.meesho.cps.service;

import com.meesho.ads.lib.constants.Constants;
import com.meesho.commons.enums.CommonConstants;
import com.meesho.mq.client.models.PayloadType;
import com.meesho.mq.client.models.RequestPayload;
import com.meesho.mq.client.service.MqService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author shubham.aggarwal
 * 16/08/21
 */
@Slf4j
@Service
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MqService mqService;

    public ListenableFuture<SendResult<String, String>> sendMessage(String topic, String key, String msg) {
        return sendMessage(topic,key,msg,0);
    }

    public ListenableFuture<SendResult<String, String>> sendMessage(String topic, String key, String msg, Integer retryCount) {
        ProducerRecord<String, String> producerRecord;

        if (key != null) {
            producerRecord = new ProducerRecord<>(topic, key, msg);
        } else {
            producerRecord = new ProducerRecord<>(topic, msg);
        }

        producerRecord.headers().add(new RecordHeader(CommonConstants.COUNTRY_HEADER, MDC.get(Constants.COUNTRY_CODE).getBytes()));
        producerRecord.headers().add(new RecordHeader(Constants.Kafka.RETRY_COUNT_HEADER, retryCount.toString().getBytes()));
        return kafkaTemplate.send(producerRecord);
    }

    public void sendMessageToMq(Long mqId, String key, String msg) {
        sendMessageToMq(mqId, key, msg, 0);
    }

    public void sendMessageToMq(Long mqId, String key, String msg, Integer retryCount) {
        Map<String, byte[]> headersMap = new HashMap<>();
        headersMap.put(CommonConstants.COUNTRY_HEADER, MDC.get(Constants.COUNTRY_CODE).getBytes());
        headersMap.put(Constants.Kafka.RETRY_COUNT_HEADER, retryCount.toString().getBytes());

        List<RequestPayload> eventsList = Collections.singletonList(new RequestPayload(key, msg, PayloadType.STRING, headersMap));
        mqService.sendAndForget(mqId, eventsList);
    }

}
