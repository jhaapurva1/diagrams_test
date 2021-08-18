package com.meesho.cps.service;

import com.meesho.ads.lib.constants.Constants;
import com.meesho.commons.enums.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @author shubham.aggarwal
 * 16/08/21
 */
@Slf4j
@Service
public class KafkaService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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

}
