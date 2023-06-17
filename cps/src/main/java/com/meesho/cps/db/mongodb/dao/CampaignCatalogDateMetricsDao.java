package com.meesho.cps.db.mongodb.dao;

import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.cps.constants.UserInteractionType;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.mongodb.projection.CampaignCatalogLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.DateLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.CampaignLevelMetrics;
import com.meesho.cps.data.entity.mongodb.projection.SupplierLevelMetrics;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import com.meesho.cps.db.mongodb.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.helper.ValidationHelper;
import com.meesho.cps.utils.DateTimeHelper;
import com.mongodb.ReadPreference;
import com.mongodb.bulk.BulkWriteError;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.BulkOperationException;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.meesho.cps.constants.MongoFields.*;
import static com.meesho.cps.constants.TelegrafConstants.VIEW_INCREMENTS;
import static com.meesho.cps.utils.DateTimeHelper.MONGO_DATE_FORMAT;

@Repository
@Slf4j
public class CampaignCatalogDateMetricsDao {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Autowired
    private CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @PostConstruct
    public void init() {
        mongoTemplate.setReadPreference(ReadPreference.secondaryPreferred());
    }

    public void save(List<CampaignCatalogDateMetrics> documentList) {
        campaignCatalogDateMetricsRepository.saveAll(documentList);
    }

    public CampaignCatalogDateMetrics find(Long campaignId, Long catalogId, String date) {
        return campaignCatalogDateMetricsRepository.findByCampaignIdAndCatalogIdAndDate(campaignId, catalogId, date);
    }

    public List<CampaignCatalogDateMetrics> findAllByCampaignId(Long campaignId) {
        return campaignCatalogDateMetricsRepository.findAllByCampaignId(campaignId);
    }

    public List<CampaignCatalogDateMetrics> scrollForDate(String date, String lastProcessedId, int limit) throws ParseException {
        ObjectId id;
        if (lastProcessedId == null || "".equals(lastProcessedId)) {
            id = ObjectId.getSmallestWithDate(new SimpleDateFormat(DateTimeHelper.MONGO_DATE_FORMAT).parse(date));
        }
        else {
            id = new ObjectId(lastProcessedId);
        }
        return campaignCatalogDateMetricsRepository.findByDateAndIdGreaterThanOrderByIdAsc(date, id, PageRequest.of(0, limit));
    }

    public List<DateLevelMetrics> getDateLevelMetrics(Long campaignId, String startDate, String endDate) {
        AggregationOperation campaignIdAndDateFilter = Aggregation.match(Criteria.where(CAMPAIGN_ID).is(campaignId)
                .and(DATE).gte(startDate).lte(endDate));


        AggregationOperation group = Aggregation.group(DATE).sum(VIEWS).as(VIEWS).sum(CLICKS).as(CLICKS)
                .sum(ORDERS).as(ORDERS).sum(REVENUE).as(REVENUE).sum(BUDGET_UTILISED).as(BUDGET_UTILISED);

        Aggregation aggregation = Aggregation.newAggregation(campaignIdAndDateFilter, group);


        return mongoTemplate.aggregate(aggregation, CampaignCatalogDateMetrics.class,
                DateLevelMetrics.class).getMappedResults();
    }


    public List<CampaignLevelMetrics> getCampaignLevelMetrics(List<Long> campaignId, String startDate,
                                                              String endDate) {
        AggregationOperation campaignIdAndDateFilter = Aggregation.match(Criteria.where(CAMPAIGN_ID).in(campaignId)
                .and(DATE).gte(startDate).lte(endDate));


        AggregationOperation group = Aggregation.group(CAMPAIGN_ID).sum(VIEWS).as(VIEWS).sum(CLICKS).as(CLICKS)
                .sum(ORDERS).as(ORDERS).sum(REVENUE).as(REVENUE).sum(BUDGET_UTILISED).as(BUDGET_UTILISED)
                .sum(SHARES).as(SHARES).sum(WISHLISTS).as(WISHLISTS);

        Aggregation aggregation = Aggregation.newAggregation(campaignIdAndDateFilter, group);


        return mongoTemplate.aggregate(aggregation, CampaignCatalogDateMetrics.class,
                CampaignLevelMetrics.class).getMappedResults();

    }

    public List<CampaignCatalogLevelMetrics> getCampaignCatalogLevelMetrics(Long campaignId, List<Long> catalogId,
                                                                            String startDate, String endDate) {
        AggregationOperation filter = Aggregation.match(Criteria.where(CAMPAIGN_ID).is(campaignId)
                .and(CATALOG_ID).in(catalogId).and(DATE).gte(startDate).lte(endDate));


        AggregationOperation group = Aggregation.group(CAMPAIGN_ID, CATALOG_ID).sum(VIEWS).as(VIEWS)
                .sum(CLICKS).as(CLICKS).sum(ORDERS).as(ORDERS).sum(REVENUE).as(REVENUE)
                .sum(BUDGET_UTILISED).as(BUDGET_UTILISED).sum(WISHLISTS).as(WISHLISTS).sum(SHARES).as(SHARES);

        Aggregation aggregation = Aggregation.newAggregation(filter, group);

        return mongoTemplate.aggregate(aggregation, CampaignCatalogDateMetrics.class,
                CampaignCatalogLevelMetrics.class).getMappedResults();

    }

