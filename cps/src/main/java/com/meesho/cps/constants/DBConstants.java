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
        public static final String CAMPAIGN_CATALOG_KEY = "%s" + DELIMITER + "%s";

        public static class Tables {
            public static final String CAMPAIGN_PERFORMANCE_METRICS = "advertisement_campaign_performance";
        }

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
    }

}
