package com.meesho.cps.data.entity.hbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.ads.lib.utils.HashingUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignCatalogDateMetrics {
    private Long campaignId;
    private Long catalogId;
    private Long clickCount;
    private Long sharesCount;
    private Long wishlistCount;
    private Long viewCount;
    private BigDecimal budgetUtilised;
    private BigDecimal revenue;
    private Integer orders;
    private LocalDate date;

    @JsonIgnore
    private String rowKey;

    public static String generateRowKey(Long campaignId, Long catalogId, LocalDate date) {
        return HashingUtils.generateRowKeyWithHashedId(campaignId.toString(), catalogId.toString(), date.toString());
    }

    public static String generateRowKeyForMonthPrefix(Long campaignId, Long catalogId, String monthPrefix) {
        return HashingUtils.generateRowKeyWithHashedId(campaignId.toString(), catalogId.toString(), monthPrefix);
    }

    public String getRowKey() {
        rowKey = (rowKey == null) ? generateRowKey(campaignId, catalogId, date) : rowKey;
        return rowKey;
    }

    public static LocalDate getLocalDateFromRowKey(String rowKey) {
        String dateString = rowKey.substring(rowKey.lastIndexOf(":") + 1);
        return LocalDate.parse(dateString);
    }

}
