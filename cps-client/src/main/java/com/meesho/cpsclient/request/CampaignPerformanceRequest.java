package com.meesho.cpsclient.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
public class CampaignPerformanceRequest extends BasePerformanceRequest{

    @NotEmpty(message = "campaigns list cannot be empty")
    @JsonProperty("campaigns")
    private List<CampaignDetails> campaignDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CampaignDetails {

        @JsonProperty("campaign_id")
        @NotEmpty(message = "Field 'campaign_id' is required")
        private Long campaignId;

        @NotEmpty(message = "catalog_ids cannot be empty")
        @JsonProperty("catalog_ids")
        private List<Long> catalogIds;
    }
}
