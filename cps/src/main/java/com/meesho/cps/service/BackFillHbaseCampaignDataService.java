package com.meesho.cps.service;

import com.google.common.collect.Lists;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.data.presto.CampaignCatalogReconciledMetricsPrestoData;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.transformer.DebugTransformer;
import com.meesho.cpsclient.request.HbaseCampaignDataBackfillRequest;
import com.meesho.prism.beans.PrismSortOrder;
import com.meesho.prism.proxy.beans.EngineResponse;
import com.meesho.prism.sdk.FetchType;
import com.meesho.prism.sdk.PrismDW;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class BackFillHbaseCampaignDataService {

    @Autowired
    CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Autowired
    CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Autowired
    CampaignMetricsRepository campaignMetricsRepository;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    String PRESTO_TABLE_NAME = Constants.PrismEventNames.HBASE_PRESTO_TABLE_NAME;

    public static Map<String, Object> processDetails = new HashMap<>();

    public Map<String, Object> backFill(HbaseCampaignDataBackfillRequest hbaseCampaignDataBackfillRequest) {
        return process(hbaseCampaignDataBackfillRequest);
    }


    private Map<String, Object> process(HbaseCampaignDataBackfillRequest hbaseCampaignDataBackfillRequest) {
        processDetails.put("Campaign Catalog Date Metrics Updation Status", "INPROGRESS");
        int offset = 0;
        LocalDate eventDate = LocalDate.parse(hbaseCampaignDataBackfillRequest.getEventDate());

        try {
            Integer batchLimit = hbaseCampaignDataBackfillRequest.getBatchSize();
            List<CampaignCatalogReconciledMetricsPrestoData> campaignCatalogReconciledMetricsPrestoDataList = getFeedFromSource(hbaseCampaignDataBackfillRequest.getEventDate(), hbaseCampaignDataBackfillRequest.getDumpId(), batchLimit, offset);
            if (CollectionUtils.isEmpty(campaignCatalogReconciledMetricsPrestoDataList)) {
                log.info("{} : backFill Completed", offset);
                processDetails.put("Status", "COMPLETED- no data");
                return processDetails;
            }

            int totalProcessed = 0;
            Map<Long, String> campaignIdtoCampaignTypeMap = new HashMap<>();
            Map<Long, Double> campaignIdtoCampaignBudgetUtilizedMap = new HashMap<>();
            Set<Long> nullCampaignType = new HashSet<>();
            while (!campaignCatalogReconciledMetricsPrestoDataList.isEmpty()) {

                List<List<CampaignCatalogReconciledMetricsPrestoData>> partitionedList = Lists.partition(campaignCatalogReconciledMetricsPrestoDataList, Constants.API.HBASE_BATCH_SIZE);
                for (List<CampaignCatalogReconciledMetricsPrestoData> campaignCatalogReconciledMetricsPrestoDataPartitionedList : partitionedList) {
                    List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = new ArrayList<>();
                    List<CampaignCatalogDate> campaignCatalogDates = new ArrayList<>();
                    for (CampaignCatalogReconciledMetricsPrestoData campaignCatalogReconciledMetricsPrestoData : campaignCatalogReconciledMetricsPrestoDataPartitionedList) {
                        CampaignCatalogDateMetrics campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
                        if(hbaseCampaignDataBackfillRequest.getBackfillCampaignCatalogDateMetrics()) {
                            campaignCatalogDateMetrics = campaignCatalogDateMetricsRepository.get(campaignCatalogReconciledMetricsPrestoData.getCampaignId(),
                                    campaignCatalogReconciledMetricsPrestoData.getCatalogId(), LocalDate.parse(campaignCatalogReconciledMetricsPrestoData.getEventDate()));
                            campaignCatalogDateMetrics = DebugTransformer.convertCampaignCatalogMetricsFromCampaignCatalogPrestoMetrics(campaignCatalogReconciledMetricsPrestoData, campaignCatalogDateMetrics);
                            campaignCatalogDateMetricsList.add(campaignCatalogDateMetrics);
                        }
                        if(Objects.nonNull(campaignCatalogReconciledMetricsPrestoData.getCampaignType())) {
                            campaignIdtoCampaignTypeMap.put(campaignCatalogReconciledMetricsPrestoData.getCampaignId(), campaignCatalogReconciledMetricsPrestoData.getCampaignType());
                        }else {
                            nullCampaignType.add(campaignCatalogReconciledMetricsPrestoData.getCampaignId());
                        }
                        if (campaignIdtoCampaignBudgetUtilizedMap.containsKey(campaignCatalogReconciledMetricsPrestoData.getCampaignId())) {
                            double budgetUtilized = campaignIdtoCampaignBudgetUtilizedMap.get(campaignCatalogReconciledMetricsPrestoData.getCampaignId());
                            budgetUtilized = budgetUtilized + campaignCatalogReconciledMetricsPrestoData.getBudgetUtilized();
                            campaignIdtoCampaignBudgetUtilizedMap.put(campaignCatalogReconciledMetricsPrestoData.getCampaignId(), budgetUtilized);
                        } else {
                            campaignIdtoCampaignBudgetUtilizedMap.put(campaignCatalogReconciledMetricsPrestoData.getCampaignId(), campaignCatalogReconciledMetricsPrestoData.getBudgetUtilized());
                        }
                        if(hbaseCampaignDataBackfillRequest.getBackfillCampaignCatalogDateMetrics()) {
                            CampaignCatalogDate campaignCatalogDate = new CampaignCatalogDate();
                            campaignCatalogDate.setCampaignId(campaignCatalogDateMetrics.getCampaignId());
                            campaignCatalogDate.setCatalogId(campaignCatalogDateMetrics.getCatalogId());
                            campaignCatalogDate.setDate(campaignCatalogDateMetrics.getDate().toString());
                            campaignCatalogDates.add(campaignCatalogDate);
                        }
                    }
                    if(hbaseCampaignDataBackfillRequest.getBackfillCampaignCatalogDateMetrics()) {
                        campaignCatalogDateMetricsRepository.putAll(campaignCatalogDateMetricsList);
                        updatedCampaignCatalogCacheDao.add(campaignCatalogDates);
                    }
                }
                processDetails.put("lastProcessedOffset", offset);
                totalProcessed += campaignCatalogReconciledMetricsPrestoDataList.size();
                offset = totalProcessed;
                processDetails.put("totalProcessed", totalProcessed);
                campaignCatalogReconciledMetricsPrestoDataList = getFeedFromSource(hbaseCampaignDataBackfillRequest.getEventDate(), hbaseCampaignDataBackfillRequest.getDumpId(), batchLimit, offset);
            }
            processDetails.put("Campaign Catalog Date Metrics Updation Status", "COMPLETED");
            log.info("{} : Processing Campaign Catalog Date Metrics Completed : {}", offset, processDetails);
            nullCampaignType.removeAll(campaignIdtoCampaignTypeMap.keySet());
            processDetails.put("nullCampaignType", nullCampaignType.size());
            saveToCampaignDatewiseMetricsRepository(campaignIdtoCampaignTypeMap, campaignIdtoCampaignBudgetUtilizedMap, eventDate);
            saveToCampaignMetricsRepository(campaignIdtoCampaignTypeMap);
            return processDetails;
        } catch (Exception e) {
            processDetails.put("Error", e.getMessage());
            processDetails.put("lastProcessedOffset", offset);
            processDetails.put("Campaign Catalog Date Metrics Updation Status", "FAILED");
            log.error("ERROR Processing failed details : {}", processDetails, e);
            return processDetails;
        }
    }


    public List<CampaignCatalogReconciledMetricsPrestoData> getFeedFromSource(String eventDate, String dumpId, int batchLimit, int offset) {

        final LinkedHashMap<String, PrismSortOrder> sortOrderMap = new LinkedHashMap<>();
        sortOrderMap.put("dump_id", PrismSortOrder.ASCENDING);

        String filter = String.format("dump_id = '%s' and event_date = '%s'", dumpId, eventDate);

        PrismDW prismDW = PrismDW.getInstance();
        EngineResponse prismEngineResponse = prismDW.fetchOffset(PRESTO_TABLE_NAME,
                Collections.singletonList("*"), filter, null, null, sortOrderMap, batchLimit, offset, CampaignCatalogReconciledMetricsPrestoData.class, FetchType.JDBC);

        List<CampaignCatalogReconciledMetricsPrestoData> prismEngineResponseList = new ArrayList<>();
        while (prismEngineResponse.hasNext()) {
            List<CampaignCatalogReconciledMetricsPrestoData> responseList = prismEngineResponse.extractData();
            prismEngineResponseList.addAll(responseList);
            prismEngineResponse = prismEngineResponse.next();
        }
        log.info("{} : getFeedFromSource : fetched  records: {}", offset, prismEngineResponseList.size());
        return prismEngineResponseList;

    }


    public void saveToCampaignDatewiseMetricsRepository(Map<Long, String> campaignIdtoCampaignTypeMap, Map<Long, Double> campaignIdtoCampaignBudgetUtilizedMap, LocalDate eventDate) {
        processDetails.put("Campaign Datewise Metrics Updation Status", "IN PROGRESS");
        try {
            for (Map.Entry<Long, String> mapElement : campaignIdtoCampaignTypeMap.entrySet()) {
                    if (CampaignType.DAILY_BUDGET.getValue().equals(mapElement.getValue())) {
                        CampaignDatewiseMetrics campaignDateMetrics = campaignDatewiseMetricsRepository.get(mapElement.getKey(), eventDate);
                        if (Objects.isNull(campaignDateMetrics)) {
                            campaignDateMetrics = new CampaignDatewiseMetrics();
                            campaignDateMetrics.setCampaignId(mapElement.getKey());
                            campaignDateMetrics.setDate(eventDate);
                        }
                        campaignDateMetrics.setBudgetUtilised(BigDecimal.valueOf(campaignIdtoCampaignBudgetUtilizedMap.get(mapElement.getKey())));
                        campaignDatewiseMetricsRepository.put(campaignDateMetrics);
                    }
            }
            processDetails.put("Campaign Datewise Metrics Updation Status", "COMPLETED");
        } catch (Exception e) {
            processDetails.put("Error", e.getMessage());
            processDetails.put("Campaign Datewise Metrics Updation Status", "FAILED");
            log.error("ERROR Processing failed details : {}", processDetails, e);
        }
    }


    public void saveToCampaignMetricsRepository(Map<Long, String> campaignIdtoCampaignTypeMap) {
        processDetails.put("Campaign Metrics Updation Status", "IN PROGRESS");
        try {
            for (Map.Entry<Long, String> mapElement : campaignIdtoCampaignTypeMap.entrySet()) {
                if (CampaignType.TOTAL_BUDGET.getValue().equals(mapElement.getValue())) {
                    List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = campaignCatalogDateMetricsRepository.scanByCampaignId(mapElement.getKey());
                    BigDecimal budgetUtilized = BigDecimal.valueOf(0);
                    for (CampaignCatalogDateMetrics campaignCatalogDateMetrics : campaignCatalogDateMetricsList) {
                        if (Objects.nonNull(campaignCatalogDateMetrics.getBudgetUtilised())) {
                            budgetUtilized = budgetUtilized.add(campaignCatalogDateMetrics.getBudgetUtilised());
                        }
                    }
                    CampaignMetrics campaignMetrics = campaignMetricsRepository.get(mapElement.getKey());
                    if (Objects.isNull(campaignMetrics)) {
                        campaignMetrics = new CampaignMetrics();
                        campaignMetrics.setCampaignId(mapElement.getKey());
                    }
                    campaignMetrics.setBudgetUtilised(budgetUtilized);
                    campaignMetricsRepository.put(campaignMetrics);
                }
            }
            processDetails.put("Campaign Metrics Updation Status", "COMPLETED");
        } catch (Exception e) {
            processDetails.put("Error", e.getMessage());
            processDetails.put("Campaign Metrics Updation Status", "FAILED");
            log.error("ERROR Processing failed details : {}", processDetails, e);
        }
    }

}
