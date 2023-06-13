package com.meesho.cps.data.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BasePerformanceMetrics {

    private Long clicks;

    private Long views;

    private BigDecimal budgetUtilised;

    private BigDecimal revenue;

    private Integer orders;

    private Long shares;

    private Long wishlists;

}
