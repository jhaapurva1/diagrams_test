package com.meesho.cps.service.redshift;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.data.internal.RedshiftProcessedMetadata;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.db.mysql.dao.CampaignPerformanceDao;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.utils.CommonUtils;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
@Slf4j
@Service
public class CampaignPerformanceHandler {

    @Autowired
    private AdService adService;

    @Autowired
    private CampaignPerformanceDao campaignPerformanceDao;

    @Autowired
    private CampaignPerformanceHelper campaignPerformanceHelper;

    public RedshiftProcessedMetadata<CampaignPerformance> transformResults(ResultSet resultSet) throws SQLException {
        RedshiftProcessedMetadata<CampaignPerformance> redshiftProcessedMetadata =
                CommonUtils.getDefaultRedshitProcessedMetadata();
        List<CampaignPerformance> entities = new ArrayList<>();
        CampaignPerformance.CampaignPerformanceBuilder campaignPerformanceBuilder = CampaignPerformance.builder();

        int i = 0;
        while (resultSet.next()) {
            Long campaignId = resultSet.getLong("campaign_id");
            BigDecimal revenue = resultSet.getBigDecimal("revenue");
            Integer orderCount = resultSet.getInt("order_count");
            long catalogId = resultSet.getLong("catalog_id");

            //Temporary fix to handle issue where some rows are having catalog_id as null. Can be removed once
            // redshift issue is fixed
            i++;
            if (catalogId == 0) {
                continue;
            }

            redshiftProcessedMetadata.setLastEntryCreatedAt(resultSet.getString("created_at"));

            campaignPerformanceBuilder.campaignId(campaignId)
                    .catalogId(catalogId)
                    .revenue(revenue)
                    .orderCount(orderCount)
                    .country(Country.getValue(MDC.get(Constants.COUNTRY_CODE)));
            entities.add(campaignPerformanceBuilder.build());
        }
        redshiftProcessedMetadata.setProcessedDataSize(i);
        redshiftProcessedMetadata.setEntities(entities);
        return redshiftProcessedMetadata;
    }

    public void handle(List<CampaignPerformance> campaignPerformanceList) {
        List<Long> campaignIds = campaignPerformanceList.stream()
                .map(CampaignPerformance::getCampaignId)
                .collect(Collectors.toList());

        List<CampaignDetails> catalogMetadataList = adService.getCampaignMetadata(campaignIds);
        if (!CollectionUtils.isEmpty(catalogMetadataList)) {
            Map<Long, CampaignDetails> campaignIdAndCampaignDetailsMap = catalogMetadataList.stream()
                    .collect(Collectors.toMap(CampaignDetails::getCampaignId, Function.identity()));

            campaignPerformanceHelper.updateCampaignPerformanceFromHbase(campaignPerformanceList, campaignIdAndCampaignDetailsMap);
            // update supplier id
            for (CampaignPerformance entity : campaignPerformanceList) {
                CampaignDetails campaignDetails = campaignIdAndCampaignDetailsMap.get(entity.getCampaignId());
                entity.setSupplierId(Objects.nonNull(campaignDetails) ? campaignDetails.getSupplierId() : null);
            }
        }

        List<CampaignPerformance> existingEntities = campaignPerformanceDao.findAllByCampaignIds(campaignIds);

        Map<String, CampaignPerformance> existingEntitiesMap = existingEntities.stream()
                .collect(Collectors.toMap(this::getUniqueKey, Function.identity()));

        for (CampaignPerformance entity : campaignPerformanceList) {
            String key = getUniqueKey(entity);
            if (existingEntitiesMap.containsKey(key)) {
                CampaignPerformance existingEntity = existingEntitiesMap.get(key);
                entity.setId(existingEntity.getId());
            }
        }
        campaignPerformanceDao.saveAll(campaignPerformanceList);
        log.info("CampaignPerformance scheduler processed result set for campaign ids {} ", campaignIds);
    }

    public String getUniqueKey(CampaignPerformance campaignPerformance) {
        return String.format(DBConstants.Redshift.CAMPAIGN_CATALOG_KEY, campaignPerformance.getCampaignId(),
                campaignPerformance.getCatalogId());
    }

}
