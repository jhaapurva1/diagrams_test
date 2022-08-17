package com.meesho.cps.db.hbase.repository;

import com.meesho.ads.lib.exception.HbaseException;
import com.meesho.ads.lib.utils.HbaseUtils;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
public class SupplierWeekWiseMetricsRepository {

    private static final Long MULTIPLIER = 100000L;
    private static final String TABLE_NAME = "supplier_week_wise_performance_metrics";
    private static final byte[] COLUMN_FAMILY = Bytes.toBytes("cf");
    private static final byte[] COLUMN_SUPPLIER_ID = Bytes.toBytes("supplier_id");
    private static final byte[] COLUMN_BUDGET_UTILISED = Bytes.toBytes("budget_utilised");
    private static final byte[] COLUMN_WEEK_START_DATE = Bytes.toBytes("week_start_date");

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

    private SupplierWeekWiseMetrics mapper(Result result) {
        SupplierWeekWiseMetrics supplierWeekWiseMetrics = new SupplierWeekWiseMetrics();
        supplierWeekWiseMetrics.setSupplierId(HbaseUtils.getColumnAsLong(COLUMN_FAMILY, COLUMN_SUPPLIER_ID, result));
        supplierWeekWiseMetrics.setBudgetUtilised(HbaseUtils.getLongColumnAsBigDecimal(COLUMN_FAMILY,
                COLUMN_BUDGET_UTILISED, result, MULTIPLIER));
        supplierWeekWiseMetrics.setWeekStartDate(HbaseUtils.getColumnAsLocalDate(COLUMN_FAMILY, COLUMN_WEEK_START_DATE,
                result));
        return supplierWeekWiseMetrics;
    }


    public SupplierWeekWiseMetrics get(Long supplierId, LocalDate weekStartDate) {
        if (Objects.isNull(supplierId) || Objects.isNull(weekStartDate))
            return null;
        Get get = new Get(Bytes.toBytes(SupplierWeekWiseMetrics.generateRowKey(supplierId, weekStartDate)));
        try (Table table = getTable()) {
            Result result = table.get(get);
            if (result.isEmpty()) {
                log.info("Empty results found for supplierId {} and week-start-date {} in Hbase table {}", supplierId,
                        weekStartDate, table.getName());
                return null;
            }
            return mapper(result);
        } catch (IOException e) {
            throw new HbaseException(e.getMessage());
        }
    }

    public List<SupplierWeekWiseMetrics> getAll(List<Long> supplierIds, LocalDate weekStartDate) {
        if (CollectionUtils.isEmpty(supplierIds) || Objects.isNull(weekStartDate))
            return new ArrayList<>();
        List<SupplierWeekWiseMetrics> supplierWeekWiseMetrics = new ArrayList<>();
        List<Get> gets = new ArrayList<>();
        for (Long supplierId : supplierIds) {
            if (Objects.nonNull(supplierId)) {
                Get get = new Get(Bytes.toBytes(SupplierWeekWiseMetrics.generateRowKey(supplierId, weekStartDate)));
                gets.add(get);
            }
        }
        try (Table table = getTable()) {
            Result[] results = table.get(gets);
            if (results.length == 0)
                return new ArrayList<>();
            for (Result result : results) {
                supplierWeekWiseMetrics.add(mapper(result));
            }
            return supplierWeekWiseMetrics;
        } catch (IOException e) {
            throw new HbaseException(e.getMessage());
        }
    }


    public void put(SupplierWeekWiseMetrics supplierWeekWiseMetrics) {
        Put put = new Put(Bytes.toBytes(supplierWeekWiseMetrics.getRowKey()));
        HbaseUtils.addLongColumn(COLUMN_FAMILY, COLUMN_SUPPLIER_ID, supplierWeekWiseMetrics.getSupplierId(), put);
        HbaseUtils.addBigDecimalAsLongColumn(COLUMN_FAMILY, COLUMN_BUDGET_UTILISED,
                supplierWeekWiseMetrics.getBudgetUtilised(), MULTIPLIER, put);
        HbaseUtils.addLocalDateColumn(COLUMN_FAMILY, COLUMN_WEEK_START_DATE,
                supplierWeekWiseMetrics.getWeekStartDate(), put);

        try (Table table = getTable()) {
            table.put(put);
        } catch (IOException e) {
            log.error("IOException in put SupplierWeekWiseMetricsRepository", e);
            throw new HbaseException(e.getMessage());
        }
    }

    public BigDecimal incrementBudgetUtilised(Long supplierId, LocalDate weekStartDate, BigDecimal interactionMultiplier) {
        if (Objects.isNull(this.get(supplierId, weekStartDate))) {
            this.put(SupplierWeekWiseMetrics.builder().supplierId(supplierId).budgetUtilised(BigDecimal.ZERO)
                    .weekStartDate(weekStartDate).build());
        }
        try (Table table = getTable()) {
            long value = table.incrementColumnValue(
                    Bytes.toBytes(SupplierWeekWiseMetrics.generateRowKey(supplierId, weekStartDate)),
                    COLUMN_FAMILY, COLUMN_BUDGET_UTILISED,
                    interactionMultiplier.multiply(BigDecimal.valueOf(MULTIPLIER)).longValue());
            return BigDecimal.valueOf(value).divide(BigDecimal.valueOf(MULTIPLIER));
        } catch (IOException e) {
            log.error("Error in incrementing budget utilised for supplierId {}", supplierId);
            throw new HbaseException(e.getMessage());
        }
    }

}
