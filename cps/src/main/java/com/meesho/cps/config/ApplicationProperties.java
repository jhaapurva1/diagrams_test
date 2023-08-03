package com.meesho.cps.config;

import com.meesho.ads.lib.data.internal.SchedulerProperty;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.SchedulerType;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@Configuration
public class ApplicationProperties {

    @Value("${auth.tokens}")
    private List<String> authTokens;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE.MONITOR_CODE)
    private Map<String, String> campaignPerformanceCountryAndCronitorCodeMap;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE.ENABLE_SCHEDULER)
    private Map<String, Boolean> campaignPerformanceCountryAndCronEnableMap;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE.CRON_EXPRESSION)
    private Map<String, String> campaignPerformanceCountryAndCronExpMap;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE.BATCH_SIZE)
    private Map<String, Integer> campaignPerformanceCountryAndCronBatchSizeMap;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE.PROCESS_BATCH_SIZE)
    private Map<String, Integer> campaignPerformanceCountryAndCronProcessBatchSizeMap;

    @Value(Constants.Cron.DAY_WISE_PERF_EVENTS.MONITOR_CODE)
    private Map<String, String> dayWisePerfEventsCountryAndCronitorCodeMap;

    @Value(Constants.Cron.DAY_WISE_PERF_EVENTS.ENABLE_SCHEDULER)
    private Map<String, Boolean> dayWisePerfEventsCountryAndCronEnableMap;

    @Value(Constants.Cron.DAY_WISE_PERF_EVENTS.CRON_EXPRESSION)
    private Map<String, String> dayWisePerfEventsCountryAndCronExpMap;

    @Value(Constants.Cron.DAY_WISE_PERF_EVENTS.BATCH_SIZE)
    private Map<String, Integer> dayWisePerfEventsCountryAndCronBatchSizeMap;

    @Value(Constants.Cron.DAY_WISE_PERF_EVENTS.PROCESS_BATCH_SIZE)
    private Map<String, Integer> dayWisePerfEventsCountryAndCronProcessBatchSizeMap;

    @Value(Constants.Cron.CATALOG_CPC_DISCOUNT.MONITOR_CODE)
    private Map<String, String> catalogCPCDiscountCountryAndCronitorCodeMap;

    @Value(Constants.Cron.CATALOG_CPC_DISCOUNT.ENABLE_SCHEDULER)
    private Map<String, Boolean> catalogCPCDiscountCountryAndCronEnableMap;

    @Value(Constants.Cron.CATALOG_CPC_DISCOUNT.CRON_EXPRESSION)
    private Map<String, String> catalogCPCDiscountCountryAndCronExpMap;

    @Value(Constants.Cron.CATALOG_CPC_DISCOUNT.BATCH_SIZE)
    private Map<String, Integer> catalogCPCDiscountCountryAndCronBatchSizeMap;

    @Value(Constants.Cron.CATALOG_CPC_DISCOUNT.PROCESS_BATCH_SIZE)
    private Map<String, Integer> catalogCPCDiscountCountryAndCronProcessBatchSizeMap;

    private Map<String, Map<String, SchedulerProperty>> schedulerTypeCountryAndPropertyMap = new HashMap<>();

    @Value("#{'${log_disabled_paths}'.split(',')}")
    private List<String> logDisabledPaths;

    @Value("${api.default.connect.timeout.ms}")
    private Integer apiDefaultConnectTimeout;

    @Value("${api.default.read.timeout.ms}")
    private Integer apiDefaultReadTimeout;

    @Value("${daily_budget_reset_time}")
    private String dailyBudgetResetTime;

    @Value("${user_catalog_interaction_window_in_seconds}")
    private Integer userCatalogInteractionWindowInSeconds;

    @Value("${user_catalog_interaction_ttl_seconds}")
    private Integer userCatalogInteractionTTLSeconds;

    @Value("${increment_view_mongo_batch_size}")
    private Integer incrementViewMongoBatchSize;

    @Value("${ad_service_fetch_ccm_batch_size}")
    private Integer adServiceFetchCCMBatchSize;

    @Value("${redis.updated_campaign_catalogs_set.partition_count}")
    private Integer redisUpdatedCampaignCatalogsSetPartitionCount;

    @Value("${redis.updated_campaign_catalogs_set.batch_size}")
    private Integer redisUpdatedCampaignCatalogSetBatchSize;

    @Value("${campaign.date-wise.metrics.batch-size}")
    private Integer campaignDatewiseMetricsBatchSize;

    @Value("${backfill.date-wise.metrics.prism.batch-size}")
    private Integer backfillDateWiseMetricsBatchSize;

    @Value("${kafka.ingestion.view.event.consumer.batch.interval.ms}")
    private Long batchInterval;

    @Value("#{T(java.time.LocalDate).parse('${campaign.date-wise.metrics.reference-date}')}")
    private LocalDate campaignDatewiseMetricsReferenceDate;

    @Value("${encryption.key.ads_metadata}")
    private String adsMetadataEncryptionKey;

    @Value("${cps.common.async.executor.core.pool.size}")
    private Integer commonAsyncExecutorCorePoolSize;

    @Value("${cps.common.async.executor.max.pool.size}")
    private Integer commonAsyncExecutorMaxPoolSize;

    @Value("${cron.app.termination.delay.milliseconds}")
    private Long cronAppTerminationDelayMilliseconds;

    @Value("${spring.data.mongodb.uri}")
    private String mongoDBUri;

    @Value("${schedulers_in_memory}")
    private Set<SchedulerType> schedulersToBeRunInMemory;

    @PostConstruct
    public void init() {
        SchedulerProperty.SchedulerPropertyBuilder schedulerPropertyBuilder = SchedulerProperty.builder();
        for (SchedulerType schedulerType : SchedulerType.values()) {
            for (Country country : Country.values()) {
                Boolean enableCron = null;
                String cronitorCode = null;
                String cronExpression = null;
                Integer batchSize = null;
                Integer processBatchSize = null;

                switch (schedulerType) {
                    case CAMPAIGN_PERFORMANCE_NEW:
                        enableCron = campaignPerformanceCountryAndCronEnableMap.get(country.getCountryCode());
                        cronitorCode = campaignPerformanceCountryAndCronitorCodeMap.get(country.getCountryCode());
                        cronExpression = campaignPerformanceCountryAndCronExpMap.get(country.getCountryCode());
                        batchSize = campaignPerformanceCountryAndCronBatchSizeMap.get(country.getCountryCode());
                        processBatchSize =
                                campaignPerformanceCountryAndCronProcessBatchSizeMap.get(country.getCountryCode());
                        break;
                    case DAY_WISE_PERF_EVENTS:
                        enableCron = dayWisePerfEventsCountryAndCronEnableMap.get(country.getCountryCode());
                        cronitorCode = dayWisePerfEventsCountryAndCronitorCodeMap.get(country.getCountryCode());
                        cronExpression = dayWisePerfEventsCountryAndCronExpMap.get(country.getCountryCode());
                        batchSize = dayWisePerfEventsCountryAndCronBatchSizeMap.get(country.getCountryCode());
                        processBatchSize = dayWisePerfEventsCountryAndCronProcessBatchSizeMap.get(country.getCountryCode());
                        break;
                    case CATALOG_CPC_DISCOUNT_NEW:
                        enableCron = catalogCPCDiscountCountryAndCronEnableMap.get(country.getCountryCode());
                        cronitorCode = catalogCPCDiscountCountryAndCronitorCodeMap.get(country.getCountryCode());
                        cronExpression = catalogCPCDiscountCountryAndCronExpMap.get(country.getCountryCode());
                        batchSize = catalogCPCDiscountCountryAndCronBatchSizeMap.get(country.getCountryCode());
                        processBatchSize = catalogCPCDiscountCountryAndCronProcessBatchSizeMap.get(country.getCountryCode());
                        break;
                }
                Map<String, SchedulerProperty> countryAndSchedulerPropertyMap =
                        schedulerTypeCountryAndPropertyMap.getOrDefault(schedulerType.name(), new HashMap<>());
                if (null != enableCron && null != cronitorCode && null != cronExpression && null != batchSize) {
                    schedulerPropertyBuilder.enableCron(enableCron)
                            .cronitorCode(cronitorCode)
                            .cronExpression(cronExpression)
                            .batchSize(batchSize)
                            .processBatchSize(processBatchSize);
                    countryAndSchedulerPropertyMap.put(country.getCountryCode(), schedulerPropertyBuilder.build());
                    schedulerTypeCountryAndPropertyMap.put(schedulerType.name(), countryAndSchedulerPropertyMap);
                } else {
                    countryAndSchedulerPropertyMap.put(country.getCountryCode(), null);
                    schedulerTypeCountryAndPropertyMap.put(schedulerType.name(), countryAndSchedulerPropertyMap);
                }
            }
        }
    }
}
