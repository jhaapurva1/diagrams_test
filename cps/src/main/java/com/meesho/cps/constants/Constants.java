package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public class Constants {

    public static final String ADS_COST_DEDUCTION_EVENT_TYPE = "SUPPLIER";
    public static final String[] CAMPAIGN_CATALOG_DATE_FORMAT = new String[]{"campaign_id", "catalog_id", "date"};

    public static class API {
        public static final String BASE_PATH = "/api/v1";
        public static final Integer HBASE_BATCH_SIZE = 30;
        public static final String HEALTH_CHECK_ENDPOINT = "/health";
        public static final String MANUAL_SCHEDULER_START = "/api/v1/scheduler/start";
        public static final String MIGRATE_CAMPAIGN_PERFORMANCE = "/api/v1/migrate-campaign-performance";

        // performance metrics apis
        public static final String SUPPLIER_PERFORMANCE = "/supplier/performance";
        public static final String CAMPAIGN_PERFORMANCE = "/campaign/performance";
        public static final String CAMPAIGN_CATALOG_PERFORMANCE = "/campaign-catalog/performance";
        public static final String CAMPAIGN_CATALOG_PERFORMANCE_DATE_WISE = "/campaign-catalog/performance_date_wise";
        public static final String CAMPAIGN_BUDGET_UTILISED = "/campaign/budget-utilised";

        public static final String CAMPAIGN_CATALOG_DATE_LEVEL_BUDGET_UTILISED = "campaign-catalog-date/budget-utilised";
        public static final String ACTIVE_CAMPAIGNS = "/get-active-campaigns";

        public static class DEBUG_API {
            public static final String BASE_PATH = "/api/v1/debug";
            public static final String GET_CAMPAIGN_METRICS = "/campaign_metrics/get";
            public static final String SAVE_CAMPAIGN_METRICS = "/campaign_metrics/save";
            public static final String SAVE_CAMPAIGN_CATALOG_METRICS = "/campaign_catalog_metrics/save";
            public static final String SAVE_CAMPAIGN_DATEWISE_METRICS = "/campaign_datewise_metrics/save";
            public static final String GET_CAMPAIGN_CATALOG_METRICS = "/campaign_catalog_metrics/get";
            public static final String CAMPAIGN_PERFORMANCE_MIGRATE = "/campaign_performance/migrate";
            public static final String GET_CAMPAIGN_DATEWISE_METRICS = "/campaign_datewise_metrics/get";
            public static final String BACKFILL_CAMPAIGN_CATALOG_DAY_PERFORMANCE_EVENT = "/backfill/campaign_catalog_day_performance_events";
            public static final String SEND_BUDGET_EXHAUSTED_EVENT = "/budget_exhausted_event/send";
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

        public static class CAMPAIGN_PERFORMANCE_ES_INDEXING {
            public static final String MONITOR_CODE = "#{${scheduler.campaign_performance_es_indexing.monitor.code}}";
            public static final String CRON_EXPRESSION = "#{${scheduler.campaign_performance_es_indexing.cron.expression}}";
            public static final String ENABLE_SCHEDULER = "#{${scheduler.campaign_performance_es_indexing.enable}}";
            public static final String BATCH_SIZE = "#{${scheduler.campaign_performance_es_indexing.batch.size}}";
        }

        public static class ADS_DEDUCTION_CAMPAIGN_SUPPLIER {
            public static final String MONITOR_CODE = "#{${scheduler.ads.deduction.campaign.supplier.monitor.code}}";
            public static final String CRON_EXPRESSION = "#{${scheduler.ads.deduction.campaign.supplier.cron.expression}}";
            public static final String ENABLE_SCHEDULER = "#{${scheduler.ads.deduction.campaign.supplier.enable}}";
            public static final String BATCH_SIZE = "#{${scheduler.ads.deduction.campaign.supplier.batch.size}}";
            public static final String PROCESS_BATCH_SIZE = "#{${scheduler.ads.deduction.campaign.supplier.process.batch.size}}";
        }
    }

    public static class PrismEventNames {
        public static final String AD_INTERACTIONS = "ad_interaction_events";
        public static final String DAY_WISE_PERF_EVENTS = "campaign_catalog_day_performance_events";
        public static final String HBASE_PRESTO_TABLE_NAME = "scrap.campaign_catalog_reconciled_metrics";
    }

    public static class DefaultRealEstateMetaData {
        public static final String ORIGIN = "default_origin";
        public static final String SCREEN = "default_screen";
    }

    public static class AdWidgets {
        public static final String ORIGIN_SEARCH = "search_widget";
        public static final String SCREEN_TOP_OF_SEARCH = "top_of_search";
        public static final String SCREEN_MID_FEED_SEARCH = "mid_feed_After_slot_%s";
        public static final String TOP_OF_SEARCH_CPC_MULTIPLIER = "${ad_widget_top_of_search_cpc_multiplier}";

        public static final String ORIGIN_PDP = "pdp_widget";
        public static final String PDP_CPC_MULTIPLIER = "${ad_widget_pdp_cpc_multiplier}";
        public static final String SCREEN_PDP = "pdp_widget_%s";

    }

    public static class DailyBudgetConstants {
        public static final String TIME_FORMAT = "HH:mm:ss";
    }

    public static class ESConstants {
        public static final String BY_CAMPAIGN = "by_campaign";
        public static final String BY_CATALOG = "by_catalog";
        public static final String BY_DATE = "by_date";
        public static final String DAY_DATE_FORMAT = "yyyy-MM-dd";
        public static final String MONTH_DATE_FORMAT = "yyyy-MM";
        public static final String TOTAL_VIEWS = "total_views";
        public static final String TOTAL_CLICKS = "total_clicks";
        public static final String TOTAL_SHARES = "total_shares";
        public static final String TOTAL_WISHLIST = "total_wishlist";
        public static final String TOTAL_ORDERS = "total_orders";
        public static final String TOTAL_REVENUE = "total_revenues";
        public static final String TOTAL_BUDGET_UTILISED = "total_budget_utilised";
    }

    public static class ESFieldNames {
        public static final String CAMPAIGN_DATE = "date";
        public static final String BUDGET_UTILISED = "budget_utilised";
        public static final String ID = "_id";
        public static final String CAMPAIGN_ID = "campaign_id";
        public static final String SUPPLIER_ID = "supplier_id";
        public static final String CATALOG_ID = "catalog_id";

    }

    public static class Kafka {
        public static final String BUDGET_EXHAUSTED_TOPIC = "${kafka.budget_exhausted.topic}";

        public static final String ADS_COST_DEDUCTION_TOPIC = "${kafka.ads.cost.deduction.topic}";

        public static final String SUPPLIER_WEEKLY_BUDGET_EXHAUSTED_TOPIC = "${kafka.supplier.weekly.budget.exhausted.topic}";

        public static final String CATALOG_BUDGET_EXHAUSTED_TOPIC = "${kafka.catalog_budget_exhausted.topic}";
    }

    public static class FetchCampaignCatalog {
        public static final Integer DEFAULT_LIMIT = 100;
    }

    public static class CpcData {

        public static final String MULTIPLIED_CPC = "multipliedCpc";
        public static final String MULTIPLIER = "multiplier";
    }
}
