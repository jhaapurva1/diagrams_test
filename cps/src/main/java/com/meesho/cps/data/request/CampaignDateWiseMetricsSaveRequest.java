package com.meesho.cps.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.meesho.ad.client.constants.FeedType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignDateWiseMetricsSaveRequest {

    @NotNull
    @JsonProperty("campaign_id")
    private Long campaignId;

    @NotNull
    @JsonProperty("budget_utilised")
    private BigDecimal budgetUtilised;

    @JsonProperty("real_estate_budget_utilised_list")
    private List<RealEstateBudgetUtilised> realEstateBudgetUtilisedList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RealEstateBudgetUtilised {

        @JsonProperty("real_estate")
        private FeedType realEstate;

        @JsonProperty("budget_utilised")
        private BigDecimal budgetUtilised;
    }

}
