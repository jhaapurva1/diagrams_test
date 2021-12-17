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

    @Value(Constants.Cron.REAL_ESTATE_METADATA_CACHE_SYNC.MONITOR_CODE)
    private Map<String, String> realEstateMetadataCacheSyncCountryAndCronitorCodeMap;

    @Value(Constants.Cron.REAL_ESTATE_METADATA_CACHE_SYNC.ENABLE_SCHEDULER)
    private Map<String, Boolean> realEstateMetadataCacheSyncCountryAndCronEnableMap;

    @Value(Constants.Cron.REAL_ESTATE_METADATA_CACHE_SYNC.CRON_EXPRESSION)
    private Map<String, String> realEstateMetadataCacheSyncCountryAndCronExpMap;

    @Value(Constants.Cron.REAL_ESTATE_METADATA_CACHE_SYNC.BATCH_SIZE)
    private Map<String, Integer> realEstateMetadataCacheSyncCountryAndCronBatchSizeMap;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE_ES_INDEXING.MONITOR_CODE)
    private Map<String, String> campaignPerformanceHbaseESCountryAndCronitorCodeMap;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE_ES_INDEXING.ENABLE_SCHEDULER)
    private Map<String, Boolean> campaignPerformanceHbaseESCountryAndCronEnableMap;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE_ES_INDEXING.CRON_EXPRESSION)
    private Map<String, String> campaignPerformanceHbaseESCountryAndCronExpMap;

    @Value(Constants.Cron.CAMPAIGN_PERFORMANCE_ES_INDEXING.BATCH_SIZE)
    private Map<String, Integer> campaignPerformanceHbaseESCountryAndCronBatchSizeMap;

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

    @Value("${increment_view_hbase_batch_size}")
    private Integer incrementViewHbaseBatchSize;

    @Value("${ad_service_fetch_ccm_batch_size}")
    private Integer adServiceFetchCCMBatchSize;

    @Value("${es.campaign.catalog.date-wise.indices}")
    private String esCampaignCatalogDateWiseIndices;

    @Value("${es.campaign.catalog.month-wise.indices}")
    private String esCampaignCatalogMonthWiseIndices;

    @Value("${redis.updated_campaign_catalogs_set.partition_count}")
    private Integer redisUpdatedCampaignCatalogsSetPartitionCount;

    @Value("${redis.updated_campaign_catalogs_set.batch_size}")
    private Integer redisUpdatedCampaignCatalogSetBatchSize;

    @Value("${campaign.date-wise.metrics.batch-size}")
    private Integer campaignDatewiseMetricsBatchSize;

    @Value("#{T(java.time.LocalDate).parse('${campaign.date-wise.metrics.reference-date}')}")
    private LocalDate campaignDatewiseMetricsReferenceDate;

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
                    case CAMPAIGN_PERFORMANCE:
                        enableCron = campaignPerformanceCountryAndCronEnableMap.get(country.getCountryCode());
                        cronitorCode = campaignPerformanceCountryAndCronitorCodeMap.get(country.getCountryCode());
                        cronExpression = campaignPerformanceCountryAndCronExpMap.get(country.getCountryCode());
                        batchSize = campaignPerformanceCountryAndCronBatchSizeMap.get(country.getCountryCode());
                        processBatchSize =
                                campaignPerformanceCountryAndCronProcessBatchSizeMap.get(country.getCountryCode());
                        break;
                    case REAL_ESTATE_METADATA_CACHE_SYNC:
                        enableCron = realEstateMetadataCacheSyncCountryAndCronEnableMap.get(country.getCountryCode());
                        cronitorCode =
                                realEstateMetadataCacheSyncCountryAndCronitorCodeMap.get(country.getCountryCode());
                        cronExpression = realEstateMetadataCacheSyncCountryAndCronExpMap.get(country.getCountryCode());
                        batchSize = realEstateMetadataCacheSyncCountryAndCronBatchSizeMap.get(country.getCountryCode());
                        processBatchSize = 0; // setting to 0 as this config is not used
                        break;
                    case CAMPAIGN_PERFORMANCE_ES_INDEXING:
                        enableCron = campaignPerformanceHbaseESCountryAndCronEnableMap.get(country.getCountryCode());
                        cronitorCode = campaignPerformanceHbaseESCountryAndCronitorCodeMap.get(country.getCountryCode());
                        cronExpression = campaignPerformanceHbaseESCountryAndCronExpMap.get(country.getCountryCode());
                        batchSize = campaignPerformanceHbaseESCountryAndCronBatchSizeMap.get(country.getCountryCode());
                        processBatchSize = 0; // setting to 0 as this config is not used
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
