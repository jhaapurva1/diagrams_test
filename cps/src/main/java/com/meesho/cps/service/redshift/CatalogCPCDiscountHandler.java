package com.meesho.cps.service.redshift;

import com.meesho.cps.data.entity.hbase.CatalogCPCDiscount;
import com.meesho.cps.data.presto.CatalogCPCDiscountPrestoData;
import com.meesho.cps.db.hbase.repository.CatalogCPCDiscountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CatalogCPCDiscountHandler {

    @Autowired
    CatalogCPCDiscountRepository catalogCPCDiscountRepository;

    public void handle(List<CatalogCPCDiscountPrestoData> catalogCPCDiscountPrestoDataList) {
        if (catalogCPCDiscountPrestoDataList.isEmpty())
            return;

        List<CatalogCPCDiscount> catalogCPCDiscountList = new ArrayList<>();
        catalogCPCDiscountPrestoDataList.forEach(entity -> {
            CatalogCPCDiscount catalogCPCDiscount = CatalogCPCDiscount.builder()
                    .catalogId(entity.getCatalogId())
                    .discount(entity.getDiscount())
                    .build();
            catalogCPCDiscountList.add(catalogCPCDiscount);
        });
        catalogCPCDiscountRepository.putAll(catalogCPCDiscountList);
        log.info("CatalogCPCDiscount scheduler processed result set for presto data {}", catalogCPCDiscountPrestoDataList.size());
    }

}
