package com.meesho.cpsclient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.meesho.ad.client.constants.FeedType;
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
    private List<BudgetUtilisedDetails> budgetUtilisedDetails = new ArrayList<>();

    @JsonProperty("suppliers")
    private List<SupplierBudgetUtilisedDetails> suppliersBudgetUtilisedDetails = new ArrayList<>();

    public BigDecimal getCampaignBudgetUtilized(Long campaignId) {
        return budgetUtilisedDetails.stream().filter(x -> x.getCampaignId().equals(campaignId))
                .map(BudgetUtilisedDetails::getBudgetUtilised).findAny().orElse(BigDecimal.ZERO);
    }

    public BigDecimal getSupplierBudgetUtilized(Long supplierId) {
        return suppliersBudgetUtilisedDetails.stream().filter(x -> x.getSupplierId().equals(supplierId))
                .map(SupplierBudgetUtilisedDetails::getBudgetUtilised).findAny().orElse(BigDecimal.ZERO);
    }

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

        @JsonProperty("real_estate_budget_utilised_list")
        private List<RealEstateBudgetUtilised> realEstateBudgetUtilisedList;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class RealEstateBudgetUtilised {

            @JsonProperty("real_estate")
            private FeedType realEstate;

            @JsonProperty("budget_utilised")
            private BigDecimal budgetUtilised;;
        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SupplierBudgetUtilisedDetails {

        @JsonProperty("supplier_id")
        private Long supplierId;

        @JsonProperty("budget_utilised")
        private BigDecimal budgetUtilised;

    }
}
