package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@Builder
public class BudgetExhaustedEvent {

    @JsonProperty("campaign_id")
    private Long campaignId;

    @JsonProperty("catalog_id")
    private Long catalogId;


}
