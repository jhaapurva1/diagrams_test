package com.meesho.cps.data.entity.hbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.ads.lib.utils.HashingUtils;
import com.meesho.commons.enums.Country;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignCatalogMetrics {
    private Long campaignId;
    private Long catalogId;

    private BigDecimal weightedClickCount;
    private BigDecimal weightedSharesCount;
    private BigDecimal weightedWishlistCount;

    private Long viewCount;

    private Map<String, Long> originWiseClickCount;

    private BigDecimal budgetUtilised;

    private Country country;

    @JsonIgnore
    private String rowKey;

    public static String generateRowKey(Long campaignId, Long catalogId) {
        return HashingUtils.generateRowKeyWithHashedId(campaignId.toString(), catalogId.toString());
    }

    public String getRowKey() {
        rowKey = (rowKey == null) ? generateRowKey(campaignId, catalogId) : rowKey;
        return rowKey;
    }

}
