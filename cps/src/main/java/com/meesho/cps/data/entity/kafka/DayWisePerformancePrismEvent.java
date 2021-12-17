package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DayWisePerformancePrismEvent {
    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("campaign_id")
    private Long campaignId;

    @JsonProperty("catalog_id")
    private Long catalogId;

    @JsonProperty("supplier_id")
    private Long supplierId;

    @JsonProperty("clicks")
    private Long clicks;

    @JsonProperty("views")
    private Long views;

    @JsonProperty("shares")
    private Long shares;

    @JsonProperty("wishlist")
    private Long wishlist;

    @JsonProperty("orders")
    private Integer orders;

    @JsonProperty("revenue")
    private BigDecimal revenue;

    @JsonProperty("budget_utilised")
    private BigDecimal budgetUtilised;

    @JsonProperty("date")
    private String date;

    @JsonProperty("processed_at")
    private String currentTimestamp;
}
