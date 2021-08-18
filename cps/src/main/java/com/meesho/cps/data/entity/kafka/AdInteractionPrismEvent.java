package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.constants.AdInteractionStatus;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdInteractionPrismEvent {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("campaign_id")
    private Long campaignId;

    @JsonProperty("catalog_id")
    private Long catalogId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("origin")
    private String origin;

    @JsonProperty("screen")
    private String screen;

    @JsonProperty("status")
    private AdInteractionStatus status;

    @JsonProperty("reason")
    private AdInteractionInvalidReason reason;

    @JsonProperty("cpc")
    private BigDecimal cpc;

    @JsonProperty("click_multiplier")
    private BigDecimal clickMultiplier;

    @JsonProperty("interaction_type")
    private String interactionType;

    @JsonProperty("event_time")
    private Long eventTimestamp;

    @JsonProperty("event_time_iso")
    private String eventTimeIso;

    @JsonProperty("app_version_code")
    private Integer appVersionCode;

    @JsonProperty("processed_at")
    private String currentTimestamp;

}
