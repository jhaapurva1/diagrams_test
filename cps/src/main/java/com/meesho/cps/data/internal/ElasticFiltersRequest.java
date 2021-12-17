package com.meesho.cps.data.internal;

import lombok.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ElasticFiltersRequest {

    private List<Long> catalogIds;

    private List<Long> campaignIds;

    private List<Long> supplierIds;

    private List<RangeFilter> rangeFilters;

    private List<AggregationBuilder> aggregationBuilders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangeFilter {

        private Object gte;

        private Object lte;

        private Object gt;

        private Object lt;

        private String format;

        private String fieldName;
    }
}
