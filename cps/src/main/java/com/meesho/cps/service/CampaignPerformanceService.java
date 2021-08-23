package com.meesho.cps.service;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.data.entity.mysql.projection.CampaignOverallPerformanceView;
import com.meesho.cps.data.entity.mysql.projection.SupplierOverallPerformanceView;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.mysql.dao.CampaignPerformanceDao;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cpsclient.request.*;
import com.meesho.cpsclient.response.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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
    private CampaignCatalogMetricsRepository campaignCatalogMetricsRepository;

    @Autowired
    private CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Autowired
    private CampaignPerformanceTransformer campaignPerformanceTransformer;

    @Autowired
    private CampaignPerformanceHelper campaignPerformanceHelper;


    public SupplierPerformanceResponse getSupplierPerformanceMetrics(SupplierPerformanceRequest request)
            throws Exception {
        SupplierOverallPerformanceView supplierOverallPerformanceView =
                campaignPerformanceDao.getOverallPerformanceForSupplier(request.getSupplierId());

        return campaignPerformanceTransformer.getSupplierPerformanceResponse(supplierOverallPerformanceView);
    }

    public CampaignPerformanceResponse getCampaignPerformanceMetrics(CampaignPerformanceRequest request)
            throws Exception {
        List<CampaignOverallPerformanceView> campaignOverallPerformanceViewList =
                campaignPerformanceDao.getOverallPerformanceForCampaigns(request.getCampaignIds());
        return campaignPerformanceTransformer.getCampaignPerformanceResponse(campaignOverallPerformanceViewList);
    }

    public CampaignCatalogPerformanceResponse getCampaignCatalogPerformanceMetrics(
            CampaignCatalogPerformanceRequest request) throws Exception {
        log.info("request : {}", request);
        List<CampaignPerformance> campaignPerformanceList =
                campaignPerformanceDao.findAllByCatalogIdsAndCampaignId(request.getCatalogIds(),
                        request.getCampaignId());
        log.info("campaignPerformanceList: {}", campaignPerformanceList);
        return campaignPerformanceTransformer.getCampaignCatalogPerformanceResponse(campaignPerformanceList);
    }

    public BudgetUtilisedResponse getBudgetUtilised(BudgetUtilisedRequest request) throws Exception {
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

}
