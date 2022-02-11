package com.meesho.cps.data.redshift;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdsDeductionCampaignSupplier {

    private MetaData metaData;
    private AdsDeductionCampaignEventData data;

    @Data
    @Builder
    public static class MetaData{
        String timestamp;
        String requestId;
    }

    @Data
    @Builder
    public static class AdsDeductionCampaignEventData {
        String eventType;
        Long supplierId;
        String paymentType;
        String transactionId;
        BigDecimal amount;
        AdsDeductionCampaignSupplierData metadata;
    }

    @Data
    @Builder
    public static class AdsDeductionCampaignSupplierData{

        private Long supplierId;
        private Long campaignId;
        private BigDecimal adsCost;
        private BigDecimal gst;
        private BigDecimal netDeduction;
        private BigDecimal credits;
        private String deductionDuration;
        private String startDate;

    }

}
