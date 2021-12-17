package com.meesho.cps.data.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformancePojo {

    private Long totalClicks;

    private Long totalWishlist;

    private Long totalShares;

    private Long totalViews;

    private BigDecimal totalBudgetUtilised;

    private BigDecimal totalRevenue;

    private Integer totalOrders;

}
