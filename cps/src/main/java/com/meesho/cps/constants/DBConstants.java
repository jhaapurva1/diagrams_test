package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
public class DBConstants {

    public static class Tables {
        public static final String CAMPAIGN_PERFORMANCE = "campaign_performance";
        public static final String REAL_ESTATE_METADATA = "real_estate_metadata";
        public static final String SCHEDULER_OFFSET = "scheduler_offset";
    }

    public static class Redshift {
        public static final String URL = "#{${redshift.url}}";
        public static final String USER_NAME = "#{${redshift.username}}";
        public static final String PASSWORD = "#{${redshift.password}}";

        public static final String DELIMITER = "_";
        public static final String CAMPAIGN_CATALOG_DATE_KEY = "%s" + DELIMITER + "%s" + DELIMITER + "%s";
        public static final String ADS_DEDUCTION_CAMPAIGN_KEY = "%s";

        public static class Tables {
            public static final String CAMPAIGN_PERFORMANCE_METRICS = "campaign_performance_ads_dod";
            public static final String ADS_DEDUCTION_CAMPAIGN_SUPPLIER = "ads_deduction_campaign_supplier";
        }

    }

    public static class PrestoTables {
        public static final String CAMPAIGN_PERFORMANCE_METRICS = "gold.campaign_performance_ads_dod";
        public static final String ADS_DEDUCTION_CAMPAIGN_SUPPLIER = "gold.ads_deduction_transaction_level";
    }

    public static class HBase {
        public static final String NAMESPACE = "adserver";
        public static final String KEY_SEPARATOR = ":";

        public static final String ZOOKEEPER_HOST = "${hbase.zookeeper.host}";
        public static final String ZOOKEEPER_PORT = "${hbase.zookeeper.port}";
        public static final String ZOOKEEPER_RECOVERY_RETRIES = "${hbase.zookeeper.recovery.retries}";

        public static final String CLIENT_PAUSE = "${hbase.client.pause}";
        public static final String CLIENT_RETRIES = "${hbase.client.retries}";
        public static final String RPC_TIMEOUT = "${hbase.rpc.timeout}";
    }

    public static class Redis {
        public static final String HOST = "${redis.host}";
        public static final String PORT = "${redis.port}";
        public static final String COMMAND_TIMEOUT = "${redis.command.timeout}";
        public static final String SHUTDOWN_TIMEOUT = "${redis.shutdown.timeout}";
        public static final String CLIENT_NAME = "${redis.client.name}";
        public static final String NAMESPACE = "adserver";
        public static final String REDIS_KEY_DELIMITER = ":";

        public static final String USER_CATALOG_INTERACTIONS_PREFIX =
                NAMESPACE + REDIS_KEY_DELIMITER + "ucin" + REDIS_KEY_DELIMITER + "%s" + REDIS_KEY_DELIMITER + "%s" +
                        REDIS_KEY_DELIMITER + "%s" + REDIS_KEY_DELIMITER + "%s";

        public static final String UPDATED_CAMPAIGN_CATALOGS = "ucc" + "_{%s}";
    }

    public static class ElasticSearch {
        public static final String ID = "id";
        public static final String CAMPAIGN_ID = "campaign_id";
        public static final String CATALOG_ID = "catalog_id";
        public static final String SUPPLIER_ID = "supplier_id";
        public static final String DATE = "date";
        public static final String MONTH = "month";
        public static final String CLICKS = "clicks";
        public static final String VIEWS = "views";
        public static final String SHARES = "shares";
        public static final String WISHLIST = "wishlist";
        public static final String ORDERS = "orders";
        public static final String REVENUE = "revenue";
        public static final String BUDGET_UTILISED = "budget_utilised";
    }

}