    public List<SupplierLevelMetrics> getSupplierLevelMetrics(Long supplierId, String startDate, String endDate) {

        AggregationOperation supplierIdFilter = Aggregation.match(Criteria.where(SUPPLIER_ID).is(supplierId)
                .and(DATE).gte(startDate).lte(endDate));


        AggregationOperation group = Aggregation.group(SUPPLIER_ID).sum(VIEWS).as(VIEWS)
                .sum(CLICKS).as(CLICKS).sum(ORDERS).as(ORDERS).sum(REVENUE).as(REVENUE)
                .sum(BUDGET_UTILISED).as(BUDGET_UTILISED).sum(SHARES).as(SHARES).sum(WISHLISTS).as(WISHLISTS);

        Aggregation aggregation = Aggregation.newAggregation(supplierIdFilter, group);

        return mongoTemplate.aggregate(aggregation, CampaignCatalogDateMetrics.class,
                SupplierLevelMetrics.class).getMappedResults();

    }

    public void incrementInteractionCount(Long supplierId, Long campaignId, Long catalogId, String date, UserInteractionType interactionType) {
        Query query = new Query().addCriteria(Criteria.where(CAMPAIGN_ID).is(campaignId).and(CATALOG_ID).is(catalogId)
                .and(DATE).is(date).and(SUPPLIER_ID).is(supplierId));
        Update update = new Update();
        if (interactionType == UserInteractionType.CLICK) {
            update.inc(CLICKS, 1);
        } else if (interactionType == UserInteractionType.WISHLIST) {
            update.inc(WISHLISTS, 1);
        } else if (interactionType == UserInteractionType.SHARE) {
            update.inc(SHARES, 1);
        }
        mongoTemplate.upsert(query, update, CampaignCatalogDateMetrics.class);
    }

    public BigDecimal incrementBudgetUtilisedAndInteractionCount(Long supplierId, Long campaignId, Long catalogId, String date, BigDecimal budgetUtilised, UserInteractionType interactionType) {
        Query query = new Query().addCriteria(Criteria.where(CAMPAIGN_ID).is(campaignId).and(CATALOG_ID).is(catalogId)
                .and(DATE).is(date).and(SUPPLIER_ID).is(supplierId));
        Update update = new Update().inc(BUDGET_UTILISED, budgetUtilised);
        if (interactionType == UserInteractionType.CLICK) {
            update.inc(CLICKS, 1);
        } else if (interactionType == UserInteractionType.WISHLIST) {
            update.inc(WISHLISTS, 1);
        } else if (interactionType == UserInteractionType.SHARE) {
            update.inc(SHARES, 1);
        }
        CampaignCatalogDateMetrics document = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true).upsert(true),
                CampaignCatalogDateMetrics.class);
        return document.getBudgetUtilised();
    }

    public void bulkIncrementViewCounts(List<CampaignCatalogViewCount> campaignCatalogViewCountList) {
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CampaignCatalogDateMetrics.class);

        List<CampaignCatalogViewCount> invalidDocs = new ArrayList<>();
        List<CampaignCatalogViewCount> validDocs = new ArrayList<>();
        for (CampaignCatalogViewCount campaignCatalogViewCount : campaignCatalogViewCountList) {
            if (!ValidationHelper.isValidViewCountIncrement(campaignCatalogViewCount)) {
                invalidDocs.add(campaignCatalogViewCount);
                continue;
            }
            validDocs.add(campaignCatalogViewCount);
            Query query = new Query().addCriteria(Criteria.where(CAMPAIGN_ID).is(campaignCatalogViewCount.getCampaignId()).and(CATALOG_ID).is(campaignCatalogViewCount.getCatalogId())
                    .and(DATE).is(campaignCatalogViewCount.getDate().toString()).and(SUPPLIER_ID).is(campaignCatalogViewCount.getSupplierId()));
            Update update = new Update().inc(VIEWS, campaignCatalogViewCount.getCount());
            bulkOperations.upsert(query, update);
        }
        if (!CollectionUtils.isEmpty(invalidDocs)) {
            log.error("Couldn't increment view count for {} docs. Invalid data - {}", invalidDocs.size(), invalidDocs);
        }
        if (!CollectionUtils.isEmpty(validDocs)) {
            try {
                bulkOperations.execute();
                telegrafMetricsHelper.increment(VIEW_INCREMENTS, campaignCatalogViewCountList.size() - invalidDocs.size());
            } catch (BulkOperationException e) {
                List<CampaignCatalogViewCount> unwrittenDocuments = new ArrayList<>();
                for (BulkWriteError bulkWriteError : e.getErrors()) {
                    int index = bulkWriteError.getIndex();
                    unwrittenDocuments.add(validDocs.get(index));
                }
                log.error("Error in bulk incrementing views for documents - {}", unwrittenDocuments, e);
                throw e;
            }
        }
    }

    public void bulkWriteOrderAndRevenue(List<CampaignCatalogDateMetrics> documentList) {
        BulkOperations bulkOperations = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, CampaignCatalogDateMetrics.class);

        for (CampaignCatalogDateMetrics document : documentList) {
            if (!ValidationHelper.isValidOrdersAndRevenueUpdate(document)) {
                log.error("Found invalid document for orders and revenue update - {}.", document);
                throw new RuntimeException("Invalid batch for orders and revenue write");
            }
            Query query = new Query().addCriteria(Criteria.where(CAMPAIGN_ID).is(document.getCampaignId()).and(CATALOG_ID).is(document.getCatalogId())
                    .and(DATE).is(document.getDate()).and(SUPPLIER_ID).is(document.getSupplierId()));
            Update update = new Update().set(ORDERS, document.getOrders()).set(REVENUE, document.getRevenue());
            bulkOperations.upsert(query, update);
        }
        bulkOperations.execute();
    }

}
