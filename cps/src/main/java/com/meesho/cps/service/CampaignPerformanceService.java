package com.meesho.cps.service;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.elasticsearch.EsCampaignCatalogAggregateResponse;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.kafka.DayWisePerformancePrismEvent;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import com.meesho.cps.db.elasticsearch.ElasticSearchRepository;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.mysql.dao.CampaignPerformanceDao;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cps.transformer.PrismEventTransformer;
import com.meesho.cps.utils.CommonUtils;
import com.meesho.cpsclient.request.BudgetUtilisedRequest;
import com.meesho.cpsclient.request.CampaignCatalogPerformanceRequest;
import com.meesho.cpsclient.request.CampaignPerformanceRequest;
import com.meesho.cpsclient.request.SupplierPerformanceRequest;
import com.meesho.cpsclient.response.BudgetUtilisedResponse;
import com.meesho.cpsclient.response.CampaignCatalogPerformanceResponse;
import com.meesho.cpsclient.response.CampaignPerformanceResponse;
import com.meesho.cpsclient.response.SupplierPerformanceResponse;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Slf4j
@Service
public class CampaignPerformanceService {

    @Autowired
    private CampaignPerformanceDao campaignPerformanceDao;

    @Autowired
    private CampaignMetricsRepository campaignMetricsRepository;

    @Autowired
    private CampaignCatalogDateMetricsRepository campaignCatalogMetricsRepository;

    @Autowired
    private ElasticSearchRepository elasticSearchRepository;

    @Autowired
    private CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Autowired
    private CampaignPerformanceTransformer campaignPerformanceTransformer;

    @Autowired
    private CampaignPerformanceHelper campaignPerformanceHelper;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private PrismService prismService;

