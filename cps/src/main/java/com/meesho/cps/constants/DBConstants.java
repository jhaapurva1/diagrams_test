package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
public class DBConstants {

    public static class PrestoTables {
        public static final String CAMPAIGN_PERFORMANCE_METRICS = "gold.campaign_performance_ads_dod";
        public static final String CATALOG_CPC_DISCOUNT = "gold.ads_catalog_level_cpc_discount";
    }

    public static class MongoCollections {
        public static final String CAMPAIGN_CATALOG_DATE_METRICS_COLLECTION = "campaign-catalog-date-metrics-collection";
        public static final String CATALOG_CPC_DISCOUNT_COLLECTION = "catalog-cpc-discount-collection";
        public static final String SUPPLIER_WEEK_WISE_METRICS_COLLECTION = "supplier-weekly-metrics-collection";
        public static final String CAMPAIGN_DATE_WISE_METRICS_COLLECTION = "campaign-daily-metrics-collection";
        public static final String CAMPAIGN_METRICS_COLLECTION = "campaign-metrics-collection";
    }

    public static class Redis {
        public static final String HOST = "${redis.host}";
        public static final String PORT = "${redis.port}";
        public static final String PASSWORD = "${redis.password}";
        public static final String COMMAND_TIMEOUT = "${redis.command.timeout}";
        public static final String SHUTDOWN_TIMEOUT = "${redis.shutdown.timeout}";
        public static final String CLIENT_NAME = "${redis.client.name}";
        public static final String NAMESPACE = "adserver";
        public static final String REDIS_KEY_DELIMITER = ":";

        public static final String USER_CATALOG_INTERACTIONS_PREFIX =
                NAMESPACE + REDIS_KEY_DELIMITER + "ucin" + REDIS_KEY_DELIMITER + "%s" + REDIS_KEY_DELIMITER + "%s" +
                        REDIS_KEY_DELIMITER + "%s" + REDIS_KEY_DELIMITER + "%s" + REDIS_KEY_DELIMITER + "%s";

        public static final String UPDATED_CAMPAIGN_CATALOGS = "new_ucc" + "_{%s}";

        public static final String PUB_SUB_STANDALONE_HOST = "${redis.pub.sub.standalone.host}";
        public static final String PUB_SUB_STANDALONE_PASSWORD = "${redis.pub.sub.standalone.password}";
        public static final String PUB_SUB_STANDALONE_PORT = "${redis.pub.sub.standalone.port}";
    }

}
