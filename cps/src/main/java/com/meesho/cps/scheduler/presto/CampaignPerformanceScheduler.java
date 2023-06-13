package com.meesho.cps.scheduler.presto;

import com.meesho.ads.lib.scheduler.PrestoFeedIngestionScheduler;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.data.presto.CampaignPerformancePrestoData;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.service.presto.CampaignPerformanceHandler;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.prism.beans.PrismSortOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
@Slf4j
@Component
public class CampaignPerformanceScheduler extends PrestoFeedIngestionScheduler<CampaignPerformancePrestoData> {

    @Autowired
    private CampaignPerformanceHandler campaignPerformanceHandler;

    @Override
    public String getType() {
        return SchedulerType.CAMPAIGN_PERFORMANCE_NEW.name();
    }

    @Override
    public String getSchedulerKey(CampaignPerformancePrestoData prestoData) {
        return prestoData.getCampaignId() + "_" + prestoData.getCatalogId() + "_" + prestoData.getDate();
    }

    @Override
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "className=campaignPerformanceScheduler")
    public void handle(List<CampaignPerformancePrestoData> campaignPerformancePrestoDataList) {
        try {
            campaignPerformanceHandler.handle(campaignPerformancePrestoDataList);
        } catch (ExternalRequestFailedException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public String getPrestoTableName() {
        return DBConstants.PrestoTables.CAMPAIGN_PERFORMANCE_METRICS;
    }

    @Override
    public Class<CampaignPerformancePrestoData> getClassType() {
        return CampaignPerformancePrestoData.class;
    }

    @Override
    public void putUniqueKeySortOrder(LinkedHashMap<String, PrismSortOrder> sortOrderMap) {
        sortOrderMap.put("dt", PrismSortOrder.ASCENDING);
        sortOrderMap.put("campaign_id", PrismSortOrder.ASCENDING);
        sortOrderMap.put("catalog_id", PrismSortOrder.ASCENDING);
    }

    @Override
    public Long process(int limit, ZonedDateTime startTime, int processBatchSize) throws Exception {
        return null;
    }
}
