package com.meesho.cps.service;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.constants.SortType;
import com.meesho.cps.data.entity.elasticsearch.EsCampaignCatalogAggregateResponse;
import com.meesho.cps.data.entity.elasticsearch.internal.SortConfig;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import com.meesho.cps.data.internal.FetchCampaignCatalogsForDateRequest;
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
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
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
                .aggregationBuilders(campaignPerformanceHelper.createBucketAggregations(
                        Constants.ESConstants.BY_CAMPAIGN, DBConstants.ElasticSearch.CAMPAIGN_ID,
                        request.getCampaignIds().size()))
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
                .aggregationBuilders(campaignPerformanceHelper.createBucketAggregations(
                        Constants.ESConstants.BY_CATALOG, DBConstants.ElasticSearch.CATALOG_ID,
                        request.getCatalogIds().size()))
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

    public FetchCampaignsForDateResponse getCampaignsForDate(FetchCampaignsForDateRequest request) throws IOException {

        String cursor = campaignPerformanceHelper.decodeCursor(request.getCursor());

        Pair<String, String> campaignDateKVPair = Pair.of(Constants.ESFieldNames.CAMPAIGN_DATE, request.getDate());
        List<String> includeFields = Arrays.asList(Constants.ESFieldNames.SUPPLIER_ID, Constants.ESFieldNames.CAMPAIGN_ID, Constants.ESFieldNames.CATALOG_ID);
        List<String> mustExistFields = Collections.singletonList(Constants.ESFieldNames.BUDGET_UTILISED);

        Object[] searchAfterValues = null;
        if(Objects.nonNull(cursor)) {
            searchAfterValues = new Object[]{cursor};
        }

        FetchCampaignCatalogsForDateRequest fetchCampaignCatalogsForDateRequest = FetchCampaignCatalogsForDateRequest.builder()
                .orderedListOfSortConfigs(Collections.singletonList(SortConfig.builder().type(SortType.FIELD).fieldName(Constants.ESFieldNames.ID).order(SortOrder.ASC).build()))
                .mustMatchKeyValuePairs(Collections.singletonList(campaignDateKVPair))
                .limit(campaignPerformanceHelper.getCampaignCatalogLimit(request.getLimit()))
                .searchAfterValues(searchAfterValues)
                .includeFields(includeFields)
                .mustExistFields(mustExistFields)
                .build();

        SearchResponse searchResponse = elasticSearchRepository.fetchEsCampaignCatalogsForDate(fetchCampaignCatalogsForDateRequest);
        log.info("Query Response: " + searchResponse);

        return campaignPerformanceTransformer.getFetchCampaignsForDateResponse(searchResponse);

    }

}
