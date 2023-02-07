package com.meesho.cps.scheduler.redshift;

import com.meesho.ads.lib.scheduler.PrestoFeedIngestionScheduler;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.data.presto.AdsDeductionCampaignSupplierPrestoData;
import com.meesho.cps.service.redshift.AdsDeductionCampaignSupplierHandler;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import com.meesho.prism.beans.PrismSortOrder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
public class AdsDeductionCampaignSupplierScheduler extends PrestoFeedIngestionScheduler<AdsDeductionCampaignSupplierPrestoData> {

    private static final String QUERY = "SELECT * FROM " + DBConstants.Redshift.Tables.ADS_DEDUCTION_CAMPAIGN_SUPPLIER +
            "  where created_at > '%s' order by created_at LIMIT '%d' OFFSET '%d' ";

    @Autowired
    private AdsDeductionCampaignSupplierHandler adsDeductionCampaignSupplierHandler;

    @Override
    public String getPrestoTableName() {
        return DBConstants.PrestoTables.ADS_DEDUCTION_CAMPAIGN_SUPPLIER;
    }

    @Override
    public Class<AdsDeductionCampaignSupplierPrestoData> getClassType() {
        return AdsDeductionCampaignSupplierPrestoData.class;
    }

    @SneakyThrows
    @Override
    @DigestLogger(metricType = MetricType.METHOD, tagSet = "className=adsDeductionCampaignSupplierEventScheduler")
    public void handle(List<AdsDeductionCampaignSupplierPrestoData> entities) throws SQLException {
        adsDeductionCampaignSupplierHandler.handle(entities);
    }

    @Override
    public String getSchedulerKey(AdsDeductionCampaignSupplierPrestoData entity) {
        return adsDeductionCampaignSupplierHandler.getUniqueKey(entity);
    }

    @Override
    public String getType() {
        return SchedulerType.ADS_DEDUCTION_CAMPAIGN_SUPPLIER.name();
    }

    @Override
    public void putUniqueKeySortOrder(LinkedHashMap<String, PrismSortOrder> sortOrderMap) {
        sortOrderMap.put("transaction_id", PrismSortOrder.ASCENDING);
    }

    @Override
    public Long process(int limit, ZonedDateTime startTime, int processBatchSize) throws Exception {
        return null;
    }
}
