package com.meesho.cps.scheduler.redshift;

import com.meesho.ads.lib.scheduler.PrestoFeedIngestionScheduler;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.data.presto.CatalogCPCDiscountPrestoData;
import com.meesho.cps.service.redshift.CatalogCPCDiscountHandler;
import com.meesho.prism.beans.PrismSortOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Component
public class CatalogCPCDiscountScheduler extends PrestoFeedIngestionScheduler<CatalogCPCDiscountPrestoData> {

    @Autowired
    CatalogCPCDiscountHandler catalogCPCDiscountHandler;

    @Override
    public String getPrestoTableName() {
        return DBConstants.PrestoTables.CATALOG_CPC_DISCOUNT;
    }

    @Override
    public Class<CatalogCPCDiscountPrestoData> getClassType() {
        return CatalogCPCDiscountPrestoData.class;
    }

    @Override
    public void putUniqueKeySortOrder(LinkedHashMap<String, PrismSortOrder> sortOrderMap) {
        sortOrderMap.put("catalog_id", PrismSortOrder.ASCENDING);
    }

    @Override
    public void handle(List<CatalogCPCDiscountPrestoData> entities) throws SQLException {
        catalogCPCDiscountHandler.handle(entities);
    }

    @Override
    public String getSchedulerKey(CatalogCPCDiscountPrestoData entity) {
        return entity.getCatalogId().toString();
    }

    @Override
    public String getType() {
        return SchedulerType.CATALOG_CPC_DISCOUNT.name();
    }
}
