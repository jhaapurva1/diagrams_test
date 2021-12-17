package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
public class ConsumerConstants {

    public static class CommonKafka {
        public static final String BOOTSTRAP_SERVERS = "${common.bootstrap.servers}";
        public static final String AVRO_SCHEMA_REGISTRY_URL = "${ingestion.consumer.avro.schema.registry.url}";
        public static final String CONTAINER_FACTORY = "commonKafkaListenerContainerFactory";
    }

    public static class CampaignUpdateConsumer {
        public static final String ID = "cpsCampaignCatalogUpdateEventConsumer";
        public static final String TOPIC = "adserver.campaign.update";
        public static final String RETRY_TOPIC = "cps.campaign.update_retry";
        public static final String DEAD_TOPIC = "cps.campaign.update_dead";
        public static final String AUTO_START = "${campaign-catalog-update.consumer.start}";
        public static final String CONCURRENCY = "${campaign-catalog-update.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${campaign-catalog-update.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${campaign-catalog-update.consumer.batch.size}";
    }

    public static class IngestionServiceKafka {
        public static final String BOOTSTRAP_SERVERS = "${ingestion.bootstrap.servers}";
        public static final String CONTAINER_FACTORY = "ingestionKafkaListenerContainerFactory";
        public static final String BATCH_CONTAINER_FACTORY = "ingestionBatchKafkaListenerContainerFactory";
    }

    public static class IngestionInteractionEvents {
        public static final String AD_CLICK_TOPIC = "ad_click";
        public static final String AD_SHARED_TOPIC = "ad_shared";
        public static final String AD_WISHLISTED_TOPIC = "ad_wishlisted";
        public static final String ANONYMOUS_AD_CLICK_TOPIC = "anonymous_ad_click";
        public static final String ANONYMOUS_AD_SHARED_TOPIC = "anonymous_ad_shared";
        public static final String ANONYMOUS_AD_WISHLISTED_TOPIC = "anonymous_ad_wishlisted";
    }

    public static class IngestionInteractionEventsConsumer {
        public static final String TOPICS = "${ingestion.interaction.event.consumer.topics}";
        public static final String ID = "adIngestionInteractionEventConsumer";
        public static final String AUTO_START = "${ingestion.interaction.event.consumer.start}";
        public static final String CONCURRENCY = "${ingestion.interaction.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${ingestion.interaction.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${ingestion.interaction.event.consumer.batch.size}";
    }


    public static class InteractionEventsConsumer {
        public static final String TOPIC = "ad_service.interactions";
        public static final String DEAD_QUEUE_TOPIC = "cps.adinteraction.dead.queue";
        public static final String ID = "adServiceInteractionEventsProcessor";
        public static final String AUTO_START = "${interaction.event.consumer.start}";
        public static final String CONCURRENCY = "${interaction.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${interaction.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${interaction.event.consumer.batch.size}";
    }

    public static class DayWisePerformanceEventsConsumer {
        public static final String TOPIC = "cps.dayWisePerf";
        public static final String DEAD_QUEUE_TOPIC = "cps.dayWisePerf.dead.queue";
        public static final String RETRY_TOPIC = "cps.dayWisePerf.retry";
        public static final String ID = "dayWisePerformanceEventsProcessor";
        public static final String AUTO_START = "${dayWisePerf.event.consumer.start}";
        public static final String CONCURRENCY = "${dayWisePerf.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${dayWisePerf.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${dayWisePerf.event.consumer.batch.size}";
    }

    public static class IngestionViewEventsConsumer {
        public static final String TOPICS = "${ingestion.view.event.consumer.topics}";
        public static final String ANONYMOUS_USER_TOPIC = "anonymous_ad_view";
        public static final String ID = "cpsIngestionViewEventConsumer";
        public static final String AUTO_START = "${ingestion.view.event.consumer.start}";
        public static final String CONCURRENCY = "${ingestion.view.event.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${ingestion.view.event.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${ingestion.view.event.consumer.batch.size}";
    }

    public static class DelayedRetryConsumer {
        public static final String TOPIC = "cps.delayed-retry";
        public static final String ID = "cpsServiceDelayedRetryConsumer";
        public static final String AUTO_START = "${delayed-retry.consumer.start}";
        public static final String CONCURRENCY = "${delayed-retry.consumer.concurrency}";
        public static final String MAX_POLL_INTERVAL_MS = "${delayed-retry.consumer.max.poll.interval.ms}";
        public static final String BATCH_SIZE = "${delayed-retry.consumer.batch.size}";
    }

}
