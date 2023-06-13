package com.meesho.cps.db.mongodb.repository;

import com.meesho.cps.data.entity.mongodb.collection.CatalogCPCDiscount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogCPCDiscountRepository extends MongoRepository<CatalogCPCDiscount, String> {

    CatalogCPCDiscount findByCatalogId(Long catalogId);

    List<CatalogCPCDiscount> findAllByCatalogIdIn(List<Long> catalogId);

}
