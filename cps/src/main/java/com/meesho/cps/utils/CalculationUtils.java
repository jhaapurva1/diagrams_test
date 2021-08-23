package com.meesho.cps.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * @author shubham.aggarwal
 * 16/08/21
 */
public class CalculationUtils {

    public static Double getConversionRate(Integer orders, Long totalClicks) {
        if (Objects.isNull(totalClicks) || Objects.isNull(orders) || totalClicks == 0) {
            return null;
        }
        double conversionRate = (orders.doubleValue() / totalClicks) * 100;
        return FormattingUtils.round(conversionRate, 2);
    }

    public static BigDecimal getRoi(BigDecimal revenue, BigDecimal budget) {
        if (Objects.isNull(revenue) || Objects.isNull(budget) || BigDecimal.ZERO.compareTo(budget) == 0) {
            return null;
        }
        return revenue.divide(budget, 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal getCpc(BigDecimal budgetUtilised, Long totalClicks) {
        if (Objects.isNull(budgetUtilised) || Objects.isNull(totalClicks) || totalClicks.equals(0l)) {
            return null;
        }
        return budgetUtilised.divide(BigDecimal.valueOf(totalClicks), 2,
                RoundingMode.HALF_UP);
    }

    public static Long getClickCountForBudget(BigDecimal budget, BigDecimal cpc) {
        if (Objects.isNull(budget) || Objects.isNull(cpc) || BigDecimal.ZERO.compareTo(cpc) == 0) {
            return null;
        }
        return budget.divide(cpc, 0, RoundingMode.HALF_UP).longValue();
    }

}
