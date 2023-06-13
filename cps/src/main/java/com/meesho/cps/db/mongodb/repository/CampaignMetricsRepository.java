package com.meesho.cps.db.mongodb.repository;

import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignMetricsRepository extends MongoRepository<CampaignMetrics, String> {

    CampaignMetrics findByCampaignId(Long campaignId);

    List<CampaignMetrics> findAllByCampaignIdIn(List<Long> campaignId);

}
