package com.meesho.cps.service;

import com.google.common.collect.Lists;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.data.presto.CampaignCatalogReconciledMetricsPrestoData;
import com.meesho.cps.db.mongodb.dao.CampaignDateWiseMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignCatalogDateMetricsDao;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.transformer.DebugTransformer;
import com.meesho.cpsclient.request.CampaignDataBackFillRequest;
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
public class BackFillCampaignDataService {

    @Autowired
    CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Autowired
    CampaignDateWiseMetricsDao campaignDateWiseMetricsDao;

    @Autowired
    CampaignMetricsDao campaignMetricsDao;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private AdService adService;

    public static Map<String, Object> processDetails = new HashMap<>();

    public Map<String, Object> backFill(CampaignDataBackFillRequest campaignDataBackfillRequest) {
        return process(campaignDataBackfillRequest);
    }


    private Map<String, Object> process(CampaignDataBackFillRequest campaignDataBackfillRequest) {
        processDetails.put("Campaign Catalog Date Metrics Updation Status", "INPROGRESS");
        int offset = 0;
        LocalDate eventDate = LocalDate.parse(campaignDataBackfillRequest.getEventDate());

        try {
            Integer batchLimit = campaignDataBackfillRequest.getBatchSize();
            List<CampaignCatalogReconciledMetricsPrestoData> campaignCatalogReconciledMetricsPrestoDataList = getFeedFromSource(campaignDataBackfillRequest.getEventDate(), campaignDataBackfillRequest.getDumpId(), batchLimit, offset, campaignDataBackfillRequest.getPrestoTableName());
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
                        if(campaignDataBackfillRequest.getBackfillCampaignCatalogDateMetrics()) {
                            campaignCatalogDateMetrics = campaignCatalogDateMetricsDao.find(campaignCatalogReconciledMetricsPrestoData.getCampaignId(),
                                    campaignCatalogReconciledMetricsPrestoData.getCatalogId(), LocalDate.parse(campaignCatalogReconciledMetricsPrestoData.getEventDate()).toString());
                            Long supplierId = null;
                            List<CampaignDetails> campaignDetails = adService.getCampaignMetadata(Collections.singletonList(campaignCatalogReconciledMetricsPrestoData.getCampaignId()));
                            if (Objects.nonNull(campaignDetails) && !campaignDetails.isEmpty()) {
                                supplierId = campaignDetails.get(0).getSupplierId();
                            }
                            else {
                                log.error("Unable to get response from ads-admin. Could not back-fill data {}", campaignCatalogReconciledMetricsPrestoData);
                                continue;
                            }
                            campaignCatalogDateMetrics = DebugTransformer.convertCampaignCatalogMetricsFromCampaignCatalogPrestoMetrics(campaignCatalogReconciledMetricsPrestoData, campaignCatalogDateMetrics, supplierId);
                            campaignCatalogDateMetricsList.add(campaignCatalogDateMetrics);
                        }
                        if(Objects.nonNull(campaignCatalogReconciledMetricsPrestoData.getCampaignType())) {
                            campaignIdtoCampaignTypeMap.put(campaignCatalogReconciledMetricsPrestoData.getCampaignId(), campaignCatalogReconciledMetricsPrestoData.getCampaignType());
                        }else {
                            nullCampaignType.add(campaignCatalogReconciledMetricsPrestoData.getCampaignId());
                        }
                        if (campaignIdtoCampaignBudgetUtilizedMap.containsKey(campaignCatalogReconciledMetricsPrestoData.getCampaignId())) {
                            double budgetUtilized = campaignIdtoCampaignBudgetUtilizedMap.get(campaignCatalogReconciledMetricsPrestoData.getCampaignId());
                            budgetUtilized = budgetUtilized + campaignCatalogReconciledMetricsPrestoData.getBudgetUtilised();
                            campaignIdtoCampaignBudgetUtilizedMap.put(campaignCatalogReconciledMetricsPrestoData.getCampaignId(), budgetUtilized);
                        } else {
                            campaignIdtoCampaignBudgetUtilizedMap.put(campaignCatalogReconciledMetricsPrestoData.getCampaignId(), campaignCatalogReconciledMetricsPrestoData.getBudgetUtilised());
                        }
                        if(campaignDataBackfillRequest.getBackfillCampaignCatalogDateMetrics()) {
                            CampaignCatalogDate campaignCatalogDate = new CampaignCatalogDate();
                            campaignCatalogDate.setCampaignId(campaignCatalogDateMetrics.getCampaignId());
                            campaignCatalogDate.setCatalogId(campaignCatalogDateMetrics.getCatalogId());
                            campaignCatalogDate.setDate(campaignCatalogDateMetrics.getDate().toString());
                            campaignCatalogDates.add(campaignCatalogDate);
                        }
                    }
                    if(campaignDataBackfillRequest.getBackfillCampaignCatalogDateMetrics()) {
                        campaignCatalogDateMetricsDao.save(campaignCatalogDateMetricsList);
                        updatedCampaignCatalogCacheDao.add(campaignCatalogDates);
                    }
                }
                processDetails.put("lastProcessedOffset", offset);
                totalProcessed += campaignCatalogReconciledMetricsPrestoDataList.size();
                offset = totalProcessed;
                processDetails.put("totalProcessed", totalProcessed);
                campaignCatalogReconciledMetricsPrestoDataList = getFeedFromSource(campaignDataBackfillRequest.getEventDate(), campaignDataBackfillRequest.getDumpId(), batchLimit, offset, campaignDataBackfillRequest.getPrestoTableName());
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


