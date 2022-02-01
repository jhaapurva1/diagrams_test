package com.meesho.cps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.meesho.ads.lib.data.internal.PaginatedResult;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.ads.lib.utils.HbaseUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.kafka.DayWisePerformancePrismEvent;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.data.request.CampaignCatalogDateMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignDatewiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.mysql.dao.CampaignPerformanceDao;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cps.transformer.DebugTransformer;
import com.meesho.cps.transformer.PrismEventTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Period;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@Service
@Slf4j
public class DebugService {

    @Autowired
    CampaignMetricsRepository campaignMetricsRepository;

    @Autowired
    CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Autowired
    CampaignCatalogMetricsRepository campaignCatalogMetricsRepository;

    @Autowired
    CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Autowired
    CampaignPerformanceDao campaignPerformanceDao;

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

    public CampaignCatalogDateMetrics saveCampaignCatalogMetrics(
            CampaignCatalogDateMetricsSaveRequest campaignCatalogMetricsSaveRequest) throws Exception {
        CampaignCatalogDateMetrics campaignCatalogDateMetrics =
                campaignCatalogDateMetricsRepository.get(campaignCatalogMetricsSaveRequest.getCampaignId(),
                        campaignCatalogMetricsSaveRequest.getCatalogId(), campaignCatalogMetricsSaveRequest.getDate());
        campaignCatalogDateMetrics =
                DebugTransformer.getCampaignCatalogMetrics(campaignCatalogMetricsSaveRequest, campaignCatalogDateMetrics);
        campaignCatalogDateMetricsRepository.put(campaignCatalogDateMetrics);
        CampaignCatalogDate campaignCatalogDate = new CampaignCatalogDate();
        campaignCatalogDate.setCampaignId(campaignCatalogDateMetrics.getCampaignId());
        campaignCatalogDate.setCatalogId(campaignCatalogDateMetrics.getCatalogId());
        campaignCatalogDate.setDate(campaignCatalogDateMetrics.getDate().toString());
        updatedCampaignCatalogCacheDao.add(Arrays.asList(campaignCatalogDate));
        return campaignCatalogDateMetrics;
    }

    public CampaignMetrics saveCampaignMetrics(CampaignMetricsSaveRequest campaignMetricsSaveRequest) throws Exception {
        CampaignMetrics campaignMetrics = DebugTransformer.transform(campaignMetricsSaveRequest);
        campaignMetricsRepository.put(campaignMetrics);
        return campaignMetrics;
    }

    public CampaignDatewiseMetrics saveCampaignDatewiseMetrics(
            CampaignDatewiseMetricsSaveRequest campaignDateWiseMetricsSaveRequest) throws Exception {
        CampaignDatewiseMetrics campaignDatewiseMetrics =
                DebugTransformer.transform(campaignDateWiseMetricsSaveRequest);
        campaignDatewiseMetricsRepository.put(campaignDatewiseMetrics);
        return campaignDatewiseMetrics;
    }

    public CampaignDatewiseMetrics getCampaignDatewiseMetrics(Long campaignId, String date) {
        return campaignDatewiseMetricsRepository.get(campaignId,
                DateTimeUtils.getLocalDate(date, HbaseUtils.HBASE_DATE_FORMAT));
    }

    public CampaignMetrics getCampaignMetrics(Long campaignId) {
        return campaignMetricsRepository.get(campaignId);
    }

    public CampaignCatalogDateMetrics getCampaignCatalogMetrics(Long campaignId, Long catalogId, LocalDate date) {
        return campaignCatalogDateMetricsRepository.get(campaignId, catalogId, date);
    }

