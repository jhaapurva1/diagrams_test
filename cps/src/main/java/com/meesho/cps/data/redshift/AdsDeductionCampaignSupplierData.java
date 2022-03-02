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
public class AdsDeductionCampaignSupplierData {

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
