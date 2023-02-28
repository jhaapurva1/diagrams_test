package com.meesho.cps.data.entity.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetUtilisedData {

    private BigDecimal catalogBudgetUtilised;

    private BigDecimal campaignBudgetUtilised;

}
