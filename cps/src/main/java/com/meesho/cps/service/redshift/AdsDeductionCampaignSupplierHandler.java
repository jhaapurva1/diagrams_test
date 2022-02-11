package com.meesho.cps.service.redshift;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.data.internal.RedshiftProcessedMetadata;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.data.redshift.AdsDeductionCampaignSupplier;
import com.meesho.cps.service.KafkaService;
import com.meesho.cps.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdsDeductionCampaignSupplierHandler {

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private ObjectMapper objectMapper;

    public RedshiftProcessedMetadata<AdsDeductionCampaignSupplier> transformResults(ResultSet resultSet)
            throws SQLException {
        RedshiftProcessedMetadata<AdsDeductionCampaignSupplier> redshiftProcessedMetadata =
                CommonUtils.getDefaultRedshitProcessedMetadata();

        List<AdsDeductionCampaignSupplier> entities = new ArrayList<>();
        AdsDeductionCampaignSupplier.AdsDeductionCampaignSupplierBuilder adsDeductionCampaignSupplierBuilder =
                AdsDeductionCampaignSupplier.builder();
        int i = 0;
        while (resultSet.next()){
            Long campaignId  = resultSet.getLong("campaign_id");
            Long supplierId = resultSet.getLong("supplier_id");
            String deductionDuration = resultSet.getString("deduction_duration");
            String startDate = resultSet.getString("start_date");
            BigDecimal gst = resultSet.getBigDecimal("gst");
            BigDecimal netDeduction = resultSet.getBigDecimal("net_deduction");
            BigDecimal credits = resultSet.getBigDecimal("credits");
            BigDecimal adsCost = resultSet.getBigDecimal("ads_cost");

            i++;

            //TODO add null check

            //TODO add last processed at

            adsDeductionCampaignSupplierBuilder.campaignId(campaignId)
                    .netDeduction(netDeduction)
                    .adsCost(adsCost)
                    .credits(credits)
                    .gst(gst)
                    .startDate(startDate)
                    .deductionDuration(deductionDuration)
                    .supplierId(supplierId);

            entities.add(adsDeductionCampaignSupplierBuilder.build());

        }

        redshiftProcessedMetadata.setProcessedDataSize(i);
        redshiftProcessedMetadata.setEntities(entities);
        return redshiftProcessedMetadata;
    }

    public void handle(List<AdsDeductionCampaignSupplier> adsDeductionCampaignSupplierList) {

        final int retryCount  = 0;

        List<String> keys = adsDeductionCampaignSupplierList.stream()
                .map(this::getUniqueKey)
                .collect(Collectors.toList());

        adsDeductionCampaignSupplierList.forEach(adsDeductionCampaignSupplier -> {
            try {
                kafkaService.sendMessage(Constants.ADS_COST_TOPIC,
                        getUniqueKey(adsDeductionCampaignSupplier),
                        objectMapper.writeValueAsString(adsDeductionCampaignSupplier),
                        retryCount);
            } catch (Exception e) {
                log.error("Exception while sending Ads_Cost event {}", adsDeductionCampaignSupplier, e);
            }
        });

        log.info(SchedulerType.ADS_DEDUCTION_CAMPAIGN_SUPPLIER.name() +
                "Scheduler processed result set for ad seduction show in payment tab {}", keys);
    }

    public String getUniqueKey(AdsDeductionCampaignSupplier entity) {
        return String.format(DBConstants.Redshift.ADS_DEDUCTION_CAMPAIGN_KEY, entity.getSupplierId(),
                entity.getCampaignId(), entity.getStartDate());
    }

}
