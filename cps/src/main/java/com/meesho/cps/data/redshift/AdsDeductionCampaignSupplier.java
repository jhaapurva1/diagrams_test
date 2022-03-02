package com.meesho.cps.data.redshift;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdsDeductionCampaignSupplier {

    @JsonProperty(value = "metadata")
    private MetaData metadata;

    @JsonProperty(value = "data")
    private AdsDeductionCampaignEventData data;

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetaData{

        @JsonProperty("timestamp")
        private Long timestamp;

        @JsonProperty("request_id")
        private String requestId;

    }

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
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
        private String metadata;
    }

}
