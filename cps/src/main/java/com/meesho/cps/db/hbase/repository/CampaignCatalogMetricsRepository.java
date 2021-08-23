package com.meesho.cps.db.hbase.repository;

import com.meesho.ads.lib.exception.HbaseException;
import com.meesho.ads.lib.utils.HbaseUtils;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;

import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Slf4j
@Repository
public class CampaignCatalogMetricsRepository {

    private static final Long MULTIPLIER = 100000L;

    private static final String TABLE_NAME = "campaign_catalog_metrics";

    private static final byte[] COLUMN_FAMILY = Bytes.toBytes("cf");

    private static final byte[] COLUMN_WEIGHTED_CLICK_COUNT = Bytes.toBytes("weighted_click_count");

    private static final byte[] COLUMN_WEIGHTED_SHARES_COUNT = Bytes.toBytes("weighted_shares_count");

    private static final byte[] COLUMN_WEIGHTED_WISHLIST_COUNT = Bytes.toBytes("weighted_wishlist_count");

    private static final byte[] COLUMN_ORIGIN_WISE_CLICK_COUNT = Bytes.toBytes("origin_wise_click_count");

    private static final byte[] COLUMN_CAMPAIGN_ID = Bytes.toBytes("campaign_id");

    private static final byte[] COLUMN_CATALOG_ID = Bytes.toBytes("catalog_id");

    private static final byte[] COLUMN_COUNTRY_CODE = Bytes.toBytes("iso_country_code");

    private static final byte[] COLUMN_VIEW_COUNT = Bytes.toBytes("view_count");

    private static final byte[] COLUMN_BUDGET_UTILISED = Bytes.toBytes("budget_utilised");

    @Autowired
    Connection connection;

    private Table getTable() {
        try {
            TableName tableName = TableName.valueOf(DBConstants.HBase.NAMESPACE, TABLE_NAME);
            return connection.getTable(tableName);
        } catch (IOException e) {
            log.error("Exception while creating Hbase table instance for {}", TABLE_NAME, e);
            throw new HbaseException(e.getMessage());
        }
    }

