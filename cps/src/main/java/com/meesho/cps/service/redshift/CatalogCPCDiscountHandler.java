package com.meesho.cps.service.redshift;

import com.meesho.cps.data.entity.mongodb.collection.CatalogCPCDiscount;
import com.meesho.cps.data.presto.CatalogCPCDiscountPrestoData;
import com.meesho.cps.db.mongodb.dao.CatalogCPCDiscountDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CatalogCPCDiscountHandler {

    @Autowired
    private CatalogCPCDiscountDao catalogCPCDiscountDao;

    public void handle(List<CatalogCPCDiscountPrestoData> catalogCPCDiscountPrestoDataList) {
        if (catalogCPCDiscountPrestoDataList.isEmpty())
            return;

        List<Long> catalogIds = catalogCPCDiscountPrestoDataList.stream().map(CatalogCPCDiscountPrestoData::getCatalogId).collect(Collectors.toList());

        List<CatalogCPCDiscount> existingEntities = catalogCPCDiscountDao.get(catalogIds);

        Map<Long, CatalogCPCDiscount> existingEntitiesMap = existingEntities.stream().collect(Collectors.toMap(CatalogCPCDiscount::getCatalogId, x -> x));

        List<CatalogCPCDiscount> catalogCPCDiscountList = new ArrayList<>();
        catalogCPCDiscountPrestoDataList.forEach(entity -> {
            CatalogCPCDiscount catalogCPCDiscount = existingEntitiesMap.getOrDefault(entity.getCatalogId(), new CatalogCPCDiscount());
            catalogCPCDiscount.setCatalogId(entity.getCatalogId());
            catalogCPCDiscount.setDiscount(entity.getDiscount());
            catalogCPCDiscountList.add(catalogCPCDiscount);
        });

        catalogCPCDiscountDao.save(catalogCPCDiscountList);
        log.info("CatalogCPCDiscount scheduler processed result set for presto data {}", catalogCPCDiscountPrestoDataList.size());
    }

}
