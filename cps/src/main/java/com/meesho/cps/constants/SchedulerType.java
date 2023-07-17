package com.meesho.cps.constants;

import java.util.HashSet;
import java.util.Set;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
public enum SchedulerType {
    CAMPAIGN_PERFORMANCE_NEW,
    CATALOG_CPC_DISCOUNT_NEW,
    DAY_WISE_PERF_EVENTS,
    UNKNOWN;

    public static SchedulerType getInstance(String name) {
        try {
            return SchedulerType.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
        }
        return SchedulerType.UNKNOWN;
    }

    public static final Set<SchedulerType> IN_MEMORY_SCHEDULERS = new HashSet<>();

}
