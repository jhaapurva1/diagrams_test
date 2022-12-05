package com.meesho.cps.helper;

import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.ads.lib.utils.EncodingUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.data.internal.ElasticFiltersRequest;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Service
@Slf4j
public class CampaignPerformanceHelper {

    @Autowired
    ApplicationProperties applicationProperties;

    public LocalDate getLocalDateForDailyCampaignFromLocalDateTime(LocalDateTime eventTime) {
        if (beforeResetTimeOfDailyBudgetForCampaign(eventTime)) {
            eventTime = eventTime.minusDays(1);
        }
        return eventTime.toLocalDate();
    }

    public boolean beforeResetTimeOfDailyBudgetForCampaign(LocalDateTime eventTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DailyBudgetConstants.TIME_FORMAT);
        LocalTime resetTime;
        try {
            resetTime = LocalTime.parse(applicationProperties.getDailyBudgetResetTime(), formatter);
        } catch (DateTimeParseException ex) {
            log.error("Error in parsing reset time for daily budget");
            resetTime = LocalTime.MIN;
        }
        return eventTime.toLocalTime().isBefore(resetTime);
    }

    public List<AggregationBuilder> createAggregations() {
        List<AggregationBuilder> aggregationBuilders = new ArrayList<>();
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_CLICKS).field(DBConstants.ElasticSearch.CLICKS));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_VIEWS).field(DBConstants.ElasticSearch.VIEWS));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_SHARES).field(DBConstants.ElasticSearch.SHARES));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_WISHLIST).field(DBConstants.ElasticSearch.WISHLIST));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_ORDERS).field(DBConstants.ElasticSearch.ORDERS));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_REVENUE).field(DBConstants.ElasticSearch.REVENUE));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_BUDGET_UTILISED).field(DBConstants.ElasticSearch.BUDGET_UTILISED));
        return aggregationBuilders;
    }

    public List<AggregationBuilder> createGraphAggregations() {
        List<AggregationBuilder> aggregationBuilders = new ArrayList<>();
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_CLICKS).field(DBConstants.ElasticSearch.CLICKS));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_VIEWS).field(DBConstants.ElasticSearch.VIEWS));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_ORDERS).field(DBConstants.ElasticSearch.ORDERS));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_SHARES).field(DBConstants.ElasticSearch.SHARES));
        aggregationBuilders.add(AggregationBuilders.sum(Constants.ESConstants.TOTAL_WISHLIST).field(DBConstants.ElasticSearch.WISHLIST));
        return aggregationBuilders;
    }

    public List<AggregationBuilder> createBucketAggregations(String term, String fieldName, Integer size) {
        AggregationBuilder aggregationBuilderRoot = AggregationBuilders.terms(term).field(fieldName).size(size);
        List<AggregationBuilder> aggregationBuilders = createAggregations();

        for (AggregationBuilder aggregationBuilder : aggregationBuilders) {
            aggregationBuilderRoot.subAggregation(aggregationBuilder);
        }

        return Collections.singletonList(aggregationBuilderRoot);
    }

    public List<AggregationBuilder> createGraphBucketAggregations(String term, String fieldName, Integer size) {
        AggregationBuilder aggregationBuilderRoot = AggregationBuilders.terms(term).field(fieldName).size(size);
        List<AggregationBuilder> aggregationBuilders = createGraphAggregations();

        for (AggregationBuilder aggregationBuilder : aggregationBuilders) {
            aggregationBuilderRoot.subAggregation(aggregationBuilder);
        }
        return Collections.singletonList(aggregationBuilderRoot);
    }

    public List<ElasticFiltersRequest.RangeFilter> addTillDateRangeFilter() {
        List<ElasticFiltersRequest.RangeFilter> rangeFilters = new ArrayList<>();
        rangeFilters.add(ElasticFiltersRequest.RangeFilter.builder()
                .gte(applicationProperties.getCampaignDatewiseMetricsReferenceDate().format(DateTimeFormatter.ofPattern(Constants.ESConstants.MONTH_DATE_FORMAT)))
                .lte(LocalDate.now().format(DateTimeFormatter.ofPattern(Constants.ESConstants.MONTH_DATE_FORMAT))).fieldName(DBConstants.ElasticSearch.MONTH)
                .format(Constants.ESConstants.MONTH_DATE_FORMAT).build());
        return rangeFilters;
    }

    public List<ElasticFiltersRequest.RangeFilter> addMonthWiseRangeFilter(LocalDate startDate, LocalDate endDate) {
        List<ElasticFiltersRequest.RangeFilter> rangeFilters = new ArrayList<>();
        rangeFilters.add(ElasticFiltersRequest.RangeFilter.builder()
                .gt(startDate.with(TemporalAdjusters.lastDayOfMonth()).format(DateTimeFormatter.ofPattern(Constants.ESConstants.MONTH_DATE_FORMAT)))
                .lt(endDate.with(TemporalAdjusters.firstDayOfMonth()).format(DateTimeFormatter.ofPattern(Constants.ESConstants.MONTH_DATE_FORMAT)))
                .fieldName(DBConstants.ElasticSearch.MONTH).format(Constants.ESConstants.MONTH_DATE_FORMAT).build());
        return rangeFilters;
    }

    public List<ElasticFiltersRequest.RangeFilter> addDateWiseRangeFilters(LocalDate startDate, LocalDate endDate) {
        List<ElasticFiltersRequest.RangeFilter> rangeFilters = new ArrayList<>();
        if ((startDate.getMonth() == endDate.getMonth()) && (startDate.getYear() == endDate.getYear())) {
            rangeFilters.add(ElasticFiltersRequest.RangeFilter.builder().gte(startDate).lte(endDate)
                    .fieldName(DBConstants.ElasticSearch.DATE).format(Constants.ESConstants.DAY_DATE_FORMAT).build());
        } else {
            rangeFilters.add(ElasticFiltersRequest.RangeFilter.builder().fieldName(DBConstants.ElasticSearch.DATE).gte(startDate)
                    .lte(startDate.with(TemporalAdjusters.lastDayOfMonth())).format(Constants.ESConstants.DAY_DATE_FORMAT).build());
            rangeFilters.add(ElasticFiltersRequest.RangeFilter.builder().fieldName(DBConstants.ElasticSearch.DATE).format(Constants.ESConstants.DAY_DATE_FORMAT)
                    .gte(endDate.with(TemporalAdjusters.firstDayOfMonth())).lte(endDate).build());
        }
        return rangeFilters;
    }

    public String decodeCursor(String encodedCursor) {
        String cursor = null;
        try {
            if (!encodedCursor.isEmpty()) {
                cursor = EncodingUtils.decodeFromBase64(encodedCursor, String.class);
            }
        } catch (Exception e) {
            log.error("Exception parsing cursor: {}, using default value.", encodedCursor);
        }
        return cursor;
    }

    public String encodeCursor(String decodedCursor) {
        String cursor = null;
        try {
            if(!decodedCursor.isEmpty()) {
                cursor = EncodingUtils.encodeToBase64(decodedCursor);
            }
        } catch (Exception e) {
            log.error("Exception encoding cursor: {}, using default value", decodedCursor);
        }
        return cursor;
    }

}
