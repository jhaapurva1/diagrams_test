package com.meesho.cpsclient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shubham.aggarwal
 * 27/07/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BudgetUtilisedResponse {

    @JsonProperty("campaigns")
    private List<BudgetUtilisedDetails> budgetUtilisedDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BudgetUtilisedDetails {

        @JsonProperty("campaign_id")
        private Long campaignId;

        @JsonProperty("budget_utilised")
        private BigDecimal budgetUtilised;

    }

}
