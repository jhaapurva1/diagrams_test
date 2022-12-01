package com.meesho.cpsclient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignPerformanceDatewiseResponse {

    @JsonProperty("campaign_id")
    private Long campaignId;

    @JsonProperty("date_catalog_details_map")
    Map<LocalDate, GraphDetails> dateCatalogsMap;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GraphDetails {

        @JsonProperty("orders")
        private Integer orders;

        @JsonProperty("views")
        private Long views;

        @JsonProperty("clicks")
        private Long clicks;

    }
}
