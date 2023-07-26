package com.meesho.cps.constants;


/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public class Constants {

    public static final String[] CAMPAIGN_CATALOG_DATE_FORMAT = new String[]{"campaign_id", "catalog_id", "date"};

    public static class API {
        public static final String BASE_PATH = "/api/v1";
        public static final Integer HBASE_BATCH_SIZE = 30;
        public static final String HEALTH_CHECK_ENDPOINT = "/health";
        public static final String MANUAL_SCHEDULER_START = "/api/v1/scheduler/start";

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
            public static final String INCREMENT_BUDGET_UTILISED = "/campaign_metrics/increment_budget_utilised";
            public static final String SAVE_CAMPAIGN_METRICS = "/campaign_metrics/save";
            public static final String SAVE_CAMPAIGN_CATALOG_METRICS = "/campaign_catalog_metrics/save";
            public static final String SAVE_CAMPAIGN_DATEWISE_METRICS = "/campaign_datewise_metrics/save";
            public static final String GET_CAMPAIGN_CATALOG_METRICS = "/campaign_catalog_metrics/get";
            public static final String CAMPAIGN_PERFORMANCE_MIGRATE = "/campaign_performance/migrate";
            public static final String GET_CAMPAIGN_DATEWISE_METRICS = "/campaign_datewise_metrics/get";
            public static final String BACKFILL_CAMPAIGN_CATALOG_DAY_PERFORMANCE_EVENT = "/backfill/campaign_catalog_day_performance_events";
            public static final String SAVE_CATALOG_CPC_DISCOUNT = "/catalog_cpc_discount/save";
            public static final String GET_CATALOG_CPC_DISCOUNT = "/catalog_cpc_discount/get";
            public static final String SEND_BUDGET_EXHAUSTED_EVENT = "/budget_exhausted_event/send";
            public static final String SEND_CATALOG_BUDGET_EXHAUSTED_EVENT = "/catalog_budget_exhausted_event/send";
            public static final String SEND_SUPPLIER_BUDGET_EXHAUSTED_EVENT = "/supplier_budget_exhausted_event/send";
            public static final String PRODUCE_KAFKA = "/produce_kafka_interaction_event";
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

        public static class DAY_WISE_PERF_EVENTS {
            public static final String MONITOR_CODE = "#{${scheduler.day.wise.perf.events.monitor.code}}";
            public static final String CRON_EXPRESSION = "#{${scheduler.day.wise.perf.events.cron.expression}}";
            public static final String ENABLE_SCHEDULER = "#{${scheduler.day.wise.perf.events.enable}}";
            public static final String BATCH_SIZE = "#{${scheduler.day.wise.perf.events.batch.size}}";
            public static final String PROCESS_BATCH_SIZE = "#{${scheduler.day.wise.perf.events.process.batch.size}}";
        }

        public static class CATALOG_CPC_DISCOUNT {
            public static final String MONITOR_CODE = "#{${scheduler.catalog_cpc_discount.monitor.code}}";
            public static final String CRON_EXPRESSION = "#{${scheduler.catalog_cpc_discount.cron.expression}}";
            public static final String ENABLE_SCHEDULER = "#{${scheduler.catalog_cpc_discount.enable}}";
            public static final String BATCH_SIZE = "#{${scheduler.catalog_cpc_discount.batch.size}}";
            public static final String PROCESS_BATCH_SIZE = "#{${scheduler.catalog_cpc_discount.process.batch.size}}";
        }

    }

    public static class PrismEventNames {
        public static final String AD_INTERACTIONS = "ad_interaction_events";
        public static final String DAY_WISE_PERF_EVENTS = "campaign_catalog_day_performance_events";
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

    public static class Kafka {
        public static final String BUDGET_EXHAUSTED_MQ_ID= "${kafka.budget_exhausted.mq.id}";

        public static final String INTERACTION_EVENT_MQ_ID= "${kafka.adserver.interaction.event.consumer.mq.id}";

        public static final String PRESTO_SCHEDULER_EVENT_MQ_ID = "${kafka.presto.scheduler.event.consumer.mq.id}";

        public static final String PRESTO_SCHEDULER_EVENT_RETRY_MQ_ID = "${kafka.presto.scheduler.event.retry.consumer.mq.id}";

        public static final String ADS_COST_DEDUCTION_TOPIC = "${kafka.ads.cost.deduction.topic}";

        public static final String SUPPLIER_WEEKLY_BUDGET_EXHAUSTED_MQ_ID = "${kafka.supplier.weekly.budget.exhausted.mq.id}";

        public static final String CAMPAIGN_REAL_ESTATE_BUDGET_EXHAUSTED_MQ_ID = "${kafka.campaign_real_estate_budget_exhausted.mq.id}";

        public static final String CATALOG_BUDGET_EXHAUSTED_MQ_ID = "${kafka.catalog_budget_exhausted.mq.id}";

        public static final String DELAYED_RETRY_EVENT_MQ_ID="${kafka.delayed_retry.event.consumer.mq.id}";

        public static final String AD_VIEW_CAMPAIGN_CATALOG_CACHE_UPDATE_EVENT_MQ_ID="${kafka.ad_view_campaign_catalog_cache_update.event.consumer.mq.id}";
        public static final String DAY_PERF_EVENT_MQ_ID="${kafka.dayWisePerf.event.consumer.mq.id}";
        public static final String DAY_PERF_EVENT_RETRY_MQ_ID="${kafka.dayWisePerf.event.consumer.retry.mq.id}";
    }

    public static class FetchCampaignCatalog {
        public static final Integer DEFAULT_LIMIT = 100;
    }

    public static class CpcData {

        public static final String MULTIPLIED_CPC = "multipliedCpc";
        public static final String MULTIPLIER = "multiplier";
    }
}
