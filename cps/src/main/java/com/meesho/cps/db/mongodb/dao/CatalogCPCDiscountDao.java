package com.meesho.cps.db.mongodb.dao;

import com.meesho.cps.data.entity.mongodb.collection.CatalogCPCDiscount;
import com.meesho.cps.db.mongodb.repository.CatalogCPCDiscountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Slf4j
public class CatalogCPCDiscountDao {

    @Autowired
    private CatalogCPCDiscountRepository catalogCPCDiscountRepository;

    public List<CatalogCPCDiscount> get(List<Long> catalogIds) {
        return catalogCPCDiscountRepository.findAllByCatalogIdIn(catalogIds);
    }

    public CatalogCPCDiscount get(Long catalogId) {
        return catalogCPCDiscountRepository.findByCatalogId(catalogId);
    }

    public void save(List<CatalogCPCDiscount> catalogCPCDiscountList) {
        catalogCPCDiscountRepository.saveAll(catalogCPCDiscountList);
    }

}
