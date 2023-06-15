package com.meesho.cps.listener.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.mongodb.collection.*;
import com.meesho.cps.data.entity.toberemoved.*;
import com.meesho.cps.db.mongodb.dao.*;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class MigrationListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Autowired
    private CampaignMetricsDao campaignMetricsDao;

    private static final String hbaseMigrationConsumerEnabled = "${migration.total.budget.campaign.consumer.enabled}";

    private static final String eseMigrationConsumerEnabled = "${migration.es.to.mongo.consumer.enabled}";

    @KafkaListener(id = "EsToMongoConsumer", containerFactory = ConsumerConstants.AdServiceKafka.BATCH_CONTAINER_FACTORY,
            topics = "es_to_mongo", autoStartup = eseMigrationConsumerEnabled, concurrency = "10",
            properties = {ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" + ConsumerConstants.InteractionEventsConsumer.MAX_POLL_INTERVAL_MS,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + "500"})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=EsToMongoConsumer")
    public void listen(List<ConsumerRecord<String, String>> consumerRecords) {
        List<CampaignCatalogDateMetrics> mongoDocs = new ArrayList<>();

        for (ConsumerRecord<String, String> record1 : consumerRecords) {
            try {
                EsDailyIndexDocument esDailyIndexDocument = objectMapper.readValue(record1.value(), EsDailyIndexDocument.class);
                CampaignCatalogDateMetrics existingDocument = campaignCatalogDateMetricsDao.find(esDailyIndexDocument.getCampaignId(), esDailyIndexDocument.getCatalogId(), esDailyIndexDocument.getDate());
                mongoDocs.add(transform(esDailyIndexDocument, existingDocument));
            } catch (JsonProcessingException e) {
                log.error("Error in consuming es msg to save to mongo", e);
            }
        }

        try {
            campaignCatalogDateMetricsDao.save(mongoDocs);
        } catch (Exception e) {
            log.error("exception while saving es doc to mongo - ", e);
        }
    }

    @KafkaListener(id = "CampaignsMongoConsumer", containerFactory = ConsumerConstants.AdServiceKafka.BATCH_CONTAINER_FACTORY,
            topics = "hbase_campaign_to_mongo", autoStartup = hbaseMigrationConsumerEnabled, concurrency = "10",
            properties = {ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG + "=" + ConsumerConstants.InteractionEventsConsumer.MAX_POLL_INTERVAL_MS,
                    ConsumerConfig.MAX_POLL_RECORDS_CONFIG + "=" + "1"})
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "consumer=HbaseCampaignToMongoConsumer")
    public void listenCampaignMetrics(List<ConsumerRecord<String, String>> consumerRecords) {
        MDC.put("guid", UUID.randomUUID().toString());
        List<CampaignMetrics> mongoDocs = new ArrayList<>();

        for (ConsumerRecord<String, String> record1 : consumerRecords) {
            try {
                List<HBaseCampaignMetrics> documents = objectMapper.readValue(record1.value(), new TypeReference<List<HBaseCampaignMetrics>>(){});
                for (HBaseCampaignMetrics doc : documents) {
                    CampaignMetrics existingDocument = campaignMetricsDao.findByCampaignId(doc.getCampaignId());
                    mongoDocs.add(transform(doc, existingDocument));
                }
            } catch (JsonProcessingException e) {
                log.error("Error in consuming total budget campaign msg to save to mongo", e);
            }
        }

        try {
            campaignMetricsDao.saveAll(mongoDocs);
        } catch (Exception e) {
            log.error("exception while saving es doc to mongo - " + e);
        }
    }

    private CampaignCatalogDateMetrics transform(EsDailyIndexDocument d, CampaignCatalogDateMetrics existingDocument) {
        if (existingDocument == null) {
            existingDocument = new CampaignCatalogDateMetrics();
        }
        existingDocument.setCampaignId(d.getCampaignId());
        existingDocument.setCatalogId(d.getCatalogId());
        existingDocument.setSupplierId(d.getSupplierId());
        existingDocument.setDate(d.getDate());
        existingDocument.setClicks(d.getClicks());
        existingDocument.setWishlists(d.getWishlist());
        existingDocument.setShares(d.getShares());
        existingDocument.setViews(d.getViews());
        existingDocument.setRevenue(d.getRevenue());
        existingDocument.setBudgetUtilised(d.getBudgetUtilised());
        existingDocument.setOrders(d.getOrders());
        return existingDocument;
    }

    private CampaignMetrics transform(HBaseCampaignMetrics d, CampaignMetrics existingDocument) {
        if (existingDocument == null) {
            existingDocument = new CampaignMetrics();
        }
        existingDocument.setBudgetUtilised(d.getBudgetUtilised());
        existingDocument.setCampaignId(d.getCampaignId());
        return existingDocument;
    }


}
