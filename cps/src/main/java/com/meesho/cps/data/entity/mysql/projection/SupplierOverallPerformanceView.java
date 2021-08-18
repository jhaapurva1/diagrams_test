package com.meesho.cps.data.entity.mysql.projection;

import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
public interface SupplierOverallPerformanceView {

    @Value("#{target.total_budget_utilised}")
    BigDecimal getTotalBudgetUtilized();

    @Value("#{target.total_orders}")
    Integer getTotalOrders();

    @Value("#{target.total_clicks}")
    Long getTotalClicks();

    @Value("#{target.total_views}")
    Long getTotalViews();

    @Value("#{target.total_revenue}")
    BigDecimal getTotalRevenue();

}
