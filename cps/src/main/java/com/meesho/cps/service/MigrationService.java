package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.db.mysql.dao.CampaignPerformanceDao;
import com.meesho.cps.service.external.AdService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 19/08/21
 */
@Service
@Slf4j
public class MigrationService {

    @Autowired
    private AdService adService;

    @Autowired
    private CampaignPerformanceDao campaignPerformanceDao;

    public Long migrateCampaignPerformance(){
        long offset = 0;
        boolean processed = false;
        int page = 0;
        int limit = 100;
        while (!processed) {
            Pageable pageable = PageRequest.of(page, limit);
            List<CampaignPerformance> campaignPerformanceList = campaignPerformanceDao.getAllCampaigns(pageable);
            if (CollectionUtils.isEmpty(campaignPerformanceList)) {
                processed = true;
            }
            List<Long> campaignIds = campaignPerformanceList.stream().map(CampaignPerformance::getCampaignId).collect(Collectors.toList());
            List<CampaignDetails> campaignDetailsList = adService.getCampaignMetadata(campaignIds);
            if(!CollectionUtils.isEmpty(campaignDetailsList)){
                Map<Long, CampaignDetails> campaignDetailsMap = campaignDetailsList.stream().collect(Collectors.toMap(CampaignDetails::getCampaignId, Function.identity()));
                List<CampaignPerformance> entitiesToSave = new ArrayList<>();
                for(CampaignPerformance campaignPerformance: campaignPerformanceList){
                    CampaignDetails campaignDetails = campaignDetailsMap.getOrDefault(campaignPerformance.getCampaignId(), null);
                    if(Objects.isNull(campaignPerformance.getSupplierId()) && !Objects.isNull(campaignDetails)){
                        Long supplierId = campaignDetails.getSupplierId();
                        campaignPerformance.setSupplierId(supplierId);
                        entitiesToSave.add(campaignPerformance);
                    }
                }
                campaignPerformanceDao.saveAll(entitiesToSave);
                log.info("Campaign performance supplier id updated for: {}", entitiesToSave);
            }
            offset += campaignPerformanceList.size();
            page++;
        }
        return offset;
    }

}
