package com.meesho.cps.db.mongodb.repository;

import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CampaignCatalogDateMetricsRepository extends MongoRepository<CampaignCatalogDateMetrics, String> {

    List<CampaignCatalogDateMetrics> findByDateAndIdGreaterThanAndBudgetUtilisedExistsOrderByIdAsc(String date, ObjectId id, Boolean exists, Pageable pageable);

    List<CampaignCatalogDateMetrics> findAllByCampaignId(Long campaignId);

}