    public void performMigrationOfCampaignPerformance() {
        String cursor = StringUtils.EMPTY;
        PaginatedResult<String, CampaignCatalogMetrics> page;

        log.info("Starting lifetime to day metrics migration script");
        int processedRows = 0;

        do {
            page = campaignCatalogMetricsRepository.scan(cursor, applicationProperties.getCampaignDatewiseMetricsBatchSize());
            List<CampaignCatalogMetrics> campaignCatalogMetricsList = page.getResults();
            List<Long> campaignIds = campaignCatalogMetricsList.stream().map(CampaignCatalogMetrics::getCampaignId).collect(Collectors.toList());

            List<CampaignPerformance> campaignPerformanceList = campaignPerformanceDao.findAllByCampaignIds(campaignIds).stream()
                    .filter(cp -> Objects.nonNull(cp.getCampaignId()) && Objects.nonNull(cp.getCatalogId())).collect(Collectors.toList());
            Map<Pair<Long, Long>, CampaignPerformance> campaignPerformanceMap = campaignPerformanceList.stream()
                    .collect(Collectors.toMap(cp -> Pair.of(cp.getCampaignId(), cp.getCatalogId()), Function.identity()));

            List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = new ArrayList<>();

            for (CampaignCatalogMetrics cm : campaignCatalogMetricsList) {
                CampaignCatalogDateMetrics cc = CampaignCatalogDateMetrics.builder().campaignId(cm.getCampaignId())
                        .catalogId(cm.getCatalogId()).budgetUtilised(cm.getBudgetUtilised())
                        .date(LocalDate.now().minus(Period.ofDays(1))).viewCount(cm.getViewCount())
                        .clickCount(Optional.ofNullable(cm.getWeightedClickCount()).orElse(BigDecimal.ZERO).longValue())
                        .wishlistCount(Optional.ofNullable(cm.getWeightedWishlistCount()).orElse(BigDecimal.ZERO).longValue())
                        .sharesCount(Optional.ofNullable(cm.getWeightedSharesCount()).orElse(BigDecimal.ZERO).longValue())
                        .build();

                if (campaignPerformanceMap.containsKey(Pair.of(cm.getCampaignId(), cm.getCatalogId()))) {
                    CampaignPerformance cp = campaignPerformanceMap.get(Pair.of(cm.getCampaignId(), cm.getCatalogId()));
                    cc.setOrders(cp.getOrderCount());
                    cc.setRevenue(cp.getRevenue());
                }

                campaignCatalogDateMetricsList.add(cc);
            }
            campaignCatalogDateMetricsRepository.putAll(campaignCatalogDateMetricsList);
            cursor = page.getCursor();
            // send message to kafka to trigger es indexing
            List<CampaignCatalogDate> campaignCatalogDates = campaignCatalogDateMetricsList.stream()
                    .map(cm -> new CampaignCatalogDate(cm.getCampaignId(), cm.getCatalogId(), cm.getDate().toString()))
                    .collect(Collectors.toList());
            try {
                kafkaService.sendMessage(ConsumerConstants.DayWisePerformanceEventsConsumer.TOPIC, null,
                        objectMapper.writeValueAsString(campaignCatalogDates));
            } catch (JsonProcessingException e) {
                log.error("failed to send kafka message for campaign catalog dates: {}", campaignCatalogDates);
            }
            processedRows += campaignCatalogDateMetricsList.size();
            log.info("Processed rows {}", processedRows);
        } while (page.isHasNext());
    }

    // Debug service
    public void BackillCampaignCatalogDayPerformanceEventsToPrism(){

        List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = campaignCatalogDateMetricsRepository
                .scanInDateRange(LocalDateTime.of(0,Month.JANUARY,0,0,0),
                        LocalDateTime.of(2021, Month.DECEMBER, 22, 0,0));

        Integer eventBatchSize = applicationProperties.getBackfillDateWiseMetricsBatchSize();

        List<DayWisePerformancePrismEvent> dayWisePerformancePrismEvents = PrismEventTransformer
                .getDayWisePerformancePrismEvent(campaignCatalogDateMetricsList);

        AtomicInteger index = new AtomicInteger(1);

        List<List<DayWisePerformancePrismEvent>> batchEventLists = Lists.partition(dayWisePerformancePrismEvents,
                eventBatchSize);

        batchEventLists.forEach(batch -> {
            prismService.publishEvent(Constants.PrismEventNames.DAY_WISE_PERF_EVENTS, batch);
            log.info("Backfill event batch "+ index, batch.toString());
            index.addAndGet(1);
        });

    }
}
