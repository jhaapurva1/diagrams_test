package com.meesho.cps.db.mongodb.dao;

import com.meesho.cps.data.entity.mongodb.collection.SupplierWeekWiseMetrics;
import com.meesho.cps.db.mongodb.repository.SupplierWeekWiseMetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

import static com.meesho.cps.constants.MongoFields.*;

@Repository
@Slf4j
public class SupplierWeekWiseMetricsDao {

    @Autowired
    private SupplierWeekWiseMetricsRepository supplierWeekWiseMetricsRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void save(SupplierWeekWiseMetrics document) {
        supplierWeekWiseMetricsRepository.save(document);
    }

    public void saveAll(List<SupplierWeekWiseMetrics> document) {
        supplierWeekWiseMetricsRepository.saveAll(document);
    }

    public SupplierWeekWiseMetrics findBySupplierIdAndWeekStartDate(Long supplierId, String weekStartDate) {
        return supplierWeekWiseMetricsRepository.findBySupplierIdAndWeekStartDate(supplierId, weekStartDate);
    }

    public List<SupplierWeekWiseMetrics> findAllBySupplierIdAndWeekStartDate(List<Long> supplierIds, String weekStartDate) {
        return supplierWeekWiseMetricsRepository.findAllBySupplierIdInAndWeekStartDate(supplierIds, weekStartDate);
    }

    public BigDecimal incrementSupplierWeeklyBudgetUtilised(Long supplierId, String weekStartDate, BigDecimal budgetUtilised) {
        Query query = new Query().addCriteria(Criteria.where(SUPPLIER_ID).is(supplierId).and(WEEK_START_DATE).is(weekStartDate));
        Update update = new Update().inc(BUDGET_UTILISED, budgetUtilised);
        SupplierWeekWiseMetrics document = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true).upsert(true),
                SupplierWeekWiseMetrics.class);
        return document.getBudgetUtilised();
    }
}
