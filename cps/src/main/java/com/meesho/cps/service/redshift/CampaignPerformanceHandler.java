package com.meesho.cps.service.redshift;

import com.meesho.ads.lib.data.internal.RedshiftProcessedMetadata;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.data.redshift.CampaignPerformanceRedshift;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.transformer.CampaignPerformanceTransformer;
import com.meesho.cps.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
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

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
@Slf4j
@Service
public class CampaignPerformanceHandler {

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private CampaignCatalogDateMetricsRepository campaignCatalogMetricsRepository;

    public RedshiftProcessedMetadata<CampaignPerformanceRedshift> transformResults(ResultSet resultSet)
            throws SQLException {
        RedshiftProcessedMetadata<CampaignPerformanceRedshift> redshiftProcessedMetadata =
                CommonUtils.getDefaultRedshitProcessedMetadata();
        List<CampaignPerformanceRedshift> entities = new ArrayList<>();
        CampaignPerformanceRedshift.CampaignPerformanceRedshiftBuilder campaignPerformanceBuilder =
                CampaignPerformanceRedshift.builder();

        int i = 0;
        while (resultSet.next()) {
            Long campaignId = resultSet.getLong("campaign_id");
            long catalogId = resultSet.getLong("catalog_id");
            String date = resultSet.getString("dt");
            String[] dateTimeSplit = date.split(" ");
            date = dateTimeSplit[0];
            BigDecimal revenue = resultSet.getBigDecimal("revenue");
            Integer orderCount = resultSet.getInt("order_count");

            //Temporary fix to handle issue where some rows are having catalog_id as null. Can be removed once
            // redshift issue is fixed
            i++;
            if (catalogId == 0) {
                continue;
            }

            redshiftProcessedMetadata.setLastEntryCreatedAt(resultSet.getString("created_at"));

            campaignPerformanceBuilder.campaignId(campaignId)
                    .catalogId(catalogId)
                    .date(date)
                    .revenue(revenue)
                    .orderCount(orderCount);
            entities.add(campaignPerformanceBuilder.build());
        }
        redshiftProcessedMetadata.setProcessedDataSize(i);
        redshiftProcessedMetadata.setEntities(entities);
        return redshiftProcessedMetadata;
    }

    public void handle(List<CampaignPerformanceRedshift> campaignPerformanceRedshiftList) {
        List<String> keys = campaignPerformanceRedshiftList.stream()
                .map(this::getUniqueKey)
                .collect(Collectors.toList());
        List<CampaignCatalogDateMetrics> campaignCatalogDateMetricsList = campaignPerformanceRedshiftList.stream()
                        .map(CampaignPerformanceTransformer::transform).collect(Collectors.toList());
        campaignCatalogMetricsRepository.putOrdersAndRevenueColumns(campaignCatalogDateMetricsList);

        //update redis set
        List<CampaignCatalogDate> campaignCatalogDates = campaignCatalogDateMetricsList.stream()
                .map(x -> new CampaignCatalogDate(x.getCampaignId(), x.getCatalogId(), x.getDate().toString()))
                .collect(Collectors.toList());
        updatedCampaignCatalogCacheDao.add(campaignCatalogDates);
        log.info("CampaignPerformance scheduler processed result set for campaign_catalog_date {} ", keys);
    }

    public String getUniqueKey(CampaignPerformanceRedshift entity) {
        return String.format(DBConstants.Redshift.CAMPAIGN_CATALOG_DATE_KEY, entity.getCampaignId(),
                entity.getCatalogId(), entity.getDate());
    }

}
