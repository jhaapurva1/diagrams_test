package com.meesho.cps.db.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.data.entity.elasticsearch.ESDailyIndexDocument;
import com.meesho.cps.data.entity.elasticsearch.ESMonthlyIndexDocument;
import com.meesho.cps.data.entity.elasticsearch.EsCampaignCatalogAggregateResponse;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import com.meesho.cps.data.internal.FetchCampaignCatalogsESRequest;
import com.meesho.cps.exception.ESIndexingException;
import com.meesho.cps.helper.ESQueryBuilder;
import com.meesho.cpsclient.response.CampaignPerformanceDatewiseResponse;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public CampaignPerformanceDatewiseResponse getCampaignCatalogDatePerf(
            ElasticFiltersRequest elasticFiltersRequest, Long campaignId) throws IOException {
        SearchSourceBuilder searchSourceBuilder = ESQueryBuilder.getESQuery(elasticFiltersRequest);
        searchSourceBuilder.size(1000);
        searchSourceBuilder.from(0);
        SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder)
                .indices(applicationProperties.getEsCampaignCatalogDateWiseIndices());
        log.info("Daily index ES query : {}", searchRequest.source().toString());
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
        Map<String, List<CampaignPerformanceDatewiseResponse.CatalogDetails>> dateCatalogsMap = new HashMap<>();
        for (SearchHit searchHit : searchHits.getHits()) {
            ESDailyIndexDocument esDailyIndexDocument = objectMapper.readValue(searchHit.getSourceAsString(),
                    ESDailyIndexDocument.class);
            if(dateCatalogsMap.containsKey(esDailyIndexDocument.getDate())) {
                dateCatalogsMap.get(esDailyIndexDocument.getDate()).add(
                        CampaignPerformanceDatewiseResponse.CatalogDetails.builder()
                                .catalogId(esDailyIndexDocument.getCatalogId())
                                .clicks(esDailyIndexDocument.getClicks())
                                .views(esDailyIndexDocument.getViews())
                                .orders(esDailyIndexDocument.getOrders())
                                .build());
            } else {
                dateCatalogsMap.put(esDailyIndexDocument.getDate(), Lists.newArrayList(
                        CampaignPerformanceDatewiseResponse.CatalogDetails.builder()
                                .catalogId(esDailyIndexDocument.getCatalogId())
                                .clicks(esDailyIndexDocument.getClicks())
                                .views(esDailyIndexDocument.getViews())
                                .orders(esDailyIndexDocument.getOrders())
                                .build()
                ));
            }
        }
        return CampaignPerformanceDatewiseResponse.builder()
                .campaignId(campaignId)
                .dateCatalogsMap(dateCatalogsMap)
                .build();
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

        String scrollId = fetchCampaignCatalogsESRequest.getScrollId();
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(applicationProperties.getFetchActiveCampaignsEsScrollTimeoutMinutes()));
        SearchResponse searchResponse = null;

        if(Objects.nonNull(scrollId)){
            SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId).scroll(scroll);
            log.debug("Date wise index ES Scroll query : {}", scrollRequest.toString());
            searchResponse =  restHighLevelClient.scroll(scrollRequest, RequestOptions.DEFAULT);
        }else{
            SearchSourceBuilder searchSourceBuilder = ESQueryBuilder.getESQuery(fetchCampaignCatalogsESRequest);
            SearchRequest searchRequest = new SearchRequest().source(searchSourceBuilder).scroll(scroll)
                    .indices(applicationProperties.getEsCampaignCatalogDateWiseIndices());
            log.debug("Date wise index ES query : {}", searchRequest.source().toString());
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        }
        return searchResponse;
    }

}
