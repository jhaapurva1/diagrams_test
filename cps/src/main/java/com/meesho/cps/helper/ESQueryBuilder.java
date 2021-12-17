package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import lombok.NonNull;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

public class ESQueryBuilder {

    private ESQueryBuilder() {
    }

    public static SearchSourceBuilder getESQuery(@NonNull ElasticFiltersRequest elasticFiltersRequest) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        addQueriesForFiltersPassedInRequest(boolQuery, elasticFiltersRequest);
        SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource().query(boolQuery);
        addRangeFilters(boolQuery, elasticFiltersRequest);
        addAggregations(searchSourceBuilder, elasticFiltersRequest);
        return searchSourceBuilder;
    }

    private static void addAggregations(SearchSourceBuilder searchSourceBuilder, ElasticFiltersRequest elasticFiltersRequest) {
        if (!CollectionUtils.isEmpty(elasticFiltersRequest.getAggregationBuilders())) {
            for (AggregationBuilder aggregationBuilder : elasticFiltersRequest.getAggregationBuilders()) {
                searchSourceBuilder.aggregation(aggregationBuilder);
            }
        }
    }

    private static void addRangeFilters(BoolQueryBuilder topLevelBoolQuery, ElasticFiltersRequest elasticFiltersRequest) {
        if (!CollectionUtils.isEmpty(elasticFiltersRequest.getRangeFilters())) {
            BoolQueryBuilder rangeBoolQuery = QueryBuilders.boolQuery();
            for (ElasticFiltersRequest.RangeFilter rangeFilter : elasticFiltersRequest.getRangeFilters()) {
                RangeQueryBuilder rangeQueryBuilder = new RangeQueryBuilder(rangeFilter.getFieldName());
                if (Objects.nonNull(rangeFilter.getGte())) {
                    rangeQueryBuilder.gte(rangeFilter.getGte());
                }
                if (Objects.nonNull(rangeFilter.getLte())) {
                    rangeQueryBuilder.lte(rangeFilter.getLte());
                }
                if (Objects.nonNull(rangeFilter.getLt())) {
                    rangeQueryBuilder.lt(rangeFilter.getLt());
                }
                if (Objects.nonNull(rangeFilter.getGt())) {
                    rangeQueryBuilder.gt(rangeFilter.getGt());
                }
                rangeBoolQuery.should(rangeQueryBuilder);
            }
            topLevelBoolQuery.filter(rangeBoolQuery);
        }
    }

    private static void addQueriesForFiltersPassedInRequest(BoolQueryBuilder topLevelBoolQuery, ElasticFiltersRequest elasticFiltersRequest) {
        if (!CollectionUtils.isEmpty(elasticFiltersRequest.getCatalogIds())) {
            topLevelBoolQuery.filter(getTermsQuery(DBConstants.ElasticSearch.CATALOG_ID, elasticFiltersRequest.getCatalogIds()));
        }
        if (!CollectionUtils.isEmpty(elasticFiltersRequest.getCampaignIds())) {
            topLevelBoolQuery.filter(getTermsQuery(DBConstants.ElasticSearch.CAMPAIGN_ID, elasticFiltersRequest.getCampaignIds()));
        }
        if (!CollectionUtils.isEmpty(elasticFiltersRequest.getSupplierIds())) {
            topLevelBoolQuery.filter(getTermsQuery(DBConstants.ElasticSearch.SUPPLIER_ID, elasticFiltersRequest.getSupplierIds()));
        }
    }

    private static TermsQueryBuilder getTermsQuery(String termField, List<?> termIds) {
        return new TermsQueryBuilder(termField, termIds);
    }
}
