package com.meesho.cps.db.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.data.entity.elasticsearch.ESDailyIndexDocument;
import com.meesho.cps.data.entity.elasticsearch.ESMonthlyIndexDocument;
import com.meesho.cps.data.entity.elasticsearch.EsCampaignCatalogAggregateResponse;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import com.meesho.cps.data.internal.FetchCampaignCatalogsESRequest;
import com.meesho.cps.exception.ESIndexingException;
import com.meesho.cps.helper.ESQueryBuilder;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@DigestLogger(metricType = MetricType.METHOD, tagSet = "className=elasticSearchRepository")
public class ElasticSearchRepository {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ObjectMapper objectMapper;

    public void bulkIndexDailyDocs(List<ESDailyIndexDocument> dailyIndexDocuments) throws IOException {
        if(CollectionUtils.isEmpty(dailyIndexDocuments)) {
            log.info("Empty list of documents, returning");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (ESDailyIndexDocument dailyIndexDocument : dailyIndexDocuments) {
            IndexRequest indexRequest = new IndexRequest();
            try {
                indexRequest.index(applicationProperties.getEsCampaignCatalogDateWiseIndices())
                        .id(String.valueOf(dailyIndexDocument.getId()))
                        .source(objectMapper.writeValueAsString(dailyIndexDocument), XContentType.JSON);
                bulkRequest.add(indexRequest);
            } catch (JsonProcessingException e) {
                log.error("failed to create bulk index request for {}", dailyIndexDocument, e);
                throw new ESIndexingException(e.getMessage());
            }
        }
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    public void bulkIndexMonthlyDocs(List<ESMonthlyIndexDocument> monthlyIndexDocuments) throws IOException {
        if(CollectionUtils.isEmpty(monthlyIndexDocuments)) {
            log.info("Empty list of documents, returning");
            return;
        }
        BulkRequest bulkRequest = new BulkRequest();
        for (ESMonthlyIndexDocument monthlyIndexDocument : monthlyIndexDocuments) {
            IndexRequest indexRequest = new IndexRequest();
            try {
                indexRequest.index(applicationProperties.getEsCampaignCatalogMonthWiseIndices())
                        .id(String.valueOf(monthlyIndexDocument.getId()))
                        .source(objectMapper.writeValueAsString(monthlyIndexDocument), XContentType.JSON);
                bulkRequest.add(indexRequest);
            } catch (JsonProcessingException e) {
                log.error("failed to create bulk index request for {}", monthlyIndexDocument, e);
                throw new ESIndexingException(e.getMessage());
            }
        }
        restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    public EsCampaignCatalogAggregateResponse fetchEsCampaignCatalogsDateWise(
            ElasticFiltersRequest elasticFiltersRequest) throws IOException {
        SearchSourceBuilder searchSourceBuilder = ESQueryBuilder.getESQuery(elasticFiltersRequest);
        searchSourceBuilder.size(0);
        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder)
                .indices(applicationProperties.getEsCampaignCatalogDateWiseIndices());
        log.info("Daily index ES query : {}", searchRequest.source().toString());
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return EsCampaignCatalogAggregateResponse.builder().aggregations(searchResponse.getAggregations()).build();
    }

    public EsCampaignCatalogAggregateResponse fetchEsCampaignCatalogsMonthWise(
            ElasticFiltersRequest elasticFiltersRequest) throws IOException {
        SearchSourceBuilder searchSourceBuilder = ESQueryBuilder.getESQuery(elasticFiltersRequest);
        searchSourceBuilder.size(0);
        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder)
                .indices(applicationProperties.getEsCampaignCatalogMonthWiseIndices());
        log.info("Monthly index ES query : {}", searchRequest.source().toString());
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        return EsCampaignCatalogAggregateResponse.builder().aggregations(searchResponse.getAggregations()).build();
    }

    public SearchResponse fetchEsCampaignCatalogsForDate(
            FetchCampaignCatalogsESRequest fetchCampaignCatalogsESRequest) throws IOException {
        SearchSourceBuilder searchSourceBuilder = ESQueryBuilder.getESQuery(fetchCampaignCatalogsESRequest);
        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder)
                .indices(applicationProperties.getEsCampaignCatalogDateWiseIndices());
        log.info("Date wise index ES query : {}", searchRequest.source().toString());
        return restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
    }

}
