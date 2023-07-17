package com.meesho.cps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.meesho.ad.client.constants.FeedType;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.internal.CampaignBudgetUtilisedData;
import com.meesho.cps.data.entity.mongodb.collection.*;
import com.meesho.cps.data.entity.kafka.CatalogBudgetExhaustEvent;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.DayWisePerformancePrismEvent;
import com.meesho.cps.data.entity.kafka.SupplierWeeklyBudgetExhaustedEvent;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.data.request.BudgetExhaustedEventRequest;
import com.meesho.cps.data.request.CampaignCatalogDateMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignDateWiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;
import com.meesho.cps.data.request.CatalogCPCDiscountSaveRequest;
import com.meesho.cps.db.mongodb.dao.CampaignCatalogDateMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignDateWiseMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignMetricsDao;
import com.meesho.cps.db.mongodb.dao.CatalogCPCDiscountDao;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.helper.BackFillCampaignHelper;
import com.meesho.cps.helper.InteractionEventAttributionHelper;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cps.transformer.DebugTransformer;
import com.meesho.cps.transformer.PrismEventTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@Service
@Slf4j
public class DebugService {

    @Autowired
    CampaignMetricsDao campaignMetricsDao;

    @Autowired
    CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Autowired
    CampaignDateWiseMetricsDao campaignDateWiseMetricsDao;

    @Autowired
    CatalogCPCDiscountDao catalogCPCDiscountDao;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    KafkaService kafkaService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private PrismService prismService;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    InteractionEventAttributionHelper interactionEventAttributionHelper;

    @Value(ConsumerConstants.DayWisePerformanceEventsConsumer.TOPIC)
    String dayWisePerformanceEventsConsumerTopic;

    @Value(Constants.Kafka.INTERACTION_EVENT_MQ_ID)
    Long interactionEventMqID;

    public CampaignCatalogDateMetrics saveCampaignCatalogMetrics(
            CampaignCatalogDateMetricsSaveRequest campaignCatalogMetricsSaveRequest) throws Exception {
        CampaignCatalogDateMetrics campaignCatalogDateMetrics =
                campaignCatalogDateMetricsDao.find(campaignCatalogMetricsSaveRequest.getCampaignId(),
                        campaignCatalogMetricsSaveRequest.getCatalogId(), campaignCatalogMetricsSaveRequest.getDate().toString());
        campaignCatalogDateMetrics =
                DebugTransformer.getCampaignCatalogMetrics(campaignCatalogMetricsSaveRequest, campaignCatalogDateMetrics);
        campaignCatalogDateMetricsDao.save(Collections.singletonList(campaignCatalogDateMetrics));
        CampaignCatalogDate campaignCatalogDate = new CampaignCatalogDate();
        campaignCatalogDate.setCampaignId(campaignCatalogDateMetrics.getCampaignId());
        campaignCatalogDate.setCatalogId(campaignCatalogDateMetrics.getCatalogId());
        campaignCatalogDate.setDate(campaignCatalogDateMetrics.getDate().toString());
        updatedCampaignCatalogCacheDao.add(Arrays.asList(campaignCatalogDate));
        return campaignCatalogDateMetrics;
    }

    public CampaignMetrics saveCampaignMetrics(CampaignMetricsSaveRequest campaignMetricsSaveRequest) throws Exception {
        CampaignMetrics campaignMetrics = DebugTransformer.transform(campaignMetricsSaveRequest);
        campaignMetricsDao.save(campaignMetrics);
        return campaignMetrics;
    }

    public CampaignDateWiseMetrics saveCampaignDateWiseMetrics(
            CampaignDateWiseMetricsSaveRequest campaignDateWiseMetricsSaveRequest) throws Exception {
        CampaignDateWiseMetrics campaignDatewiseMetrics =
                DebugTransformer.transform(campaignDateWiseMetricsSaveRequest);
        campaignDateWiseMetricsDao.save(campaignDatewiseMetrics);
        return campaignDatewiseMetrics;
    }

