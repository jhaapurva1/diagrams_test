package com.meesho.cps.db.hbase.repository;

import com.meesho.ads.lib.exception.HbaseException;
import com.meesho.ads.lib.utils.HbaseUtils;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.hbase.CatalogCPCDiscount;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class CatalogCPCDiscountRepository {

    private static final String TABLE_NAME = "catalog_cpc_discount_preprod";
    private static final byte[] COLUMN_FAMILY = Bytes.toBytes("cf");
    private static final byte[] COLUMN_CATALOG_ID = Bytes.toBytes("catalog_id");
    private static final byte[] COLUMN_DISCOUNT = Bytes.toBytes("discount");

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

    private CatalogCPCDiscount mapper(Result result) {
        CatalogCPCDiscount catalogCPCDiscount = new CatalogCPCDiscount();
        catalogCPCDiscount.setCatalogId(HbaseUtils.getColumnAsInteger(COLUMN_FAMILY, COLUMN_CATALOG_ID, result));
        catalogCPCDiscount.setDiscount(HbaseUtils.getColumnAsBigDecimal(COLUMN_FAMILY, COLUMN_DISCOUNT, result));
        return catalogCPCDiscount;
    }

    private Put mapper(CatalogCPCDiscount catalogCPCDiscount) {
        Put put = new Put(Bytes.toBytes(catalogCPCDiscount.getRowKey()));
        HbaseUtils.addIntegerColumn(COLUMN_FAMILY, COLUMN_CATALOG_ID, catalogCPCDiscount.getCatalogId(), put);
        HbaseUtils.addBigDecimalColumn(COLUMN_FAMILY, COLUMN_DISCOUNT, catalogCPCDiscount.getDiscount(), put);
        return put;
    }

    public CatalogCPCDiscount get(Integer catalogId) {
        Get get = new Get(Bytes.toBytes(CatalogCPCDiscount.generateRowKey(catalogId)));
        try (Table table = getTable()) {
            Result result = table.get(get);
            if (result.isEmpty())
                return null;
            return mapper(result);
        } catch (TableNotFoundException ex) {
            log.error("TableNotFoundException while fetching Hbase table instance for {}", getTable(), ex);
            return null;
        } catch (IOException e) {
            throw new HbaseException(e.getMessage());
        }
    }

    public void putAll(List<CatalogCPCDiscount> catalogCPCDiscountList) {
        List<Put> puts = new ArrayList<>();
        for (CatalogCPCDiscount entity : catalogCPCDiscountList) {
            puts.add(mapper(entity));
        }

        try (Table table = getTable()) {
            table.put(puts);
        } catch (TableNotFoundException ex) {
            log.error("TableNotFoundException while fetching Hbase table instance for {}", getTable(), ex);
        } catch (IOException e) {
            log.error("IOException in put CatalogCPCDiscount", e);
            throw new HbaseException(e.getMessage());
        }
    }
}
