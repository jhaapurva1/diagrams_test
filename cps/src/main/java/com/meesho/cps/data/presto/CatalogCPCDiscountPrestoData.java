package com.meesho.cps.data.presto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.prism.proxy.annotations.DataLakeColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CatalogCPCDiscountPrestoData {

    @DataLakeColumn(name = "catalog_id")
    private Integer catalogId;

    @DataLakeColumn(name = "discount")
    private BigDecimal discount;

    @DataLakeColumn(name = "created_at")
    private Timestamp created_at;
}
