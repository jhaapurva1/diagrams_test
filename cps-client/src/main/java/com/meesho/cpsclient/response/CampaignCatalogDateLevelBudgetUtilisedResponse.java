package com.meesho.cpsclient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignCatalogDateLevelBudgetUtilisedResponse {

    @JsonProperty("budget_utilised")
    private List<CampaignDetails> campaignDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CampaignDetails {

        @JsonProperty("campaign_id")
        private Long campaignId;
        @JsonProperty("date")
        private LocalDate date;
        @JsonProperty("catalogs")
        private List<CatalogDetails> catalogDetails;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CatalogDetails {
            @JsonProperty("id")
            private Long catalogId;
            @JsonProperty("budget_utilised")
            private BigDecimal budgetUtilised;
        }
    }
}
