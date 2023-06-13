package com.meesho.cps.db.mongodb.repository;

import com.meesho.cps.data.entity.mongodb.collection.SupplierWeekWiseMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierWeekWiseMetricsRepository extends MongoRepository<SupplierWeekWiseMetrics, String> {

    SupplierWeekWiseMetrics findBySupplierIdAndWeekStartDate(Long supplierId, String weekStartDate);

    List<SupplierWeekWiseMetrics> findAllBySupplierIdInAndWeekStartDate(List<Long> supplierId, String weekStartDate);

}
