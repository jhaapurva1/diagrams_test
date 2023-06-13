package com.meesho.cps.db.mongodb.repository;

import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignCatalogDateMetricsRepository extends MongoRepository<CampaignCatalogDateMetrics, String> {

    CampaignCatalogDateMetrics findByCampaignIdAndCatalogIdAndDate(Long campaignId, Long catalogId, String date);

    List<CampaignCatalogDateMetrics> findByDateAndIdGreaterThanOrderByIdAsc(String date, ObjectId id, Pageable pageable);

    List<CampaignCatalogDateMetrics> findAllByCampaignId(Long campaignId);

}
