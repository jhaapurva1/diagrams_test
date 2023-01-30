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

    public boolean hasValues() {
        return !(this.totalClicks.equals(0L) && this.totalWishlist.equals(0L) && this.totalShares.equals(0L) &&
                 this.totalViews.equals(0L) && this.totalBudgetUtilised.equals(BigDecimal.ZERO) &&
                 this.totalRevenue.equals(BigDecimal.ZERO) && totalOrders.equals(0));
    }
}
