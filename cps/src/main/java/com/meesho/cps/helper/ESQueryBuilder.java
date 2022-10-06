package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.constants.SortType;
import com.meesho.cps.data.entity.elasticsearch.internal.SortConfig;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import com.meesho.cps.data.internal.FetchCampaignCatalogsForDateRequest;
import lombok.NonNull;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.springframework.data.util.Pair;
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

    public static SearchSourceBuilder getESQuery(@NonNull FetchCampaignCatalogsForDateRequest fetchCampaignCatalogsForDateRequest) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        addMustMatchFieldsToBoolQuery(boolQuery, fetchCampaignCatalogsForDateRequest.getMustMatchKeyValuePairs());
        addMustExistFieldsToBoolQuery(boolQuery, fetchCampaignCatalogsForDateRequest.getMustExistFields());
        SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource().query(boolQuery);
        searchSourceBuilder.fetchSource(fetchCampaignCatalogsForDateRequest.getIncludeFields().toArray(new String[0]), null);
        addPaginationSortingFilters(searchSourceBuilder, fetchCampaignCatalogsForDateRequest);
        return searchSourceBuilder;
    }

    private static void addMustExistFieldsToBoolQuery(BoolQueryBuilder boolQuery, List<String> mustExistFields) {

        mustExistFields.forEach(field -> {
            ExistsQueryBuilder existsQuery = QueryBuilders.existsQuery(field);
            boolQuery.must(existsQuery);
        });
    }

    private static void addMustMatchFieldsToBoolQuery(BoolQueryBuilder boolQuery, List<Pair<String, String>> mustMatchKeyValuePairs) {

        mustMatchKeyValuePairs.forEach(kvPair -> {
            MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(kvPair.getFirst(), kvPair.getSecond());
            boolQuery.must(matchQuery);
        });
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

    private static void addPaginationSortingFilters(SearchSourceBuilder searchSourceBuilder,
                                                    FetchCampaignCatalogsForDateRequest fetchCampaignCatalogsForDateRequest) {
        if(Objects.nonNull(fetchCampaignCatalogsForDateRequest.getSearchAfterValues()) && fetchCampaignCatalogsForDateRequest.getSearchAfterValues().length > 0) {
            searchSourceBuilder.searchAfter(fetchCampaignCatalogsForDateRequest.getSearchAfterValues());
        }

        if (Objects.nonNull(fetchCampaignCatalogsForDateRequest.getLimit())) {
            searchSourceBuilder.size(fetchCampaignCatalogsForDateRequest.getLimit());
        }

        if(!CollectionUtils.isEmpty(fetchCampaignCatalogsForDateRequest.getOrderedListOfSortConfigs())) {
            for (SortConfig sortConfig : fetchCampaignCatalogsForDateRequest.getOrderedListOfSortConfigs()) {
                if (sortConfig.getType().equals(SortType.FIELD)) {
                    searchSourceBuilder.sort(
                            SortBuilders.fieldSort(sortConfig.getFieldName()).order(sortConfig.getOrder()));
                }
            }
        }
    }

    private static TermsQueryBuilder getTermsQuery(String termField, List<?> termIds) {
        return new TermsQueryBuilder(termField, termIds);
    }
}
