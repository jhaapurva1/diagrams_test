package com.meesho.cps.data.presto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.meesho.prism.proxy.annotations.DataLakeColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignCatalogReconciledMetricsPrestoData {

    @DataLakeColumn(name ="campaign_id")
    private Long campaignId;

    @DataLakeColumn(name ="campaign_type")
    private String campaignType;

    @DataLakeColumn(name ="catalog_id")
    private Long catalogId;

    @DataLakeColumn(name ="shares_count")
    private Long sharesCount;

    @DataLakeColumn(name ="click_count")
    private Long clickCount;

    @DataLakeColumn(name ="wishlist_count")
    private Long wishlistCount;

    @DataLakeColumn(name ="budget_utilized")
    private Double budgetUtilized;

    @DataLakeColumn(name ="dump_id")
    private String dumpId;

    @DataLakeColumn(name ="event_date")
    private String eventDate;


}
