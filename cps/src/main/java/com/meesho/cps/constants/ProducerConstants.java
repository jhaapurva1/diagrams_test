package com.meesho.cps.constants;

public class ProducerConstants {

    public static class PayoutServiceKafka {
        public static final String PAYOUT_BOOTSTRAP_SERVERS = "${payout.bootstrap.servers}";
        public static final String PRODUCER_PAYOUT_CONFIG = "producer-payout-config";
        public static final String PRODUCER_PAYOUT_FACTORY = "producer-payout-factory";
        public static final String PAYOUT_KAFKA_TEMPLATE = "payout-kafka-template";
    }

}