    public SupplierPerformanceResponse getSupplierPerformanceMetrics(SupplierPerformanceRequest request) throws IOException {
        ElasticFiltersRequest elasticFiltersRequestMonthWise = ElasticFiltersRequest.builder()
                .supplierIds(Collections.singletonList(request.getSupplierId()))
                .aggregationBuilders(campaignPerformanceHelper.createAggregations())
                .build();

        EsCampaignCatalogAggregateResponse monthWiseResponse = new EsCampaignCatalogAggregateResponse();
        EsCampaignCatalogAggregateResponse dateWiseResponse = new EsCampaignCatalogAggregateResponse();

        if (!request.isDateRangePresent()) {
            elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addTillDateRangeFilter());
            monthWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsMonthWise(elasticFiltersRequestMonthWise);
        } else {
            if(CommonUtils.shouldQueryMonthWiseIndex(request.getStartDate(), request.getEndDate())) {
                elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addMonthWiseRangeFilter(request.getStartDate(), request.getEndDate()));
                monthWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsMonthWise(elasticFiltersRequestMonthWise);
            }
            elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addDateWiseRangeFilters(request.getStartDate(), request.getEndDate()));
            dateWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsDateWise(elasticFiltersRequestMonthWise);
        }

        return campaignPerformanceTransformer.getSupplierPerformanceResponse(monthWiseResponse, dateWiseResponse);
    }

    public CampaignPerformanceResponse getCampaignPerformanceMetrics(CampaignPerformanceRequest request) throws IOException {
        ElasticFiltersRequest elasticFiltersRequestMonthWise = ElasticFiltersRequest.builder()
                .campaignIds(request.getCampaignIds())
                .aggregationBuilders(campaignPerformanceHelper.createBucketAggregations(Constants.ESConstants.BY_CAMPAIGN, DBConstants.ElasticSearch.CAMPAIGN_ID))
                .build();

        EsCampaignCatalogAggregateResponse monthWiseResponse = new EsCampaignCatalogAggregateResponse();
        EsCampaignCatalogAggregateResponse dateWiseResponse = new EsCampaignCatalogAggregateResponse();

        if (!request.isDateRangePresent()) {
            elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addTillDateRangeFilter());
            monthWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsMonthWise(elasticFiltersRequestMonthWise);
        } else {
            if(CommonUtils.shouldQueryMonthWiseIndex(request.getStartDate(), request.getEndDate())) {
                elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addMonthWiseRangeFilter(request.getStartDate(), request.getEndDate()));
                monthWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsMonthWise(elasticFiltersRequestMonthWise);
            }
            elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addDateWiseRangeFilters(request.getStartDate(), request.getEndDate()));
            dateWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsDateWise(elasticFiltersRequestMonthWise);
        }
        return campaignPerformanceTransformer.getCampaignPerformanceResponse(monthWiseResponse, dateWiseResponse, request.getCampaignIds());
    }

    public CampaignCatalogPerformanceResponse getCampaignCatalogPerformanceMetrics(CampaignCatalogPerformanceRequest request) throws IOException {
        ElasticFiltersRequest elasticFiltersRequestMonthWise = ElasticFiltersRequest.builder()
                .campaignIds(Collections.singletonList(request.getCampaignId()))
                .catalogIds(request.getCatalogIds())
                .aggregationBuilders(campaignPerformanceHelper.createBucketAggregations(Constants.ESConstants.BY_CATALOG, DBConstants.ElasticSearch.CATALOG_ID))
                .build();

        EsCampaignCatalogAggregateResponse monthWiseResponse = new EsCampaignCatalogAggregateResponse();
        EsCampaignCatalogAggregateResponse dateWiseResponse = new EsCampaignCatalogAggregateResponse();

        if (!request.isDateRangePresent()) {
            elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addTillDateRangeFilter());
            monthWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsMonthWise(elasticFiltersRequestMonthWise);
        } else {
            if(CommonUtils.shouldQueryMonthWiseIndex(request.getStartDate(), request.getEndDate())) {
                elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addMonthWiseRangeFilter(request.getStartDate(), request.getEndDate()));
                monthWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsMonthWise(elasticFiltersRequestMonthWise);
            }
            elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addDateWiseRangeFilters(request.getStartDate(), request.getEndDate()));
            dateWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsDateWise(elasticFiltersRequestMonthWise);
        }
        return campaignPerformanceTransformer.getCampaignCatalogPerformanceResponse(monthWiseResponse, dateWiseResponse,
                request.getCampaignId(), request.getCatalogIds());
    }

    public BudgetUtilisedResponse getBudgetUtilised(BudgetUtilisedRequest request) {
        Map<String, List<BudgetUtilisedRequest.CampaignData>> campaignTypeAndCampaignIdsMap =
                request.getCampaignDataList()
                        .stream()
                        .collect(Collectors.groupingBy(BudgetUtilisedRequest.CampaignData::getCampaignType));

        List<Long> dailyBudgetCampaignIds =
                campaignTypeAndCampaignIdsMap.getOrDefault(CampaignType.DAILY_BUDGET.getValue(), new ArrayList<>())
                        .stream()
                        .map(BudgetUtilisedRequest.CampaignData::getCampaignId)
                        .collect(Collectors.toList());

        List<Long> totalBudgetCampaignIds =
                campaignTypeAndCampaignIdsMap.getOrDefault(CampaignType.TOTAL_BUDGET.getValue(), new ArrayList<>())
                        .stream()
                        .map(BudgetUtilisedRequest.CampaignData::getCampaignId)
                        .collect(Collectors.toList());

        LocalDate dailyBudgetTrackingDate = campaignPerformanceHelper.getLocalDateForDailyCampaignFromLocalDateTime(DateTimeUtils.getCurrentLocalDateTimeInIST());

        List<CampaignDatewiseMetrics> campaignDatewiseMetrics =
                campaignDatewiseMetricsRepository.getAll(dailyBudgetCampaignIds,
                        dailyBudgetTrackingDate);
        List<CampaignMetrics> campaignMetrics = campaignMetricsRepository.getAll(totalBudgetCampaignIds);

        return campaignPerformanceTransformer.getBudgetUtilisedResponse(campaignMetrics, campaignDatewiseMetrics);
    }

    // Debug service
    public void BackillCampaignCatalogDayPerformanceEventsToPrism(){

        List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = campaignCatalogMetricsRepository
                .scanInDateRange(LocalDateTime.of(2021, Month.DECEMBER, 22, 0,0));

        Integer eventBatchSize = applicationProperties.getBackfillDateWiseMetricsBatchSize();

        List<DayWisePerformancePrismEvent> dayWisePerformancePrismEvents = PrismEventTransformer
                .getDayWisePerformancePrismEvent(campaignCatalogDateMetricsList);

        int eventSize = dayWisePerformancePrismEvents.size();
        int start = 0, batch = 1;

        while (start < eventBatchSize) {
            int toIndex;
            if(start+eventBatchSize<eventSize){
                toIndex = start+eventBatchSize-1;
            }else {
                toIndex = eventSize-1;
            }
            prismService.publishEvent(Constants.PrismEventNames.DAY_WISE_PERF_EVENTS,dayWisePerformancePrismEvents
                    .subList(start, toIndex));

            log.info("Backfill event batch "+ batch, dayWisePerformancePrismEvents
                    .subList(start, toIndex));

            start = start + eventBatchSize;
            batch += 1;

        }

    }
}
