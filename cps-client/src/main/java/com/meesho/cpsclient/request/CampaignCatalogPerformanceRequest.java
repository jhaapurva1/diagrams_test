package com.meesho.cpsclient.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import javax.validation.constraints.NotNull;

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
public class CampaignCatalogPerformanceRequest {

    @NotNull(message = "campaign_id is required")
    @JsonProperty("campaign_id")
    private Long campaignId;

    @NotNull(message = "catalog_ids cannot be empty")
    @JsonProperty("catalog_ids")
    private List<Long> catalogIds;

}
