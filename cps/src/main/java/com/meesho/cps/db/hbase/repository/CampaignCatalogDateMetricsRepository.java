package com.meesho.cps.db.hbase.repository;

import com.meesho.ads.lib.exception.HbaseException;
import com.meesho.ads.lib.utils.HbaseUtils;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Slf4j
@Repository
public class CampaignCatalogDateMetricsRepository {

    private static final Long MULTIPLIER = 100000L;

    private static final String TABLE_NAME = "campaign_catalog_date_metrics";

    private static final byte[] COLUMN_FAMILY = Bytes.toBytes("cf");

    private static final byte[] COLUMN_CLICK_COUNT = Bytes.toBytes("click_count");

    private static final byte[] COLUMN_SHARES_COUNT = Bytes.toBytes("shares_count");

    private static final byte[] COLUMN_WISHLIST_COUNT = Bytes.toBytes("wishlist_count");

    //Only updated during orders and revenue update. Hence this column will not be present for all rows
    private static final byte[] COLUMN_CAMPAIGN_ID = Bytes.toBytes("campaign_id");

    //Only updated during orders and revenue update. Hence this column will not be present for all rows
    private static final byte[] COLUMN_CATALOG_ID = Bytes.toBytes("catalog_id");

    private static final byte[] COLUMN_VIEW_COUNT = Bytes.toBytes("view_count");

    private static final byte[] COLUMN_BUDGET_UTILISED = Bytes.toBytes("budget_utilised");

    private static final byte[] COLUMN_REVENUE = Bytes.toBytes("revenue");

    private static final byte[] COLUMN_ORDERS = Bytes.toBytes("orders");

    private static final byte[] COLUMN_DATE = Bytes.toBytes("date");

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

