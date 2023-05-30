package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
public enum SchedulerType {
    CAMPAIGN_PERFORMANCE, REAL_ESTATE_METADATA_CACHE_SYNC, CAMPAIGN_PERFORMANCE_ES_INDEXING, ADS_DEDUCTION_CAMPAIGN_SUPPLIER,
    UNKNOWN;

    public static SchedulerType getInstance(String name) {
        try {
            return SchedulerType.valueOf(name.toUpperCase());
        } catch (Exception ignored) {
        }
        return SchedulerType.UNKNOWN;
    }
}
