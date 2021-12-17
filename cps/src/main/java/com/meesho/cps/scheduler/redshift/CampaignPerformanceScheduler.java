package com.meesho.cps.scheduler.redshift;

import com.meesho.ads.lib.data.internal.RedshiftProcessedMetadata;
import com.meesho.ads.lib.scheduler.RedshiftAbstractScheduler;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.data.redshift.CampaignPerformanceRedshift;
import com.meesho.cps.service.redshift.CampaignPerformanceHandler;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
@Slf4j
@Component
@EnableScheduling
public class CampaignPerformanceScheduler extends RedshiftAbstractScheduler<CampaignPerformanceRedshift> {

    private static final String QUERY = "SELECT * FROM " + DBConstants.Redshift.Tables.CAMPAIGN_PERFORMANCE_METRICS +
            " where created_at > '%s' AND dt >= '2021-12-18' order by created_at LIMIT '%d' OFFSET '%d'";

    @Autowired
    private CampaignPerformanceHandler campaignPerformanceHandler;

    @Override
    public String getType() {
        return SchedulerType.CAMPAIGN_PERFORMANCE.name();
    }

    @Override
    public String getSchedulerKey(CampaignPerformanceRedshift campaignPerformanceRedshift) {
        return campaignPerformanceHandler.getUniqueKey(campaignPerformanceRedshift);
    }

    @Override
    public String getQuery(String startTime, long offset, int limit) {
        return String.format(QUERY, startTime, limit, offset);
    }

    @Override
    public RedshiftProcessedMetadata<CampaignPerformanceRedshift> transformResults(ResultSet resultSet) throws SQLException {
        return campaignPerformanceHandler.transformResults(resultSet);
    }

    @Override
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "CampaignPerformanceScheduler")
    public void handle(List<CampaignPerformanceRedshift> entities) {
        campaignPerformanceHandler.handle(entities);
    }

}