    private CampaignCatalogDateMetrics mapper(Result result, Long campaignId, Long catalogId) {
        String rowKey = Bytes.toString(result.getRow());
        LocalDate date = CampaignCatalogDateMetrics.getLocalDateFromRowKey(rowKey);

        CampaignCatalogDateMetrics campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
        campaignCatalogDateMetrics.setCampaignId(campaignId);
        campaignCatalogDateMetrics.setCatalogId(catalogId);
        campaignCatalogDateMetrics.setDate(date);
        campaignCatalogDateMetrics.setClickCount(HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_CLICK_COUNT, result));
        campaignCatalogDateMetrics.setSharesCount(
                HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_SHARES_COUNT, result));
        campaignCatalogDateMetrics.setWishlistCount(
                HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_WISHLIST_COUNT, result));
        campaignCatalogDateMetrics.setViewCount(HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_VIEW_COUNT, result));
        campaignCatalogDateMetrics.setBudgetUtilised(
                HbaseUtils.getLongColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED, result, MULTIPLIER));
        campaignCatalogDateMetrics.setOrders(HbaseUtils.getColumnAsInteger(COLUMN_FAMILY, COLUMN_ORDERS, result));
        campaignCatalogDateMetrics.setRevenue(HbaseUtils.getColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_REVENUE, result));
        return campaignCatalogDateMetrics;
    }

    public CampaignCatalogDateMetrics get(Long campaignId, Long catalogId, LocalDate date) {
        Get get = new Get(Bytes.toBytes(CampaignCatalogDateMetrics.generateRowKey(campaignId, catalogId, date)));
        try (Table table = getTable()) {
            Result result = table.get(get);
            if (result.isEmpty())
                return null;
            return mapper(result, campaignId, catalogId);
        } catch (IOException e) {
            throw new HbaseException(e.getMessage());
        }
    }

    private Put mapper(CampaignCatalogDateMetrics campaignCatalogDateMetrics) {
        Put put = new Put(Bytes.toBytes(campaignCatalogDateMetrics.getRowKey()));
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CAMPAIGN_ID, campaignCatalogDateMetrics.getCampaignId(), put);
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CATALOG_ID, campaignCatalogDateMetrics.getCatalogId(), put);
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CLICK_COUNT, campaignCatalogDateMetrics.getClickCount(), put);
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_SHARES_COUNT, campaignCatalogDateMetrics.getSharesCount(), put);
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_WISHLIST_COUNT,
                campaignCatalogDateMetrics.getWishlistCount(), put);
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_VIEW_COUNT, campaignCatalogDateMetrics.getViewCount(), put);
        HbaseUtils.addBigDecimalAsLongColumn(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED,
                campaignCatalogDateMetrics.getBudgetUtilised(), MULTIPLIER, put);
        HbaseUtils.addIntegerColumn(COLUMN_FAMILY, COLUMN_ORDERS, campaignCatalogDateMetrics.getOrders(), put);
        HbaseUtils.addBigDecimalColumn(COLUMN_FAMILY, COLUMN_REVENUE, campaignCatalogDateMetrics.getRevenue(), put);
        HbaseUtils.addLocalDateColumn(COLUMN_FAMILY, COLUMN_DATE, campaignCatalogDateMetrics.getDate(), put);
        return put;
    }

    private Put orderAndRevenueColumnMapper(CampaignCatalogDateMetrics campaignCatalogDateMetrics) {
        Put put = new Put(Bytes.toBytes(campaignCatalogDateMetrics.getRowKey()));
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CAMPAIGN_ID, campaignCatalogDateMetrics.getCampaignId(), put);
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_CATALOG_ID, campaignCatalogDateMetrics.getCatalogId(), put);
        HbaseUtils.addLocalDateColumn(COLUMN_FAMILY, COLUMN_DATE, campaignCatalogDateMetrics.getDate(), put);
        HbaseUtils.addIntegerColumn(COLUMN_FAMILY, COLUMN_ORDERS, campaignCatalogDateMetrics.getOrders(), put);
        HbaseUtils.addBigDecimalColumn(COLUMN_FAMILY, COLUMN_REVENUE, campaignCatalogDateMetrics.getRevenue(), put);
        return put;
    }

    public void put(CampaignCatalogDateMetrics campaignCatalogDateMetrics) {
        Put put = mapper(campaignCatalogDateMetrics);
        try (Table table = getTable()) {
            table.put(put);
        } catch (IOException e) {
            log.error("IOException in put CampaignCatalogDateMetrics", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void putAll(List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList) {
        List<Put> puts = new ArrayList<>();
        for (CampaignCatalogDateMetrics campaignCatalogDateMetrics : campaignCatalogDateMetricsList) {
            puts.add(mapper(campaignCatalogDateMetrics));
        }
        try (Table table = getTable()) {
            table.put(puts);
        } catch (IOException e) {
            log.error("IOException in put CampaignCatalogDateMetrics", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void putOrdersAndRevenueColumns(List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList) {
        List<Put> puts = new ArrayList<>();
        for (CampaignCatalogDateMetrics campaignCatalogDateMetrics : campaignCatalogDateMetricsList) {
            puts.add(orderAndRevenueColumnMapper(campaignCatalogDateMetrics));
        }
        try (Table table = getTable()) {
            table.put(puts);
        } catch (IOException e) {
            log.error("IOException in put CampaignCatalogDateMetrics", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void incrementSharesCount(Long campaignId, Long catalogId, LocalDate date) {
        try(Table table = getTable()) {
            table.incrementColumnValue(Bytes.toBytes(CampaignCatalogDateMetrics.generateRowKey(campaignId, catalogId,
                    date)), COLUMN_FAMILY, COLUMN_SHARES_COUNT, 1L);
        } catch (IOException e) {
            log.error("Error in incrementing shares count for campaignId {}, catalogId {}, date {}", campaignId,
                    catalogId, date);
            throw new HbaseException(e.getMessage());
        }
    }

    public void incrementWishlistCount(Long campaignId, Long catalogId, LocalDate date) {
        try(Table table = getTable()) {
            table.incrementColumnValue(Bytes.toBytes(CampaignCatalogDateMetrics.generateRowKey(campaignId, catalogId,
                    date)), COLUMN_FAMILY, COLUMN_WISHLIST_COUNT, 1L);
        } catch (IOException e) {
            log.error("Error in incrementing wishlist count for campaignId {}, catalogId {}, date {}", campaignId,
                    catalogId, date);
            throw new HbaseException(e.getMessage());
        }
    }

    public void incrementClickCount(Long campaignId, Long catalogId, LocalDate date) {
        try(Table table = getTable()) {
            table.incrementColumnValue(Bytes.toBytes(CampaignCatalogDateMetrics.generateRowKey(campaignId, catalogId,
                    date)), COLUMN_FAMILY, COLUMN_CLICK_COUNT, 1L);
        } catch (IOException e) {
            log.error("Error in incrementing clicks count for campaignId {}, catalogId {}, date {}", campaignId,
                    catalogId, date);
            throw new HbaseException(e.getMessage());
        }
    }

    public void incrementViewCount(Long campaignId, Long catalogId, LocalDate date) {
        try (Table table = getTable()) {
            table.incrementColumnValue(Bytes.toBytes(CampaignCatalogDateMetrics.generateRowKey(campaignId, catalogId,
                    date)), COLUMN_FAMILY, COLUMN_VIEW_COUNT, 1L);
        } catch (IOException e) {
            log.error("Error in incrementing view count for campaignId {} and catalog {}", campaignId, catalogId, e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void bulkIncrementViewCount(List<CampaignCatalogViewCount> campaignCatalogViewCountList) {

        final List<Increment> increments = campaignCatalogViewCountList.stream()
                .map(campaignCatalogViewCount -> {
                    Increment increment = new Increment(Bytes.toBytes(CampaignCatalogDateMetrics.generateRowKey(
                            campaignCatalogViewCount.getCampaignId(), campaignCatalogViewCount.getCatalogId(),
                            campaignCatalogViewCount.getDate())));
                    increment.addColumn(COLUMN_FAMILY, COLUMN_VIEW_COUNT,
                            Long.valueOf(campaignCatalogViewCount.getCount()));
                    return increment;
                }).collect(Collectors.toList());

        try (Table table = getTable()) {
            Object[] results = new Object[increments.size()];
            table.batch(increments, results);
            log.debug("bulkIncrementViewCount results {}", results);
        } catch (InterruptedException | IOException e) {
            log.error("Error in incrementing view count for " +
                    "campaignCatalogViewCountList {}", campaignCatalogViewCountList, e);
            throw new HbaseException(e.getMessage());
        }
    }

    public void incrementBudgetUtilised(Long campaignId, Long catalogId, LocalDate date, BigDecimal interactionMultiplier) {
        try (Table table = getTable()) {
            table.incrementColumnValue(Bytes.toBytes(CampaignCatalogDateMetrics.generateRowKey(campaignId, catalogId,
                    date)), COLUMN_FAMILY, COLUMN_BUDGET_UTILISED,
                    interactionMultiplier.multiply(BigDecimal.valueOf(MULTIPLIER)).longValue());
        } catch (IOException e) {
            log.error("Error in incrementing budget utilised for campaignId {} and catalogId {}", campaignId, catalogId,
                    e);
            throw new HbaseException(e.getMessage());
        }
    }

    public List<CampaignCatalogDateMetrics> scan(Long campaignId, Long catalogId, String datePrefix) {
        String rowKey = CampaignCatalogDateMetrics.generateRowKeyForMonthPrefix(campaignId, catalogId, datePrefix);
        try (Table table = getTable()) {
            Scan scan = new Scan();
            scan.setRowPrefixFilter(Bytes.toBytes(rowKey));
            ResultScanner resultScanner = table.getScanner(scan);
            List<CampaignCatalogDateMetrics> campaignCatalogMetricsList = new ArrayList<>();
            resultScanner.forEach(result -> {
                if(!result.isEmpty()){
                    campaignCatalogMetricsList.add(mapper(result, campaignId, catalogId));
                }
            });
            return campaignCatalogMetricsList;
        } catch (IOException e) {
            log.error("Error in CampaignCatalogDateMetricsRepository scan", e);
            throw new HbaseException(e.getMessage());
        }
    }

}
