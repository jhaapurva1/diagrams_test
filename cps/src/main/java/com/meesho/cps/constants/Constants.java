package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public class Constants {

    public static final String BUDGET_EXHAUSTED_TOPIC = "cps.budget_exhuasted";
    public static final String INGESTION_VIEW_EVENTS_DEAD_QUEUE_TOPIC = "cps.ingestion-events.view.dead.queue";
    public static final String INGESTION_INTERACTION_EVENTS_DEAD_QUEUE_TOPIC =
            "cps.ingestion-events.interaction.dead.queue";

    public static class API {
        public static final String BASE_PATH = "/api/v1";

        public static final String HEALTH_CHECK_ENDPOINT = "/health";
        public static final String MANUAL_SCHEDULER_START = "/api/v1/scheduler/start";
        public static final String MIGRATE_CAMPAIGN_PERFORMANCE = "/api/v1/migrate-campaign-performance";

        // performance metrics apis
        public static final String SUPPLIER_PERFORMANCE = "/supplier/performance";
        public static final String CAMPAIGN_PERFORMANCE = "/campaign/performance";
        public static final String CAMPAIGN_CATALOG_PERFORMANCE = "/campaign-catalog/performance";
        public static final String CAMPAIGN_BUDGET_UTILISED = "/campaign/budget-utilised";

        public static class DEBUG_API {
            public static final String BASE_PATH = "/api/v1/debug";
            public static final String GET_CAMPAIGN_METRICS = "/campaign_metrics/get";
            public static final String SAVE_CAMPAIGN_METRICS = "/campaign_metrics/save";
            public static final String SAVE_CAMPAIGN_CATALOG_METRICS = "/campaign_catalog_metrics/save";
            public static final String SAVE_CAMPAIGN_DATEWISE_METRICS = "/campaign_datewise_metrics/save";
            public static final String GET_CAMPAIGN_CATALOG_METRICS = "/campaign_catalog_metrics/get";
            public static final String GET_CAMPAIGN_DATEWISE_METRICS = "/campaign_datewise_metrics/get";
        }

        public static class PrismService {
            public static final String EVENT_PUBLISH = "/api/v1/event/";
        }
    }

    public static class Cron {
        public static class CAMPAIGN_PERFORMANCE {
            public static final String MONITOR_CODE = "#{${scheduler.campaign.performance.monitor.code}}";
            public static final String CRON_EXPRESSION = "#{${scheduler.campaign.performance.cron.expression}}";
            public static final String ENABLE_SCHEDULER = "#{${scheduler.campaign.performance.enable}}";
            public static final String BATCH_SIZE = "#{${scheduler.campaign.performance.batch.size}}";
            public static final String PROCESS_BATCH_SIZE = "#{${scheduler.campaign.performance.process.batch.size}}";
        }

        public static class REAL_ESTATE_METADATA_CACHE_SYNC {
            public static final String MONITOR_CODE = "#{${scheduler.real_estate_metadata.cache.sync.monitor.code}}";
            public static final String CRON_EXPRESSION = "#{${scheduler.real_estate_metadata.cache.sync.cron.expression}}";
            public static final String ENABLE_SCHEDULER = "#{${scheduler.real_estate_metadata.cache.sync.enable}}";
            public static final String BATCH_SIZE = "#{${scheduler.real_estate_metadata.cache.sync.batch.size}}";
        }
    }

    public static class PrismEventNames {
        public static final String AD_INTERACTIONS = "ad_interaction_events";
    }

    public static class DefaultRealEstateMetaData {
        public static final String ORIGIN = "default_origin";
        public static final String SCREEN = "default_screen";
    }

    public static class DailyBudgetConstants {
        public static final String TIME_FORMAT = "HH:mm:ss";
    }

}
