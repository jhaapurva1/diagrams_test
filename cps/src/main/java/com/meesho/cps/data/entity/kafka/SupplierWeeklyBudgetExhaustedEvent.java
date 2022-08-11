package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SupplierWeeklyBudgetExhaustedEvent {

    @JsonProperty("supplier_id")
    private Long supplierId;

    @JsonProperty("catalog_id")
    private Long catalogId;

}
