package com.meesho.cps.data.entity.hbase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.ads.lib.utils.HashingUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogCPCDiscount {

    private Long catalogId;
    private Double discount;

    @JsonIgnore
    private String rowKey;

    public static String generateRowKey(Long catalogId) {
        return HashingUtils.generateRowKeyWithHashedId(catalogId.toString());
    }

    public String getRowKey() {
        rowKey = (rowKey == null) ? generateRowKey(catalogId) : rowKey;
        return rowKey;
    }

}
