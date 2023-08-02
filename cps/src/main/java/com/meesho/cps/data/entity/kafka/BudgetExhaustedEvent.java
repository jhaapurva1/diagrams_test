package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.meesho.cps.constants.BudgetExhaustedReason;
import lombok.Builder;
import lombok.Data;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@Builder
public class BudgetExhaustedEvent {

    @JsonProperty("reason")
    BudgetExhaustedReason reason;

    @JsonProperty("campaign_id")
    private Long campaignId;

    @JsonProperty("catalog_id")
    private Long catalogId;


}
