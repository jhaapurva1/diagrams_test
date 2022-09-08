package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
public class ConsumerConstants {


    public static class CommonKafka {
        public static final String BOOTSTRAP_SERVERS = "${kafka.common.bootstrap.servers}";
        public static final String AVRO_SCHEMA_REGISTRY_URL = "${kafka.ingestion.consumer.avro.schema.registry.url}";
        public static final String CONTAINER_FACTORY = "commonKafkaListenerContainerFactory";
    }

    public static class AdServiceKafka {
        public static final String BOOTSTRAP_SERVERS = "${kafka.ad.service.bootstrap.servers}";
        public static final String AVRO_SCHEMA_REGISTRY_URL = "${kafka.ingestion.consumer.avro.schema.registry.url}";
        public static final String CONTAINER_FACTORY = "adServiceKafkaListenerContainerFactory";
    }

    public static class IngestionServiceKafka {
        public static final String BOOTSTRAP_SERVERS = "${kafka.ingestion.bootstrap.servers}";
        public static final String CONTAINER_FACTORY = "ingestionKafkaListenerContainerFactory";
        public static final String BATCH_CONTAINER_FACTORY = "ingestionBatchKafkaListenerContainerFactory";
        public static final String OFFSET_COMMIT_TIME = "${kafka.consumer.offset.commit.time}";
    }

    public static class IngestionServiceConfluentKafka {
        public static final String BOOTSTRAP_SERVERS = "${kafka.ingestion.confluent.bootstrap.servers}";
        public static final String CONTAINER_FACTORY = "confluentIngestionContainerFactory";
        public static final String BATCH_CONTAINER_FACTORY = "ingestionBatchConfluentKafkaListenerContainerFactory";
        public static final String SASL_USERNAME="${kafka.ingestion.confluent.sasl_config.username}";
        public static final String SASL_PASSWORD="${kafka.ingestion.confluent.sasl_config.password}";
        public static final String AVRO_SCHEMA_REGISTRY_URL = "${kafka.ingestion.confluent.consumer.avro.schema.registry.url}";

    }

    public static class IngestionInteractionEvents {
        public static final String AD_CLICK_EVENT_NAME= "ad_click";
        public static final String AD_SHARED_EVENT_NAME = "ad_shared";
        public static final String AD_WISHLISTED_EVENT_NAME = "ad_wishlisted";
        public static final String ANONYMOUS_AD_CLICK_EVENT_NAME = "anonymous_ad_click";
        public static final String ANONYMOUS_AD_SHARED_EVENT_NAME= "anonymous_ad_shared";
        public static final String ANONYMOUS_AD_WISHLISTED_EVENT_NAME = "anonymous_ad_wishlisted";
    }

    public static class CampaignUpdateConsumer {
        public static final String ID = "${kafka.campaign_catalog_update.event.consumer.id}";
        public static final String TOPIC = "${kafka.campaign_catalog_update.event.consumer.topic}";
        public static final String RETRY_TOPIC = "${kafka.campaign_catalog_update.event.consumer.retry.topic}";
        public static final String DEAD_TOPIC = "${kafka.campaign_catalog_update.event.consumer.dead.queue.topic}";
        public static final String AUTO_START = "${kafka.campaign_catalog_update.event.consumer.start}";
        public static final String CONCURRENCY = "${kafka.campaign_catalog_update.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${kafka.campaign_catalog_update.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${kafka.campaign_catalog_update.event.consumer.batch.size}";
    }

    public static class IngestionInteractionEventsConsumer {
        public static final String ID = "${kafka.ingestion.interaction.event.consumer.id}";
        public static final String CONFLUENT_CONSUMER_ID = "${kafka.ingestion.interaction.event.consumer.confluent.id}";
        public static final String TOPICS = "${kafka.ingestion.interaction.event.consumer.topics}";
        public static final String DEAD_QUEUE_TOPIC = "${kafka.ingestion.interaction.event.consumer.dead.queue.topic}";
        public static final String AUTO_START = "${kafka.ingestion.interaction.event.consumer.start}";
        public static final String CONCURRENCY = "${kafka.ingestion.interaction.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${kafka.ingestion.interaction.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${kafka.ingestion.interaction.event.consumer.batch.size}";
    }

    public static class InteractionEventsConsumer {
        public static final String ID = "${kafka.adserver.interaction.event.consumer.id}";
        public static final String TOPIC = "${kafka.adserver.interaction.event.consumer.topic}";
        public static final String DEAD_QUEUE_TOPIC = "${kafka.adserver.interaction.event.consumer.dead.queue.topic}";
        public static final String AUTO_START = "${kafka.adserver.interaction.event.consumer.start}";
        public static final String CONCURRENCY = "${kafka.adserver.interaction.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${kafka.adserver.interaction.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${kafka.adserver.interaction.event.consumer.batch.size}";
    }

    public static class DayWisePerformanceEventsConsumer {
        public static final String ID = "${kafka.dayWisePerf.event.consumer.id}";
        public static final String TOPIC = "${kafka.dayWisePerf.event.consumer.topic}";
        public static final String DEAD_QUEUE_TOPIC = "${kafka.dayWisePerf.event.consumer.dead.queue.topic}";
        public static final String RETRY_TOPIC = "${kafka.dayWisePerf.event.consumer.retry.topic}";
        public static final String AUTO_START = "${kafka.dayWisePerf.event.consumer.start}";
        public static final String CONCURRENCY = "${kafka.dayWisePerf.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${kafka.dayWisePerf.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${kafka.dayWisePerf.event.consumer.batch.size}";
    }

    public static class IngestionViewEventsConsumer {
        public static final String ID = "${kafka.ingestion.view.event.consumer.id}";
        public static final String CONFLUENT_CONSUMER_ID = "${kafka.ingestion.view.event.consumer.confluent.id}";
        public static final String TOPICS = "${kafka.ingestion.view.event.consumer.topics}";
        public static final String DEAD_QUEUE_TOPIC = "${kafka.ingestion.view.event.consumer.dead.queue.topic}";
        public static final String AUTO_START = "${kafka.ingestion.view.event.consumer.start}";
        public static final String CONCURRENCY = "${kafka.ingestion.view.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${kafka.ingestion.view.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${kafka.ingestion.view.event.consumer.batch.size}";
        public static final String BATCH_INTERVAL_MS = "${kafka.ingestion.view.event.consumer.batch.interval.ms}";
    }

    public static class DelayedRetryConsumer {
        public static final String ID = "${kafka.delayed_retry.event.consumer.id}";
        public static final String TOPIC = "${kafka.delayed_retry.event.consumer.topic}";
        public static final String AUTO_START = "${kafka.delayed_retry.event.consumer.start}";
        public static final String CONCURRENCY = "${kafka.delayed_retry.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${kafka.delayed_retry.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${kafka.delayed_retry.event.consumer.batch.size}";
    }

}
