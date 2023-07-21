package com.meesho.cps.service;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.mongodb.collection.SupplierWeekWiseMetrics;
import com.meesho.cps.data.entity.mongodb.projection.CampaignCatalogLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.DateLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.CampaignLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.SupplierLevelMetrics;
import com.meesho.cps.db.mongodb.dao.CampaignDateWiseMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignCatalogDateMetricsDao;
import com.meesho.cps.db.mongodb.dao.SupplierWeekWiseMetricsDao;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cpsclient.request.*;
import com.meesho.cpsclient.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.meesho.cps.utils.DateTimeHelper.MONGO_DATE_FORMAT;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Slf4j
@Service
public class CampaignPerformanceService {

    @Autowired
    private CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Autowired
    private CampaignMetricsDao campaignMetricsDao;

    @Autowired
    private CampaignPerformanceTransformer campaignPerformanceTransformer;

    @Autowired
    private CampaignPerformanceHelper campaignPerformanceHelper;

    @Autowired
    private CampaignDateWiseMetricsDao campaignDateWiseMetricsDao;

    @Autowired
    private SupplierWeekWiseMetricsDao supplierWeekWiseMetricsDao;


    @Autowired
    private ApplicationProperties applicationProperties;

    public SupplierPerformanceResponse getSupplierPerformanceMetrics(SupplierPerformanceRequest request) {

        String startDate;
        String endDate;
        if (!request.isDateRangePresent()) {
            startDate  = applicationProperties.getCampaignDatewiseMetricsReferenceDate().format(DateTimeFormatter.ofPattern(MONGO_DATE_FORMAT));
            endDate = LocalDate.now().format(DateTimeFormatter.ofPattern(MONGO_DATE_FORMAT));
        } else {
            startDate = request.getStartDate().toString();
            endDate = request.getEndDate().toString();
        }

        List<SupplierLevelMetrics> supplierLevelMetrics = campaignCatalogDateMetricsDao.getSupplierLevelMetrics(request.getSupplierId(), startDate, endDate);

        if (!supplierLevelMetrics.isEmpty()) {
            return campaignPerformanceTransformer.getSupplierPerformanceResponse(supplierLevelMetrics.get(0));
        } else {
            return null;
        }
    }

    public CampaignPerformanceResponse getCampaignPerformanceMetrics(CampaignPerformanceRequest request) {
        String startDate;
        String endDate;
        if (!request.isDateRangePresent()) {
            startDate  = applicationProperties.getCampaignDatewiseMetricsReferenceDate().format(DateTimeFormatter.ofPattern(MONGO_DATE_FORMAT));
            endDate = LocalDate.now().format(DateTimeFormatter.ofPattern(MONGO_DATE_FORMAT));
        } else {
            startDate = request.getStartDate().toString();
            endDate = request.getEndDate().toString();
        }

        List<Long> campaignIds = request.getCampaignDetails().stream().map(CampaignPerformanceRequest.CampaignDetails::getCampaignId).collect(Collectors.toList());

        List<CampaignLevelMetrics> campaignLevelMetrics = campaignCatalogDateMetricsDao.getCampaignLevelMetrics(campaignIds, startDate, endDate);
        return campaignPerformanceTransformer.getCampaignPerformanceResponse(campaignLevelMetrics);
    }

