package com.meesho.cps.data.entity.elasticsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meesho.cps.constants.DBConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ESBasePerformanceMetricsDocument {

    @JsonProperty(DBConstants.ElasticSearch.ID)
    private String id;

    @JsonProperty(DBConstants.ElasticSearch.CAMPAIGN_ID)
    private Long campaignId;

    @JsonProperty(DBConstants.ElasticSearch.CATALOG_ID)
    private Long catalogId;

    @JsonProperty(DBConstants.ElasticSearch.SUPPLIER_ID)
    private Long supplierId;

    @JsonProperty(DBConstants.ElasticSearch.CLICKS)
    private Long clicks;

    @JsonProperty(DBConstants.ElasticSearch.VIEWS)
    private Long views;

    @JsonProperty(DBConstants.ElasticSearch.SHARES)
    private Long shares;

    @JsonProperty(DBConstants.ElasticSearch.WISHLIST)
    private Long wishlist;

    @JsonProperty(DBConstants.ElasticSearch.ORDERS)
    private Integer orders;

    @JsonProperty(DBConstants.ElasticSearch.REVENUE)
    private BigDecimal revenue;

    @JsonProperty(DBConstants.ElasticSearch.BUDGET_UTILISED)
    private BigDecimal budgetUtilised;
}
