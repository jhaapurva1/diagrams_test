package com.meesho.cpsclient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author shubham.aggarwal
 * 27/07/21
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignPerformanceResponse {

    @JsonProperty("campaigns")
    private List<CampaignDetails> campaigns;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CampaignDetails {

        @JsonProperty("campaign_id")
        private Long campaignId;

        @JsonProperty("budget_utilised")
        private BigDecimal budgetUtilised;

        @JsonProperty("revenue")
        private BigDecimal revenue;

        @JsonProperty("order_count")
        private Integer orderCount;

        @JsonProperty("roi")
        private BigDecimal roi;

        @JsonProperty("total_views")
        private Long totalViews;

        @JsonProperty("conversion_rate")
        private Double conversionRate;

        @JsonProperty("total_clicks")
        private Long totalClicks;

    }

}
