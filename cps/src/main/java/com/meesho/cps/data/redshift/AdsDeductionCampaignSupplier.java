package com.meesho.cps.data.redshift;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdsDeductionCampaignSupplier {

    @JsonProperty(value = "metadata")
    private MetaData metadata;

    @JsonProperty(value = "data")
    private AdsDeductionCampaignEventData data;

    @Data
    @Builder
    public static class MetaData{

        @JsonProperty("timestamp")
        private String timestamp;

        @JsonProperty("request_id")
        private String requestId;

    }

    @Data
    @Builder
    public static class AdsDeductionCampaignEventData {

        @JsonProperty("event_type")
        private String eventType;

        @JsonProperty("supplier_id")
        private Long supplierId;

        @JsonProperty("payment_type")
        private String paymentType;

        @JsonProperty("transaction_id")
        private String transactionId;

        @JsonProperty("amount")
        private BigDecimal amount;

        @JsonProperty("metadata")
        private AdsDeductionCampaignSupplierData metadata;
    }

    @Data
    @Builder
    public static class AdsDeductionCampaignSupplierData{

        @JsonProperty("supplier_id")
        private Long supplierId;

        @JsonProperty("campaign_id")
        private Long campaignId;

        @JsonProperty("ads_cost")
        private BigDecimal adsCost;

        @JsonProperty("gst")
        private BigDecimal gst;

        @JsonProperty("net_deduction")
        private BigDecimal netDeduction;

        @JsonProperty("credits")
        private BigDecimal credits;

        @JsonProperty("deduction_duration")
        private String deductionDuration;

        @JsonProperty("start_date")
        private String startDate;

    }

}
