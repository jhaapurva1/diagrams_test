package com.meesho.cps.service;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.elasticsearch.EsCampaignCatalogAggregateResponse;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import com.meesho.cps.data.internal.FetchCampaignCatalogsESRequest;
import com.meesho.cps.data.internal.PerformancePojo;
import com.meesho.cps.db.elasticsearch.ElasticSearchRepository;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
import com.meesho.cps.db.mysql.dao.CampaignPerformanceDao;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cps.utils.CommonUtils;
import com.meesho.cpsclient.request.*;
import com.meesho.cpsclient.response.*;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
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
    private SupplierWeekWiseMetricsRepository supplierWeekWiseMetricsRepository;

    private final List<String> fetchActiveCampaignsESSourceIncludeFields = Arrays.asList(Constants.ESFieldNames.SUPPLIER_ID, Constants.ESFieldNames.CAMPAIGN_ID, Constants.ESFieldNames.CATALOG_ID);
    private final List<String> fetchActiveCampaignsESMustExistFields = Collections.singletonList(Constants.ESFieldNames.BUDGET_UTILISED);

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
        if (!request.isDateRangePresent()) {
            campaignPerformanceHelper.addDatesToRequest(request);
        }
        Map<Long, PerformancePojo> campaignIdToHBaseMetricsMap = null;
        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper
            .getDatesForHBaseQuery(request.getStartDate(), request.getEndDate());
        boolean isHBaseQueryRequired = !hBaseQueryDates.isEmpty();

        if (isHBaseQueryRequired) {
            List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = campaignCatalogMetricsRepository
                .get(request.getCampaignDetails().stream().collect(Collectors.toMap(
                    CampaignPerformanceRequest.CampaignDetails::getCampaignId,
                    CampaignPerformanceRequest.CampaignDetails::getCatalogIds)),
                    hBaseQueryDates);
            campaignIdToHBaseMetricsMap = campaignCatalogDateMetricsList.stream().collect(Collectors.groupingBy(CampaignCatalogDateMetrics::getCampaignId))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> campaignPerformanceHelper.getAggregatedCampaignCatalogDateMetrics(entry.getValue())));
        }

        request.setEndDate(request.getEndDate().minusDays(hBaseQueryDates.size()));
        EsCampaignCatalogAggregateResponse monthWiseResponse = new EsCampaignCatalogAggregateResponse();
        EsCampaignCatalogAggregateResponse dateWiseResponse = new EsCampaignCatalogAggregateResponse();
        List<Long> campaignIds = request.getCampaignDetails().stream()
            .map(CampaignPerformanceRequest.CampaignDetails::getCampaignId).collect(Collectors.toList());
        boolean isESQueryRequired = !request.getEndDate().isBefore(request.getStartDate());

        if (isESQueryRequired) {
            ElasticFiltersRequest elasticFiltersRequestMonthWise = ElasticFiltersRequest.builder()
                    .campaignIds(campaignIds)
                    .aggregationBuilders(campaignPerformanceHelper.createBucketAggregations(
                            Constants.ESConstants.BY_CAMPAIGN, DBConstants.ElasticSearch.CAMPAIGN_ID,
                            campaignIds.size()))
                    .build();

            if (CommonUtils.shouldQueryMonthWiseIndex(request.getStartDate(), request.getEndDate())) {
                elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addMonthWiseRangeFilter(request.getStartDate(), request.getEndDate()));
                monthWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsMonthWise(elasticFiltersRequestMonthWise);
            }
            elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addDateWiseRangeFilters(request.getStartDate(), request.getEndDate()));
            dateWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsDateWise(elasticFiltersRequestMonthWise);
        }
        return campaignPerformanceTransformer.getCampaignPerformanceResponse(monthWiseResponse, dateWiseResponse, campaignIds,
                campaignIdToHBaseMetricsMap);
    }

    public CampaignCatalogPerformanceResponse getCampaignCatalogPerformanceMetrics(CampaignCatalogPerformanceRequest request) throws IOException {
        if (!request.isDateRangePresent()) {
            campaignPerformanceHelper.addDatesToRequest(request);
        }
        Map<Long, PerformancePojo> catalogIdToHBaseMetricsMap = null;
        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper.getDatesForHBaseQuery
                (request.getStartDate(), request.getEndDate());
        boolean isHBaseQueryRequired = !hBaseQueryDates.isEmpty();

        if (isHBaseQueryRequired) {
            List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = campaignCatalogMetricsRepository
                .get(Collections.singletonMap(request.getCampaignId(), request.getCatalogIds()), hBaseQueryDates);
            catalogIdToHBaseMetricsMap = campaignCatalogDateMetricsList.stream().collect(Collectors.groupingBy(CampaignCatalogDateMetrics::getCatalogId))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> campaignPerformanceHelper.getAggregatedCampaignCatalogDateMetrics(entry.getValue())));
        }

        request.setEndDate(request.getEndDate().minusDays(hBaseQueryDates.size()));
        EsCampaignCatalogAggregateResponse monthWiseResponse = new EsCampaignCatalogAggregateResponse();
        EsCampaignCatalogAggregateResponse dateWiseResponse = new EsCampaignCatalogAggregateResponse();
        boolean isESQueryRequired = !request.getEndDate().isBefore(request.getStartDate());

        if (isESQueryRequired) {
            ElasticFiltersRequest elasticFiltersRequestMonthWise = ElasticFiltersRequest.builder()
                    .campaignIds(Collections.singletonList(request.getCampaignId()))
                    .catalogIds(request.getCatalogIds())
                    .aggregationBuilders(campaignPerformanceHelper.createBucketAggregations(
                            Constants.ESConstants.BY_CATALOG, DBConstants.ElasticSearch.CATALOG_ID,
                            request.getCatalogIds().size()))
                    .build();

            if (CommonUtils.shouldQueryMonthWiseIndex(request.getStartDate(), request.getEndDate())) {
                elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addMonthWiseRangeFilter(request.getStartDate(), request.getEndDate()));
                monthWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsMonthWise(elasticFiltersRequestMonthWise);
            }
            elasticFiltersRequestMonthWise.setRangeFilters(campaignPerformanceHelper.addDateWiseRangeFilters(request.getStartDate(), request.getEndDate()));
            dateWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsDateWise(elasticFiltersRequestMonthWise);
        }
        return campaignPerformanceTransformer.getCampaignCatalogPerformanceResponse(monthWiseResponse, dateWiseResponse,
                request.getCampaignId(), request.getCatalogIds(), catalogIdToHBaseMetricsMap);
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

        List<Long> supplierIds = request.getSuppliersIdList();

        LocalDate dailyBudgetTrackingDate = campaignPerformanceHelper.getLocalDateForDailyCampaignFromLocalDateTime(DateTimeUtils.getCurrentLocalDateTimeInIST());
        LocalDate weekStartDate = DateTimeUtils.getFirstDayOfWeek().toLocalDate();

        List<CampaignDatewiseMetrics> campaignDateWiseMetrics =
                campaignDatewiseMetricsRepository.getAll(dailyBudgetCampaignIds,
                        dailyBudgetTrackingDate);
        List<CampaignMetrics> campaignMetrics = campaignMetricsRepository.getAll(totalBudgetCampaignIds);

        List<SupplierWeekWiseMetrics> supplierWeekWiseMetrics = supplierWeekWiseMetricsRepository.getAll(supplierIds, weekStartDate);

        return campaignPerformanceTransformer.getBudgetUtilisedResponse(campaignMetrics, campaignDateWiseMetrics, supplierWeekWiseMetrics);
    }

    public CampaignCatalogDateLevelBudgetUtilisedResponse getDateLevelBudgetUtilised(CampaignCatalogDateLevelBudgetUtilisedRequest request) {
        List<CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails> campaignDetailsResponseList = new ArrayList<>();

        for (CampaignCatalogDateLevelBudgetUtilisedRequest.CampaignDetails campaignDetails : request.getCampaignDetails()) {
            CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails campaignDetailsResponse = new CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails();

            Long campaignId = campaignDetails.getCampaignId();
            LocalDate date = campaignDetails.getDate();

            campaignDetailsResponse.setCampaignId(campaignId);
            campaignDetailsResponse.setDate(date);

            List<CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails> catalogDetailsResponseList = new ArrayList<>();
            for (CampaignCatalogDateLevelBudgetUtilisedRequest.CampaignDetails.CatalogDetails catalogDetails : campaignDetails.getCatalogDetails()) {
                Long catalogId = catalogDetails.getCatalogId();
                CampaignCatalogDateMetrics metrics = campaignCatalogMetricsRepository.get(campaignId, catalogId, date);
                if (Objects.nonNull(metrics) && Objects.nonNull(metrics.getBudgetUtilised()) && Objects.nonNull(metrics.getCatalogId())) {
                    CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails catalogDetailsResponse =
                            CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails.builder()
                                    .catalogId(metrics.getCatalogId()).budgetUtilised(metrics.getBudgetUtilised()).build();
                    catalogDetailsResponseList.add(catalogDetailsResponse);
                }
                else {
                    log.error("Error in getting budget utilised for campaignId - {}, catalogId - {}, date - {}", campaignId, catalogId, date);
                }
            }
            campaignDetailsResponse.setCatalogDetails(catalogDetailsResponseList);
            campaignDetailsResponseList.add(campaignDetailsResponse);
        }

        return CampaignCatalogDateLevelBudgetUtilisedResponse.builder().campaignDetails(campaignDetailsResponseList).build();
    }

    public FetchActiveCampaignsResponse getActiveCampaignsForDate(FetchActiveCampaignsRequest request) throws IOException {

        String scrollId = campaignPerformanceHelper.decodeCursor(request.getCursor());

        FetchCampaignCatalogsESRequest.RangeFilter rangeFilter = FetchCampaignCatalogsESRequest.RangeFilter.builder()
                .fieldName(Constants.ESFieldNames.CAMPAIGN_DATE)
                .format(Constants.ESConstants.DAY_DATE_FORMAT)
                .lte(request.getDate())
                .gte(request.getDate())
                .build();

        Integer limit = request.getLimit();
        if(Objects.isNull(limit)) {
            limit = Constants.FetchCampaignCatalog.DEFAULT_LIMIT;
        }

        FetchCampaignCatalogsESRequest fetchCampaignCatalogsESRequest = FetchCampaignCatalogsESRequest.builder()
                .limit(limit)
                .scrollId(scrollId)
                .includeFields(fetchActiveCampaignsESSourceIncludeFields)
                .rangeFilters(Arrays.asList(rangeFilter))
                .mustExistFields(fetchActiveCampaignsESMustExistFields)
                .build();

        SearchResponse searchResponse = elasticSearchRepository.fetchEsCampaignCatalogsForDate(fetchCampaignCatalogsESRequest);
        log.debug("Query Response: " + searchResponse);

        return campaignPerformanceTransformer.getFetchActiveCampaignsResponse(searchResponse);

    }

    public CampaignPerformanceDatewiseResponse getCampaignCatalogPerfDateWise(
            CampaignCatalogPerfDatawiseRequest request) throws IOException {
        Map<LocalDate, PerformancePojo> dateToHBaseMetricsMap = null;
        List<LocalDate> hBaseQueryDates = campaignPerformanceHelper
                .getDatesForHBaseQuery(request.getStartDate(), request.getEndDate());
        boolean isHBaseQueryRequired = !hBaseQueryDates.isEmpty();

        if (isHBaseQueryRequired) {
            List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = campaignCatalogMetricsRepository
                .get(Collections.singletonMap(request.getCampaignId(), request.getCatalogIds()), hBaseQueryDates);
            dateToHBaseMetricsMap = campaignCatalogDateMetricsList.stream().collect(Collectors.groupingBy(CampaignCatalogDateMetrics::getDate))
                .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> campaignPerformanceHelper.getAggregatedCampaignCatalogDateMetrics(entry.getValue())));
        }

        request.setEndDate(request.getEndDate().minusDays(hBaseQueryDates.size()));
        EsCampaignCatalogAggregateResponse dateWiseResponse = new EsCampaignCatalogAggregateResponse();
        boolean isESQueryRequired = !request.getEndDate().isBefore(request.getStartDate());

        if (isESQueryRequired) {
            ElasticFiltersRequest elasticFiltersRequestDateWise = ElasticFiltersRequest.builder()
                    .campaignIds(Collections.singletonList(request.getCampaignId()))
                    .aggregationBuilders(campaignPerformanceHelper.createGraphBucketAggregations(
                            Constants.ESConstants.BY_DATE, DBConstants.ElasticSearch.DATE,
                            (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1))
                    .build();

            elasticFiltersRequestDateWise.setRangeFilters(Collections.singletonList(ElasticFiltersRequest.RangeFilter
                    .builder().gte(request.getStartDate()).lte(request.getEndDate())
                    .fieldName(DBConstants.ElasticSearch.DATE).format(Constants.ESConstants.DAY_DATE_FORMAT).build()));

            dateWiseResponse = elasticSearchRepository.fetchEsCampaignCatalogsDateWise(elasticFiltersRequestDateWise);
        }

        return campaignPerformanceTransformer.getCampaignPerformanceDateWiseResponse(request.getCampaignId(),
                dateWiseResponse, dateToHBaseMetricsMap);
    }
}
