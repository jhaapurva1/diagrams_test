package com.meesho.cps.db.mongodb.repository;

import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignDateWiseMetricsRepository extends MongoRepository<CampaignDateWiseMetrics, String> {

    CampaignDateWiseMetrics findByCampaignIdAndDate(Long campaignId, String date);

    List<CampaignDateWiseMetrics> findAllByCampaignIdInAndDate(List<Long> campaignId, String date);

}
