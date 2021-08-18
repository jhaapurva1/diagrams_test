package com.meesho.cps.helper;

import com.google.common.collect.Lists;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.data.entity.kafka.CampaignCatalogUpdateEvent;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.db.mysql.dao.CampaignPerformanceDao;

import com.meesho.cps.service.external.AdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Service
@Slf4j
public class CampaignCatalogUpdateEventHelper {

    @Autowired
    private AdService adService;

    @Autowired
    CampaignPerformanceDao campaignPerformanceDao;

    @Autowired
    CampaignPerformanceHelper campaignHelper;

    public void onCampaignCatalogUpdate(CampaignCatalogUpdateEvent.CatalogData catalogData) {
        log.info("Received CampaignCatalogUpdateEvent event {}", catalogData);
        Long catalogId = catalogData.getId();
        Long campaignId = catalogData.getCampaignId();

        CampaignPerformance campaignPerformance =
                campaignPerformanceDao.findByCampaignIdAndCatalogId(campaignId, catalogId)
                        .orElse(CampaignPerformance.builder().campaignId(campaignId).catalogId(catalogId).build());


        List<CampaignDetails> campaignDetailsList = adService.getCampaignMetadata(Lists.newArrayList(campaignId));
        Map<Long, CampaignDetails> campaignIdAndCampaignDetailsMap = campaignDetailsList.stream()
                        .collect(Collectors.toMap(CampaignDetails::getCampaignId , Function.identity()));

        campaignHelper.updateCampaignPerformanceFromHbase(
                Collections.singletonList(campaignPerformance),
                campaignIdAndCampaignDetailsMap
        );
        campaignPerformanceDao.save(campaignPerformance);
    }

}
