package com.meesho.cps.data.entity.elasticsearch.internal;

import com.meesho.cps.constants.SortType;
import lombok.Builder;
import lombok.Data;
import org.elasticsearch.search.sort.SortOrder;

@Data
@Builder
public class SortConfig {
    private SortType type;
    private String fieldName;
    private SortOrder order;

    public static SortConfig forField(String fieldName, SortOrder sortOrder) {
        return SortConfig.builder()
                .type(SortType.FIELD)
                .fieldName(fieldName)
                .order(sortOrder)
                .build();
    }

}
