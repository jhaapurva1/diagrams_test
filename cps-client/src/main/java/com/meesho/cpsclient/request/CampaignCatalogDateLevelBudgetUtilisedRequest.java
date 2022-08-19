package com.meesho.cpsclient.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignCatalogDateLevelBudgetUtilisedRequest {

    @JsonProperty("campaigns")
    @Size(min = 1, message = "Field 'campaigns' can not be empty")
    @NotNull(message = "Field 'campaigns' is required")
    @Valid
    private List<CampaignDetails> campaignDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CampaignDetails {

        @JsonProperty("campaign_id")
        @NotNull(message = "Field 'campaign_id' is required")
        private Long campaignId;
        @JsonProperty("date")
        @NotNull(message = "Field 'date' is required")
        private LocalDate date;
        @JsonProperty("catalogs")
        @Size(min = 1, message = "Field 'catalogs' can not be empty")
        @NotNull(message = "Field 'catalogs' is required")
        @Valid
        private List<CatalogDetails> catalogDetails;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class CatalogDetails {
            @JsonProperty("catalog_id")
            @NotNull(message = "Field 'catalog_id' is required")
            private Long catalogId;
        }
    }
}