    public CampaignCatalogPerformanceResponse getCampaignCatalogPerformanceMetrics(CampaignCatalogPerformanceRequest request) throws IOException {
        String startDate;
        String endDate;
        if (!request.isDateRangePresent()) {
            startDate  = applicationProperties.getCampaignDatewiseMetricsReferenceDate().format(DateTimeFormatter.ofPattern(MONGO_DATE_FORMAT));
            endDate = LocalDate.now().format(DateTimeFormatter.ofPattern(MONGO_DATE_FORMAT));
        } else {
            startDate = request.getStartDate().toString();
            endDate = request.getEndDate().toString();
        }

        List<CampaignCatalogLevelMetrics> campaignCatalogLevelMetricsList = campaignCatalogDateMetricsDao.getCampaignCatalogLevelMetrics(request.getCampaignId(), request.getCatalogIds(), startDate, endDate);

        return campaignPerformanceTransformer.getCampaignCatalogPerformanceResponse(campaignCatalogLevelMetricsList);
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
        dailyBudgetCampaignIds.addAll(campaignTypeAndCampaignIdsMap.getOrDefault(CampaignType.SMART_CAMPAIGN.getValue(), new ArrayList<>())
                .stream()
                .map(BudgetUtilisedRequest.CampaignData::getCampaignId)
                .collect(Collectors.toList()));

        List<Long> totalBudgetCampaignIds =
                campaignTypeAndCampaignIdsMap.getOrDefault(CampaignType.TOTAL_BUDGET.getValue(), new ArrayList<>())
                        .stream()
                        .map(BudgetUtilisedRequest.CampaignData::getCampaignId)
                        .collect(Collectors.toList());

        List<Long> supplierIds = request.getSuppliersIdList();
        LocalDate dailyBudgetTrackingDate = campaignPerformanceHelper.getLocalDateForDailyCampaignFromLocalDateTime(DateTimeUtils.getCurrentLocalDateTimeInIST());
        LocalDate weekStartDate = DateTimeUtils.getFirstDayOfWeek().toLocalDate();

        List<CampaignDateWiseMetrics> campaignDateWiseMetricsList = campaignDateWiseMetricsDao.findAllByCampaignIdsInAndDate(dailyBudgetCampaignIds, dailyBudgetTrackingDate.toString());
        List<CampaignMetrics> campaignMetricsList = campaignMetricsDao.findAllByCampaignIdsIn(totalBudgetCampaignIds);
        List<SupplierWeekWiseMetrics> supplierWeekWiseMetricsList = supplierWeekWiseMetricsDao.findAllBySupplierIdAndWeekStartDate(supplierIds, weekStartDate.toString());


        return campaignPerformanceTransformer.getBudgetUtilisedResponse(campaignMetricsList, campaignDateWiseMetricsList, supplierWeekWiseMetricsList);
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
                CampaignCatalogDateMetrics document = campaignCatalogDateMetricsDao.find(campaignId, catalogId, date.toString());
                if (Objects.nonNull(document) && Objects.nonNull(document.getBudgetUtilised()) && Objects.nonNull(document.getCatalogId())) {
                    CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails catalogDetailsResponse =
                            CampaignCatalogDateLevelBudgetUtilisedResponse.CampaignDetails.CatalogDetails.builder()
                                    .catalogId(document.getCatalogId()).budgetUtilised(document.getBudgetUtilised()).build();
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

    public FetchActiveCampaignsResponse getActiveCampaignsForDate(FetchActiveCampaignsRequest request) throws ParseException {

        String lastProcessedId = campaignPerformanceHelper.decodeCursor(request.getCursor());

        Integer limit = request.getLimit();
        if(Objects.isNull(limit)) {
            limit = Constants.FetchCampaignCatalog.DEFAULT_LIMIT;
        }

        List<CampaignCatalogDateMetrics> documentList = campaignCatalogDateMetricsDao.scrollForDate(request.getDate(), lastProcessedId, limit);

        return campaignPerformanceTransformer.getFetchActiveCampaignsResponse(documentList);

    }

    public CampaignPerformanceDatewiseResponse getCampaignCatalogPerfDateWise(CampaignCatalogPerfDatawiseRequest request) {

        List<DateLevelMetrics> dateLevelMetrics = campaignCatalogDateMetricsDao.getDateLevelMetrics(request.getCampaignId(), request.getStartDate().toString(), request.getEndDate().toString());

        return campaignPerformanceTransformer.getCampaignPerformanceDateWiseResponse(dateLevelMetrics, request.getCampaignId());
    }
}
