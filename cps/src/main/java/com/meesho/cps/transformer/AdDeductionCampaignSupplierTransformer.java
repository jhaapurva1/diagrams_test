package com.meesho.cps.transformer;

import com.meesho.ads.lib.utils.Utils;
import com.meesho.commons.utils.DateUtils;
import com.meesho.cps.constants.AdsDeductionPaymentType;
import com.meesho.cps.data.redshift.AdsDeductionCampaignSupplier;

import java.util.UUID;

public class AdDeductionCampaignSupplierTransformer {

    public static AdsDeductionCampaignSupplier transform(
            AdsDeductionCampaignSupplier.AdsDeductionCampaignSupplierData data, String transactionId){

        return AdsDeductionCampaignSupplier.builder()
                .metadata(AdsDeductionCampaignSupplier.MetaData.builder()
                        .timestamp(DateUtils.toIsoString(DateUtils.getCurrentTimestamp(Utils.getCountry()), Utils.getCountry()))
                        .requestId(UUID.randomUUID().toString())
                        .build())
                .data(AdsDeductionCampaignSupplier.AdsDeductionCampaignEventData.builder()
                        .supplierId(data.getSupplierId())
                        .transactionId(transactionId)
                        .paymentType(AdsDeductionPaymentType.ADS_COST.name())
                        .amount(data.getNetDeduction().add(data.getGst()))
                        .metadata(data)
                        .build())
                .build();
    }

}
