package com.meesho.cps.config;

import com.meesho.cps.constants.ConsumerConstants;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value(ConsumerConstants.CommonKafka.BOOTSTRAP_SERVERS)
    private String commonBootstrapServers;

    @Value(ConsumerConstants.PayoutServiceKafka.PAYOUT_BOOTSTRAP_SERVERS)
    private String payoutServers;

    @Value(ConsumerConstants.CommonKafka.AVRO_SCHEMA_REGISTRY_URL)
    private String avroSchemaRegistryUrl;

    @Value(ConsumerConstants.IngestionServiceKafka.BOOTSTRAP_SERVERS)
    private String ingestionBootstrapServers;

    private ConsumerFactory<String, String> ingestionKafkaConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ingestionBootstrapServers);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        //configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        configs.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, avroSchemaRegistryUrl);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean(name = ConsumerConstants.IngestionServiceKafka.CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> ingestionKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(ingestionKafkaConsumerFactory());
        concurrentKafkaListenerContainerFactory.setBatchListener(false);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        log.info("ingestion kafka consumer created with configs {}",
                concurrentKafkaListenerContainerFactory.getConsumerFactory().getConfigurationProperties());
        return concurrentKafkaListenerContainerFactory;
    }

    @Bean(name = ConsumerConstants.IngestionServiceKafka.BATCH_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> ingestionBatchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(ingestionKafkaConsumerFactory());
        concurrentKafkaListenerContainerFactory.setBatchListener(true);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        log.info("ingestion kafka consumer created with configs {}",
                concurrentKafkaListenerContainerFactory.getConsumerFactory().getConfigurationProperties());
        return concurrentKafkaListenerContainerFactory;
    }

    private ConsumerFactory<String, String> commonKafkaConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, commonBootstrapServers);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean(name = ConsumerConstants.CommonKafka.CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> commonKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(commonKafkaConsumerFactory());
        concurrentKafkaListenerContainerFactory.setBatchListener(false);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        log.info("Common kafka consumer created with configs {}",
                concurrentKafkaListenerContainerFactory.getConsumerFactory().getConfigurationProperties());
        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    @Primary
    public Map<String, Object> producerConfigs() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, commonBootstrapServers);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return configs;
    }

    @Bean
    @Primary
    public DefaultKafkaProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    @Bean
    @Primary
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean(ConsumerConstants.PayoutServiceKafka.PRODUCER_PAYOUT_CONFIG)
    public Map<String, Object> payoutProducerConfig() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, payoutServers);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return configs;
    }

    @Bean(ConsumerConstants.PayoutServiceKafka.PRODUCER_PAYOUT_FACTORY)
    public DefaultKafkaProducerFactory<String, String> payoutProducerFactory() {
        return new DefaultKafkaProducerFactory<>(payoutProducerConfig());
    }

    @Bean(ConsumerConstants.PayoutServiceKafka.PAYOUT_KAFKA_TEMPLATE)
    public KafkaTemplate<String, String> payoutKafkaTemplate() {
        return new KafkaTemplate<>(payoutProducerFactory());
    }

}