    public CampaignDateWiseMetrics getCampaignDateWiseMetrics(Long campaignId, String date) {
        return campaignDateWiseMetricsDao.findByCampaignIdAndDate(campaignId,
                DateTimeUtils.getLocalDate(date, "yyyy-MM-dd").toString());
    }

    public CampaignMetrics getCampaignMetrics(Long campaignId) {
        return campaignMetricsDao.findByCampaignId(campaignId);
    }

    public CampaignCatalogDateMetrics getCampaignCatalogMetrics(Long campaignId, Long catalogId, LocalDate date) {
        return campaignCatalogDateMetricsDao.find(campaignId, catalogId, date.toString());
    }

    // Debug service
    public void BackillCampaignCatalogDayPerformanceEventsToPrism(String filePath) {
        log.info("Starting day performance events back fill script");
        List<CampaignCatalogDate> campaignCatalogDates = new ArrayList<>();
        try {
            FileReader fileReader = new FileReader(filePath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            campaignCatalogDates = BackFillCampaignHelper.getCampaignCatalogAndDateFromCSV(bufferedReader);
        } catch (IOException e) {
            log.error("Error reading file {}", filePath, e);
            return;
        }

        List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = new ArrayList<>();

        campaignCatalogDates.forEach(ccd -> {
            CampaignCatalogDateMetrics campaignCatalogDateMetrics = campaignCatalogDateMetricsDao
                    .find(ccd.getCampaignId(),ccd.getCatalogId(), LocalDate.parse(ccd.getDate()).toString());
            campaignCatalogDateMetricsList.add(campaignCatalogDateMetrics);
        });
        List<CampaignCatalogDateMetrics> filteredList = campaignCatalogDateMetricsList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Integer eventBatchSize = applicationProperties.getBackfillDateWiseMetricsBatchSize();

        List<DayWisePerformancePrismEvent> dayWisePerformancePrismEvents = PrismEventTransformer
                .getDayWisePerformancePrismEvent(filteredList);

        List<List<DayWisePerformancePrismEvent>> batchEventLists = Lists.partition(dayWisePerformancePrismEvents,
                eventBatchSize);

        for (int i = 1; i <= batchEventLists.size(); i++) {
            prismService.publishEvent(Constants.PrismEventNames.DAY_WISE_PERF_EVENTS, batchEventLists.get(i-1));
            log.info("Backfill event batch processed "+ i);
        }
    }

    public CatalogCPCDiscount saveCatalogCPCDiscount(CatalogCPCDiscountSaveRequest request) {
        CatalogCPCDiscount catalogCPCDiscount = DebugTransformer.transform(request);
        catalogCPCDiscountDao.save(Collections.singletonList(catalogCPCDiscount));
        return catalogCPCDiscount;
    }

    public CatalogCPCDiscount getCatalogCPCDiscount(Long catalogId) {
        return catalogCPCDiscountDao.get(catalogId);
    }

    public void sendBudgetExhaustedEvent(BudgetExhaustedEventRequest request) {
        interactionEventAttributionHelper.sendBudgetExhaustedEvent(request.getCampaignId(), request.getCatalogId());
    }

    public void sendCatalogBudgetExhaustEvent(CatalogBudgetExhaustEvent request) {
        interactionEventAttributionHelper.sendCatalogBudgetExhaustEvent(request.getCampaignId(), request.getCatalogId());
    }

    public void sendSupplierBudgetExhaustedEvent(SupplierWeeklyBudgetExhaustedEvent request) {
        interactionEventAttributionHelper.sendSupplierBudgetExhaustedEvent(request.getSupplierId(), request.getCatalogId());
    }

    public void publishKafkaInteractionEvent(AdInteractionEvent adInteractionEvent) throws JsonProcessingException {
        kafkaService.sendMessageToMq(interactionEventMqID,
                adInteractionEvent.getProperties().getId().toString(),
                objectMapper.writeValueAsString(adInteractionEvent));
    }

    public CampaignBudgetUtilisedData incrementBudgetUtilised(Long campaignId, Double value, FeedType realEstate) {
        return campaignMetricsDao.incrementCampaignAndRealEstateBudgetUtilised(campaignId, BigDecimal.valueOf(value),
                realEstate);
    }
}
