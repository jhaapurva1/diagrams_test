package com.meesho.cps.data.entity.internal;

import com.meesho.ad.client.constants.FeedType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetUtilisedData {

    private BigDecimal catalogBudgetUtilised;

    private BigDecimal campaignBudgetUtilised;

    private Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap;

}
