package com.meesho.cps.db.mysql.dao;

import com.google.common.collect.Lists;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.data.entity.mysql.projection.CampaignOverallPerformanceView;
import com.meesho.cps.data.entity.mysql.projection.SupplierOverallPerformanceView;
import com.meesho.cps.db.mysql.repository.CampaignPerformanceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Service
public class CampaignPerformanceDao {

    @Autowired
    private CampaignPerformanceRepository campaignPerformanceRepository;

    public List<CampaignPerformance> findAllByCampaignIds(List<Long> campaignIds) {
        return campaignPerformanceRepository.findAllByCampaignIdIn(campaignIds);
    }

    public SupplierOverallPerformanceView getOverallPerformanceForSupplier(Long supplierId) {
        return campaignPerformanceRepository.getOverAllPerformanceBySupplierId(supplierId);
    }

    public List<CampaignOverallPerformanceView> getOverallPerformanceForCampaigns(List<Long> campaignIds) {
        return campaignPerformanceRepository.getOverAllPerformanceByCampaignIds(campaignIds);
    }

    public CampaignPerformance save(CampaignPerformance campaignPerformance) {
        return campaignPerformanceRepository.save(campaignPerformance);
    }

    public List<CampaignPerformance> saveAll(Collection<CampaignPerformance> campaignPerformances) {
        return Lists.newArrayList(campaignPerformanceRepository.saveAll(campaignPerformances));
    }

    public Optional<CampaignPerformance> findByCampaignIdAndCatalogId(Long campaignId, Long catalogId) {
        return campaignPerformanceRepository.findByCampaignIdAndCatalogId(campaignId, catalogId);
    }

    public List<CampaignPerformance> findAllByCatalogIdsAndCampaignId(List<Long> catalogIds, Long campaignId) {
        return campaignPerformanceRepository.findAllByCatalogIdInAndCampaignId(catalogIds, campaignId);
    }

    public List<CampaignPerformance> getAllCampaigns(Pageable pageable) {
        return campaignPerformanceRepository.findAll(pageable);
    }
}
