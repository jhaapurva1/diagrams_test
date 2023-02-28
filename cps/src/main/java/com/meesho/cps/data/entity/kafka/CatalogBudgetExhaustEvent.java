package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CatalogBudgetExhaustEvent {

    @JsonProperty("campaign_id")
    private Long campaignId;

    @JsonProperty("catalog_id")
    private Long catalogId;

}