    public List<CampaignCatalogReconciledMetricsPrestoData> getFeedFromSource(String eventDate, String dumpId, int batchLimit, int offset, String prestoTableName) {

        final LinkedHashMap<String, PrismSortOrder> sortOrderMap = new LinkedHashMap<>();
        sortOrderMap.put("dump_id", PrismSortOrder.ASCENDING);

        String filter = String.format("dump_id = '%s' and event_date = '%s'", dumpId, eventDate);

        PrismDW prismDW = PrismDW.getInstance();
        EngineResponse prismEngineResponse = prismDW.fetchOffset(prestoTableName,
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
                if (CampaignType.DAILY_BUDGET.getValue().equals(mapElement.getValue())
                        || CampaignType.SMART_CAMPAIGN.getValue().equals(mapElement.getValue())) {
                    CampaignDateWiseMetrics campaignDateWiseMetrics = campaignDateWiseMetricsDao.findByCampaignIdAndDate(mapElement.getKey(), eventDate.toString());
                    if (Objects.isNull(campaignDateWiseMetrics)) {
                        campaignDateWiseMetrics = new CampaignDateWiseMetrics();
                        campaignDateWiseMetrics.setCampaignId(mapElement.getKey());
                        campaignDateWiseMetrics.setDate(eventDate.toString());
                    }
                    campaignDateWiseMetrics.setBudgetUtilised(BigDecimal.valueOf(campaignIdtoCampaignBudgetUtilizedMap.get(mapElement.getKey())));
                    campaignDateWiseMetricsDao.save(campaignDateWiseMetrics);
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
                    List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = campaignCatalogDateMetricsDao.findAllByCampaignId(mapElement.getKey());
                    BigDecimal budgetUtilized = BigDecimal.valueOf(0);
                    for (CampaignCatalogDateMetrics campaignCatalogDateMetrics : campaignCatalogDateMetricsList) {
                        if (Objects.nonNull(campaignCatalogDateMetrics.getBudgetUtilised())) {
                            budgetUtilized = budgetUtilized.add(campaignCatalogDateMetrics.getBudgetUtilised());
                        }
                    }
                    CampaignMetrics campaignMetrics = campaignMetricsDao.findByCampaignId(mapElement.getKey());
                    if (Objects.isNull(campaignMetrics)) {
                        campaignMetrics = new CampaignMetrics();
                        campaignMetrics.setCampaignId(mapElement.getKey());
                    }
                    campaignMetrics.setBudgetUtilised(budgetUtilized);
                    campaignMetricsDao.save(campaignMetrics);
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
