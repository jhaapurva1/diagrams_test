package com.meesho.cps.db.hbase.repository;

import com.meesho.ads.lib.exception.HbaseException;
import com.meesho.ads.lib.utils.HbaseUtils;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;

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
public class CampaignMetricsRepository {

    private static final Long MULTIPLIER = 100000L;
    private static final String TABLE_NAME = "campaign_metrics";
    private static final byte[] COLUMN_FAMILY = Bytes.toBytes("cf");
    private static final byte[] COLUMN_CAMPAIGN_ID = Bytes.toBytes("campaign_id");
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

    private CampaignMetrics mapper(Result result) {
        CampaignMetrics campaignMetrics = new CampaignMetrics();
        campaignMetrics.setCampaignId(HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_CAMPAIGN_ID, result));
        campaignMetrics.setBudgetUtilised(
                HbaseUtils.getLongColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED, result, MULTIPLIER));
        return campaignMetrics;
    }

    public CampaignMetrics get(Long campaignId) {
        Get get = new Get(Bytes.toBytes(CampaignMetrics.generateRowKey(campaignId)));
        try (Table table = getTable()) {
            Result result = table.get(get);
            if (result.isEmpty())
                return null;
            return mapper(result);
        } catch (IOException e) {
            throw new HbaseException(e.getMessage());
        }
    }

    public List<CampaignMetrics> getAll(List<Long> campaignIds) {
        if (CollectionUtils.isEmpty(campaignIds))
            return new ArrayList<>();
        List<CampaignMetrics> campaignMetrics = new ArrayList<>();
        List<Get> gets = new ArrayList<>();
        for (Long campaignId : campaignIds) {
            Get get = new Get(Bytes.toBytes(CampaignMetrics.generateRowKey(campaignId)));
            gets.add(get);
        }
        try (Table table = getTable()) {
            Result[] results = table.get(gets);
            if (results.length == 0)
                return null;
            for (Result result : results) {
                campaignMetrics.add(mapper(result));
            }
            return campaignMetrics;
        } catch (IOException e) {
            throw new HbaseException(e.getMessage());
        }
    }

    public void put(CampaignMetrics campaignMetrics) {
        Put put = new Put(Bytes.toBytes(campaignMetrics.getRowKey()));
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CAMPAIGN_ID, campaignMetrics.getCampaignId(), put);
        HbaseUtils.addBigDecimalAsLongColumn(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED, campaignMetrics.getBudgetUtilised(),
                MULTIPLIER, put);

        try (Table table = getTable()) {
            table.put(put);
        } catch (IOException e) {
            log.error("IOException in put CampaignMetricsRepository", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public BigDecimal incrementBudgetUtilised(Long campaignId, BigDecimal interactionMultiplier) {
        try (Table table = getTable()) {
            long value =
                    table.incrementColumnValue(Bytes.toBytes(CampaignMetrics.generateRowKey(campaignId)), COLUMN_FAMILY,
                            COLUMN_BUDGET_UTILISED,
                            interactionMultiplier.multiply(BigDecimal.valueOf(MULTIPLIER)).longValue());
            return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(MULTIPLIER));
        } catch (IOException e) {
            log.error("Error in incrementing budget utilised for campaignId {}", campaignId);
            throw new HbaseException(e.getMessage());
        }
    }

}
