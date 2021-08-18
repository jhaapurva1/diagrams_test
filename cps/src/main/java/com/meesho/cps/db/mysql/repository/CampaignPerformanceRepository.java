package com.meesho.cps.db.mysql.repository;

import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.data.entity.mysql.projection.CampaignOverallPerformanceView;
import com.meesho.cps.data.entity.mysql.projection.SupplierOverallPerformanceView;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Repository
public interface CampaignPerformanceRepository extends CrudRepository<CampaignPerformance, Long> {

    List<CampaignPerformance> findAllByCampaignIdIn(List<Long> campaignIds);

    List<CampaignPerformance> findByCampaignId(Long campaignId);

    Optional<CampaignPerformance> findByCampaignIdAndCatalogId(Long campaignId, Long catalogId);

    List<CampaignPerformance> findAllByCatalogIdInAndCampaignId(List<Long> catalogIds, Long campaignId);

    @Query(value = "SELECT SUM(cp.revenue) as total_revenue, SUM(cp.orderCount) as total_orders," +
            "SUM(cp.budgetUtilised) as total_budget_utilised, SUM(cp.totalViews) as total_views," +
            "SUM(cp.totalClicks) as total_clicks FROM CampaignPerformance cp " + "WHERE cp.supplierId = :supplierId")
    SupplierOverallPerformanceView getOverAllPerformanceBySupplierId(@Param("supplierId") Long supplierId);

    @Query(value = "SELECT cp.campaignId as campaign_id, SUM(cp.revenue) as total_revenue," +
            "SUM(cp.orderCount) as total_orders, SUM(cp.budgetUtilised) as total_budget_utilised," +
            "SUM(cp.totalViews) as total_views, SUM(cp.totalClicks) as total_clicks FROM CampaignPerformance cp " +
            "WHERE cp.campaignId in (:campaignIds) group by cp.campaignId")
    List<CampaignOverallPerformanceView> getOverAllPerformanceByCampaignIds(
            @Param("campaignIds") List<Long> campaignIds);

}
