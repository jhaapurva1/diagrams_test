package com.meesho.cps.data.entity.hbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.ads.lib.utils.HashingUtils;
import com.meesho.ads.lib.utils.HbaseUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignDatewiseMetrics {

    private Long campaignId;
    private LocalDate date;
    private BigDecimal budgetUtilised;

    @JsonIgnore
    private String rowKey;

    public static String generateRowKey(Long campaignId, LocalDate date) {
        return HashingUtils.generateRowKeyWithHashedId(campaignId.toString(),
                DateTimeUtils.getLocalDateString(date, HbaseUtils.HBASE_DATE_FORMAT));
    }

    public String getRowKey() {
        rowKey = (rowKey == null) ? generateRowKey(campaignId, date) : rowKey;
        return rowKey;
    }

}
