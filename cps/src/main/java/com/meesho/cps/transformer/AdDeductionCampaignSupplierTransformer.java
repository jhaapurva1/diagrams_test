package com.meesho.cps.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.utils.Utils;
import com.meesho.commons.utils.DateUtils;
import com.meesho.cps.constants.AdsDeductionPaymentType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.redshift.AdsDeductionCampaignSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AdDeductionCampaignSupplierTransformer {

    @Autowired
    private ObjectMapper objectMapper;

    public  AdsDeductionCampaignSupplier transform(
            AdsDeductionCampaignSupplier.AdsDeductionCampaignSupplierData data, String transactionId) {

        return AdsDeductionCampaignSupplier.builder()
                .metadata(AdsDeductionCampaignSupplier.MetaData.builder()
                        .timestamp(DateUtils.getCurrentTimestamp(Utils.getCountry()).toInstant().getEpochSecond())
                        .requestId(UUID.randomUUID().toString())
                        .build())
                .data(AdsDeductionCampaignSupplier.AdsDeductionCampaignEventData.builder()
                        .eventType(Constants.ADS_COST_DEDUCTION_EVENT_TYPE)
                        .supplierId(data.getSupplierId())
                        .transactionId(transactionId)
                        .paymentType(AdsDeductionPaymentType.ADS_COST.name())
                        .amount(data.getNetDeduction().add(data.getGst()))
                        .metadata(getMetadata(data))
                        .build())
                .build();
    }

    private String getMetadata(AdsDeductionCampaignSupplier.AdsDeductionCampaignSupplierData data){

        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;

    }

}
