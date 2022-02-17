package com.meesho.cps.service.redshift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.data.internal.RedshiftProcessedMetadata;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.redshift.AdsDeductionCampaignSupplier;
import com.meesho.cps.service.PayoutKafkaService;
import com.meesho.cps.transformer.AdDeductionCampaignSupplierTransformer;
import com.meesho.cps.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AdsDeductionCampaignSupplierHandler {

    @Autowired
    private PayoutKafkaService payoutKafkaService;

    @Autowired
    private ObjectMapper objectMapper;

    public RedshiftProcessedMetadata<AdsDeductionCampaignSupplier> transformResults(ResultSet resultSet)
            throws SQLException {
        RedshiftProcessedMetadata<AdsDeductionCampaignSupplier> redshiftProcessedMetadata =
                CommonUtils.getDefaultRedshitProcessedMetadata();

        List<AdsDeductionCampaignSupplier> entities = new ArrayList<>();

        while (resultSet.next()) {
            Long campaignId = resultSet.getLong("campaign_id");
            Long supplierId = resultSet.getLong("supplier_id");
            String deductionDuration = resultSet.getString("deduction_duration");
            String startDate = resultSet.getString("start_date");
            BigDecimal gst = resultSet.getBigDecimal("gst");
            BigDecimal netDeduction = resultSet.getBigDecimal("net_deduction");
            BigDecimal credits = resultSet.getBigDecimal("credits");
            BigDecimal adsCost = resultSet.getBigDecimal("ads_cost");
            String transactionId = resultSet.getString("transaction_id");

            redshiftProcessedMetadata.setLastEntryCreatedAt(resultSet.getString("created_at"));

            entities.add(AdDeductionCampaignSupplierTransformer.transform(
                    AdsDeductionCampaignSupplier.AdsDeductionCampaignSupplierData.builder()
                            .supplierId(supplierId)
                            .campaignId(campaignId)
                            .startDate(startDate)
                            .gst(gst)
                            .netDeduction(netDeduction)
                            .deductionDuration(deductionDuration)
                            .credits(credits)
                            .adsCost(adsCost)
                            .build()
                    , transactionId));

        }

        redshiftProcessedMetadata.setProcessedDataSize(entities.size());
        redshiftProcessedMetadata.setEntities(entities);
        return redshiftProcessedMetadata;
    }

    public void handle(List<AdsDeductionCampaignSupplier> adsDeductionCampaignSupplierList) throws JsonProcessingException {

        for (AdsDeductionCampaignSupplier deduction: adsDeductionCampaignSupplierList) {
            //No need to catch, if we catch exception, we wont get alerted even if scheduler is failing
            payoutKafkaService.sendMessage(Constants.ADS_COST_DEDUCTION_TOPIC,
                    null,
                    objectMapper.writeValueAsString(deduction));

        }

    }

    public String getUniqueKey(AdsDeductionCampaignSupplier entity) {
        return String.format(DBConstants.Redshift.ADS_DEDUCTION_CAMPAIGN_KEY, entity.getData().getSupplierId(),
                entity.getData().getMetadata().getCampaignId(), entity.getData().getMetadata().getStartDate());
    }

}
