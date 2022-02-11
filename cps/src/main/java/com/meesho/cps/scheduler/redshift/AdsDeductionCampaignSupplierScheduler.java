package com.meesho.cps.scheduler.redshift;

import com.meesho.ads.lib.data.internal.RedshiftProcessedMetadata;
import com.meesho.ads.lib.scheduler.RedshiftAbstractScheduler;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.data.redshift.AdsDeductionCampaignSupplier;
import com.meesho.cps.service.redshift.AdsDeductionCampaignSupplierHandler;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
public class AdsDeductionCampaignSupplierScheduler extends RedshiftAbstractScheduler<AdsDeductionCampaignSupplier> {

    private static final String QUERY = "SELECT * FROM " + DBConstants.Redshift.Tables.ADS_DEDUCTION_CAMPAIGN_SUPPLIER +
            "  where created_at > '%s' order by created_at LIMIT '%d' OFFSET '%d' ";

    @Autowired
    private AdsDeductionCampaignSupplierHandler adsDeductionCampaignSupplierHandler;

    @Override
    public String getQuery(String startTime, long offset, int limit) {
        return String.format(QUERY, startTime, limit, offset);
    }

    @Override
    public RedshiftProcessedMetadata<AdsDeductionCampaignSupplier> transformResults(ResultSet resultSet)
            throws SQLException {
        return adsDeductionCampaignSupplierHandler.transformResults(resultSet);
    }

    @Override
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "AdsDeductionCampaignSupplierEventScheduler")
    public void handle(List<AdsDeductionCampaignSupplier> entities) throws SQLException {
        adsDeductionCampaignSupplierHandler.handle(entities);
    }

    @Override
    public String getSchedulerKey(AdsDeductionCampaignSupplier entity) {
        return adsDeductionCampaignSupplierHandler.getUniqueKey(entity);
    }

    @Override
    public String getType() {
        return SchedulerType.ADS_DEDUCTION_CAMPAIGN_SUPPLIER.name();
    }
}
