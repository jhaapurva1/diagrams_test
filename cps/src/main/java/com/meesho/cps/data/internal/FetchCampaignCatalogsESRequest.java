package com.meesho.cps.data.internal;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FetchCampaignCatalogsESRequest {

    private List<RangeFilter> rangeFilters;

    private Integer limit;

    private String scrollId;

    private List<String> mustExistFields;

    private List<String> includeFields;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RangeFilter {

        private Object gte;

        private Object lte;

        private String format;

        private String fieldName;
    }
}
