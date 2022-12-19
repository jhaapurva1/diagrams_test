package com.meesho.cps.db.hbase.repository;

import com.meesho.ads.lib.exception.HbaseException;
import com.meesho.ads.lib.utils.HbaseUtils;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Slf4j
@Repository
public class CampaignDatewiseMetricsRepository {

    private static final Long MULTIPLIER = 100000L;
    private static final String TABLE_NAME = "campaign_datewise_metrics";
    private static final byte[] COLUMN_FAMILY = Bytes.toBytes("cf");
    private static final byte[] COLUMN_CAMPAIGN_ID = Bytes.toBytes("campaign_id");
    private static final byte[] COLUMN_DATE = Bytes.toBytes("date");
    private static final byte[] COLUMN_BUDGET_UTILISED = Bytes.toBytes("budget_utilised");

    @Autowired
    private Connection connection;

    private Table getTable() {
        try {
            TableName tableName = TableName.valueOf(DBConstants.HBase.NAMESPACE, TABLE_NAME);
            return connection.getTable(tableName);
        } catch (IOException e) {
            log.error("Exception while creating Hbase table instance for {}", TABLE_NAME, e);
            throw new HbaseException(e.getMessage());
        }
    }

    private CampaignDatewiseMetrics mapper(Result result) {
        CampaignDatewiseMetrics campaignDatewiseMetrics = new CampaignDatewiseMetrics();
        campaignDatewiseMetrics.setCampaignId(HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_CAMPAIGN_ID, result));
        campaignDatewiseMetrics.setDate(HbaseUtils.getColumnAsLocalDate(COLUMN_FAMILY, COLUMN_DATE, result));
        campaignDatewiseMetrics.setBudgetUtilised(
                HbaseUtils.getLongColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED, result, MULTIPLIER));
        return campaignDatewiseMetrics;
    }

    public CampaignDatewiseMetrics get(Long campaignId, LocalDate date) {
        Get get = new Get(Bytes.toBytes(CampaignDatewiseMetrics.generateRowKey(campaignId, date)));
        try (Table table = getTable()) {
            Result result = table.get(get);
            if (result.isEmpty())
                return null;
            return mapper(result);
        } catch (IOException e) {
            throw new HbaseException(e.getMessage());
        }
    }

    public List<CampaignDatewiseMetrics> getAll(List<Long> campaignIds, LocalDate date) {
        if (CollectionUtils.isEmpty(campaignIds)) {
            return new ArrayList<>();
        }
        List<CampaignDatewiseMetrics> campaignDatewiseMetrics = new ArrayList<>();
        List<Get> gets = new ArrayList<>();
        for (Long campaignId : campaignIds) {
            Get get = new Get(Bytes.toBytes(CampaignDatewiseMetrics.generateRowKey(campaignId, date)));
            gets.add(get);
        }
        try (Table table = getTable()) {
            Result[] results = table.get(gets);
            if (results.length == 0)
                return null;
            for (Result result : results) {
                if (!result.isEmpty())
                    campaignDatewiseMetrics.add(mapper(result));
            }
            return campaignDatewiseMetrics;
        } catch (IOException e) {
            log.error("getAll exception ", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void put(CampaignDatewiseMetrics campaignDatewiseMetrics) {
        Put put = new Put(Bytes.toBytes(campaignDatewiseMetrics.getRowKey()));
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CAMPAIGN_ID, campaignDatewiseMetrics.getCampaignId(), put);
        HbaseUtils.addLocalDateColumn(COLUMN_FAMILY, COLUMN_DATE, campaignDatewiseMetrics.getDate(), put);
        HbaseUtils.addBigDecimalAsLongColumn(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED,
                campaignDatewiseMetrics.getBudgetUtilised(), MULTIPLIER, put);

        try (Table table = getTable()) {
            table.put(put);
        } catch (IOException e) {
            log.error("IOException in put CampaignDatewiseMetrics", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public BigDecimal incrementBudgetUtilised(Long campaignId, LocalDate date, BigDecimal interactionMultiplier) {
        try (Table table = getTable()) {
            long value =
                    table.incrementColumnValue(Bytes.toBytes(CampaignDatewiseMetrics.generateRowKey(campaignId, date)),
                            COLUMN_FAMILY, COLUMN_BUDGET_UTILISED,
                            interactionMultiplier.multiply(BigDecimal.valueOf(MULTIPLIER)).longValue());
            return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(MULTIPLIER));
        } catch (IOException e) {
            log.error("Error in incrementing budget utilised for campaignId {} at date {}", campaignId, date, e);
            throw new HbaseException(e.getMessage());
        }
    }

}
