package com.meesho.cps.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
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
public class CampaignCatalogMetricsSaveRequest {

    @JsonProperty("campaign_id")
    @NotNull
    private Long campaignId;

    @JsonProperty("catalog_id")
    @NotNull
    private Long catalogId;

    @JsonProperty("view_count")
    private Long viewCount;

    @JsonProperty("weighted_click_count")
    private BigDecimal weightedClickCount;

    @JsonProperty("weighted_shares_count")
    private BigDecimal weightedSharesCount;

    @JsonProperty("weighted_wishlist_count")
    private BigDecimal weightedWishlistCount;

    @JsonProperty("origin_wise_click_count")
    private Map<String, Long> originWiseClickCount;

}
