package com.meesho.cps.data.entity.hbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.ads.lib.utils.HashingUtils;
import com.meesho.ads.lib.utils.HbaseUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SupplierWeekWiseMetrics {

    private Long supplierId;
    private BigDecimal budgetUtilised;
    private LocalDate weekStartDate;

    @JsonIgnore
    private String rowKey;

    public static String generateRowKey(Long supplierId, LocalDate weekStartDate) {
        return HashingUtils.generateRowKeyWithHashedId(supplierId.toString(),
                DateTimeUtils.getLocalDateString(weekStartDate, HbaseUtils.HBASE_DATE_FORMAT));
    }

    public String getRowKey() {
        rowKey = (rowKey == null) ? generateRowKey(supplierId, weekStartDate) : rowKey;
        return rowKey;
    }

}
