package com.meesho.cps.db.mongodb.dao;

import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.db.mongodb.repository.CampaignMetricsRepository;
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
public class CampaignMetricsDao {

    @Autowired
    private CampaignMetricsRepository campaignMetricsRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void save(CampaignMetrics document) {
        campaignMetricsRepository.save(document);
    }

    public void saveAll(List<CampaignMetrics> document) {
        campaignMetricsRepository.saveAll(document);
    }

    public CampaignMetrics findByCampaignId(Long campaignId) {
        return campaignMetricsRepository.findByCampaignId(campaignId);
    }

    public List<CampaignMetrics> findAllByCampaignIdsIn(List<Long> campaignIds) {
        return campaignMetricsRepository.findAllByCampaignIdIn(campaignIds);
    }

    public BigDecimal incrementCampaignBudgetUtilised(Long campaignId, BigDecimal budgetUtilised) {
        Query query = new Query().addCriteria(Criteria.where(CAMPAIGN_ID).is(campaignId));
        Update update = new Update().inc(BUDGET_UTILISED, budgetUtilised);
        CampaignMetrics document = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true).upsert(true),
                CampaignMetrics.class);
        return document.getBudgetUtilised();
    }

}
