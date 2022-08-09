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
public class CampaignPerformancePrestoData {

    @DataLakeColumn(name = "catalog_id")
    private Long catalogId;

    @DataLakeColumn(name = "campaign_id")
    private Long campaignId;

    @DataLakeColumn(name = "dt")
    private String date;

    @DataLakeColumn(name = "order_count")
    private Integer orderCount;

    @DataLakeColumn(name = "revenue")
    private Double revenue;

    @DataLakeColumn(name = "created_at")
    private Timestamp created_at;


}
