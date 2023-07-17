package com.meesho.cps.db.mongodb.dao;

import com.meesho.ad.client.constants.FeedType;
import com.meesho.cps.data.entity.internal.CampaignBudgetUtilisedData;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.db.mongodb.repository.CampaignDateWiseMetricsRepository;
import com.meesho.cps.utils.FormattingUtils;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.Decimal128;
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
public class CampaignDateWiseMetricsDao {

    @Autowired
    private CampaignDateWiseMetricsRepository campaignDateWiseMetricsRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public void save(CampaignDateWiseMetrics document) {
        campaignDateWiseMetricsRepository.save(document);
    }

    public void saveAll(List<CampaignDateWiseMetrics> document) {
        campaignDateWiseMetricsRepository.saveAll(document);
    }

    public CampaignDateWiseMetrics findByCampaignIdAndDate(Long campaignId, String date) {
        return campaignDateWiseMetricsRepository.findByCampaignIdAndDate(campaignId, date);
    }

    public List<CampaignDateWiseMetrics> findAllByCampaignIdsInAndDate(List<Long> campaignIds, String date) {
        return campaignDateWiseMetricsRepository.findAllByCampaignIdInAndDate(campaignIds, date);
    }

    public CampaignBudgetUtilisedData incrementCampaignAndRealEstateBudgetUtilised(Long campaignId, String date, BigDecimal budgetUtilised,
                                                                                   FeedType realEstate) {
        Query query = new Query().addCriteria(Criteria.where(CAMPAIGN_ID).is(campaignId).and(DATE).is(date));
        Update update = new Update().inc(BUDGET_UTILISED, budgetUtilised)
                .inc(FormattingUtils.getRealEstateBudgetUtilisedField(realEstate), new Decimal128(budgetUtilised));
        CampaignDateWiseMetrics document = mongoTemplate.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true)
                .upsert(true), CampaignDateWiseMetrics.class);
        return CampaignBudgetUtilisedData.builder()
                .totalBudgetUtilised(document.getBudgetUtilised())
                .realEstateBudgetUtilisedMap(document.getRealEstateBudgetUtilisedMap()).build();
    }
}
