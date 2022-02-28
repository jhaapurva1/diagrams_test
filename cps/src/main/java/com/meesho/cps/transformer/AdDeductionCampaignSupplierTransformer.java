package com.meesho.cps.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.utils.Utils;
import com.meesho.commons.utils.DateUtils;
import com.meesho.cps.constants.AdsDeductionPaymentType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.redshift.AdsDeductionCampaignSupplier;
import com.meesho.cps.data.redshift.AdsDeductionCampaignSupplierData;

import java.util.UUID;

public class AdDeductionCampaignSupplierTransformer {

    public static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public static AdsDeductionCampaignSupplier transform(
            AdsDeductionCampaignSupplierData data, String transactionId)
            throws JsonProcessingException {

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
                        .metadata(objectMapper.writeValueAsString(data))
                        .build())
                .build();
    }

}