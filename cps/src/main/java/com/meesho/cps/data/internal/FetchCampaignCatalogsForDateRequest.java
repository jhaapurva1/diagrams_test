package com.meesho.cps.data.internal;

import com.meesho.cps.data.entity.elasticsearch.internal.SortConfig;
import lombok.*;
import org.springframework.data.util.Pair;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FetchCampaignCatalogsForDateRequest {

    private List<SortConfig> orderedListOfSortConfigs;

    private List<Pair<String, String>> mustMatchKeyValuePairs;

    private Integer limit;

    private Object[] searchAfterValues;

    private List<String> mustExistFields;

    private List<String> includeFields;
}
