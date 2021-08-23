package com.meesho.cps.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignMetricsSaveRequest {

    @NotNull
    @JsonProperty("campaign_id")
    private Long campaignId;

    @NotNull
    @JsonProperty("budget_utilised")
    private BigDecimal budgetUtilised;

}
