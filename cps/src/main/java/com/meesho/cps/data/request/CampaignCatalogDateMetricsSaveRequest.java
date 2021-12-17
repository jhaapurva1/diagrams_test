package com.meesho.cps.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignCatalogDateMetricsSaveRequest {

    @JsonProperty("campaign_id")
    @NotNull
    private Long campaignId;

    @JsonProperty("catalog_id")
    @NotNull
    private Long catalogId;

    @JsonProperty("date")
    @NotNull
    private LocalDate date;

    @JsonProperty("view_count")
    private Long viewCount;

    @JsonProperty("click_count")
    private Long clickCount;

    @JsonProperty("shares_count")
    private Long sharesCount;

    @JsonProperty("wishlist_count")
    private Long wishlistCount;

    @JsonProperty("orders")
    private Integer orders;

    @JsonProperty("revenue")
    private BigDecimal revenue;

    @JsonProperty("budget_utilised")
    private BigDecimal budgetUtilised;

}