    private CampaignCatalogMetrics mapper(Result result) {
        CampaignCatalogMetrics campaignCatalogMetrics = new CampaignCatalogMetrics();
        campaignCatalogMetrics.setCampaignId(HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_CAMPAIGN_ID, result));
        campaignCatalogMetrics.setCatalogId(HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_CATALOG_ID, result));
        campaignCatalogMetrics.setWeightedClickCount(
                HbaseUtils.getColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_WEIGHTED_CLICK_COUNT, result));
        campaignCatalogMetrics.setWeightedSharesCount(
                HbaseUtils.getColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_WEIGHTED_SHARES_COUNT, result));
        campaignCatalogMetrics.setWeightedWishlistCount(
                HbaseUtils.getColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_WEIGHTED_WISHLIST_COUNT, result));
        campaignCatalogMetrics.setOriginWiseClickCount(
                HbaseUtils.getColumnAsObject(COLUMN_FAMILY, COLUMN_ORIGIN_WISE_CLICK_COUNT, result,
                        HbaseUtils.STRING_LONG_MAP_TYPE));
        campaignCatalogMetrics.setCountry(
                HbaseUtils.getColumnAsObject(COLUMN_FAMILY, COLUMN_COUNTRY_CODE, result, HbaseUtils.COUNTRY_TYPE));
        campaignCatalogMetrics.setViewCount(HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_VIEW_COUNT, result));
        campaignCatalogMetrics.setBudgetUtilised(
                HbaseUtils.getLongColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED, result, MULTIPLIER));
        return campaignCatalogMetrics;
    }

    public CampaignCatalogMetrics get(Long campaignId, Long catalogId) {
        Get get = new Get(Bytes.toBytes(CampaignCatalogMetrics.generateRowKey(campaignId, catalogId)));
        try (Table table = getTable()) {
            Result result = table.get(get);
            if (result.isEmpty())
                return null;
            return mapper(result);
        } catch (IOException e) {
            throw new HbaseException(e.getMessage());
        }
    }

    private Put mapper(CampaignCatalogMetrics campaignCatalogMetrics) {
        Put put = new Put(Bytes.toBytes(campaignCatalogMetrics.getRowKey()));
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CAMPAIGN_ID, campaignCatalogMetrics.getCampaignId(), put);
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CATALOG_ID, campaignCatalogMetrics.getCatalogId(), put);
        HbaseUtils.addBigDecimalColumn(COLUMN_FAMILY, COLUMN_WEIGHTED_CLICK_COUNT,
                campaignCatalogMetrics.getWeightedClickCount(), put);
        HbaseUtils.addBigDecimalColumn(COLUMN_FAMILY, COLUMN_WEIGHTED_SHARES_COUNT,
                campaignCatalogMetrics.getWeightedSharesCount(), put);
        HbaseUtils.addBigDecimalColumn(COLUMN_FAMILY, COLUMN_WEIGHTED_WISHLIST_COUNT,
                campaignCatalogMetrics.getWeightedWishlistCount(), put);
        HbaseUtils.addObjectColumn(COLUMN_FAMILY, COLUMN_ORIGIN_WISE_CLICK_COUNT,
                campaignCatalogMetrics.getOriginWiseClickCount(), put);
        HbaseUtils.addObjectColumn(COLUMN_FAMILY, COLUMN_COUNTRY_CODE, campaignCatalogMetrics.getCountry(), put);
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_VIEW_COUNT, campaignCatalogMetrics.getViewCount(), put);
        HbaseUtils.addBigDecimalAsLongColumn(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED,
                campaignCatalogMetrics.getBudgetUtilised(), MULTIPLIER, put);
        return put;
    }

    public void put(CampaignCatalogMetrics campaignCatalogMetrics) {
        Put put = mapper(campaignCatalogMetrics);
        try (Table table = getTable()) {
            table.put(put);
        } catch (IOException e) {
            log.error("IOException in put CampaignCatalogMetrics", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void putAll(List<CampaignCatalogMetrics> campaignCatalogMetricsList) {
        List<Put> puts = new ArrayList<>();
        for (CampaignCatalogMetrics campaignCatalogMetrics : campaignCatalogMetricsList) {
            puts.add(mapper(campaignCatalogMetrics));
        }
        try (Table table = getTable()) {
            table.put(puts);
        } catch (IOException e) {
            log.error("IOException in put CampaignCatalogMetrics", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void incrementViewCount(Long campaignId, Long catalogId) {
        try (Table table = getTable()) {
            table.incrementColumnValue(Bytes.toBytes(CampaignCatalogMetrics.generateRowKey(campaignId, catalogId)),
                    COLUMN_FAMILY, COLUMN_VIEW_COUNT, 1l);
        } catch (IOException e) {
            log.error("Error in incrementing view count for campaignId {} and catalog {}", campaignId, catalogId, e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void incrementBudgetUtilised(Long campaignId, Long catalogId, BigDecimal interactionMultiplier) {
        try (Table table = getTable()) {
            table.incrementColumnValue(Bytes.toBytes(CampaignCatalogMetrics.generateRowKey(campaignId, catalogId)),
                    COLUMN_FAMILY, COLUMN_BUDGET_UTILISED,
                    interactionMultiplier.multiply(BigDecimal.valueOf(MULTIPLIER)).longValue());
        } catch (IOException e) {
            log.error("Error in incrementing budget utilised for campaignId {} and catalogId {}", campaignId, catalogId,
                    e);
            throw new HbaseException(e.getMessage());
        }
    }

    public List<CampaignCatalogMetrics> getAll(List<Pair<Long, Long>> campaignCatalogIds) {
        try (Table table = getTable()) {
            List<Get> gets = campaignCatalogIds.stream()
                    .map(pair -> new Get(
                            Bytes.toBytes(CampaignCatalogMetrics.generateRowKey(pair.getFirst(), pair.getSecond()))))
                    .collect(Collectors.toList());
            Result[] results = table.get(gets);
            return Arrays.stream(results)
                    .filter(result -> !result.isEmpty())
                    .map(this::mapper)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error in CampaignCatalogMetricsRepository getAll", e);
            throw new HbaseException(e.getMessage());
        }
    }

}
