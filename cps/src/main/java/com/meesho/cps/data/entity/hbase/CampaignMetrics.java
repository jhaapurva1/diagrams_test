package com.meesho.cps.data.entity.hbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.ads.lib.utils.HashingUtils;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignMetrics {

    private Long campaignId;
    private BigDecimal budgetUtilised;

    @JsonIgnore
    private String rowKey;

    public static String generateRowKey(Long campaignId) {
        return HashingUtils.generateRowKeyWithHashedId(campaignId.toString());
    }

    public String getRowKey() {
        rowKey = (rowKey == null) ? generateRowKey(campaignId) : rowKey;
        return rowKey;
    }

}
