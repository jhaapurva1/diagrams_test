package com.meesho.cps.constants;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 17/08/21
 */
@Getter
public enum CampaignType {

    TOTAL_BUDGET("TOTAL_BUDGET"),
    DAILY_BUDGET("DAILY_BUDGET"),
    SMART_CAMPAIGN("SMART_CAMPAIGN");

    String value;

    CampaignType(String value){
        this.value = value;
    };

    private static final Map<String, CampaignType> valuesMap = Arrays.stream(CampaignType.values())
            .collect(Collectors.toMap(x -> x.value, x -> x));

    public static CampaignType fromValue(String value) {
        return valuesMap.getOrDefault(value, null);
    }

}
