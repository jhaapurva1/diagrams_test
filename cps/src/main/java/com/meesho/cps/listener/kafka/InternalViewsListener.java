package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import com.meesho.cps.service.CatalogViewEventService;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class InternalViewsListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    CatalogViewEventService catalogViewEventService;


    @KafkaListener(id = "MongoConsumer", containerFactory = ConsumerConstants.AdServiceKafka.BATCH_CONTAINER_FACTORY,
            topics = "ad_views_internal", autoStartup = "true", concurrency = "10",
            properties = {ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" + ConsumerConstants.InteractionEventsConsumer.MAX_POLL_INTERVAL_MS,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + "10"})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=EsToMongoConsumer")
    public void listen(List<ConsumerRecord<String, String>> consumerRecords){
        List<CampaignCatalogViewCount> campaignCatalogViewCountList = new ArrayList<>();
        for (ConsumerRecord<String, String> record1 : consumerRecords) {
            try {
                List<CampaignCatalogViewCount> campaignCatalogViewCounts = objectMapper.readValue(record1.value(), new TypeReference<List<CampaignCatalogViewCount>>(){});
                campaignCatalogViewCountList.addAll(campaignCatalogViewCounts);
            } catch (JsonProcessingException e) {
                log.error("Error in consuming msg to save to mongo", e);
            }
        }
        try {
            handle(campaignCatalogViewCountList);
        } catch (Exception e) {
            log.error("Exception while writing view counts to mongo", e);
        }

    }


    public void handle(List<CampaignCatalogViewCount> campaignCatalogViewCountList) {
        catalogViewEventService.writeToMongo(campaignCatalogViewCountList);
    }

}
