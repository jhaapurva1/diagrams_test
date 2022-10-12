package com.meesho.cpsclient.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchActiveCampaignsResponse {

    @JsonProperty("active_campaigns")
    private List<CampaignDetails> activeCampaigns;

    @JsonProperty("cursor")
    private String cursor;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CampaignDetails {

        @JsonProperty("supplier_id")
        private Long supplierID;

        @JsonProperty("campaign_id")
        private Long campaignID;

        @JsonProperty("catalog_ids")
        private List<Long> catalogIds;

    }

}
