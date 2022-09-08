package com.meesho.cps.config;

import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.constants.ProducerConstants;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
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


    @Value(ConsumerConstants.AdServiceKafka.BOOTSTRAP_SERVERS)
    private String adServiceBootstrapServers;

    @Value(ConsumerConstants.IngestionServiceConfluentKafka.BOOTSTRAP_SERVERS)
    private String ingestionConfluentKafkaBootstrapServers;

    @Value(ProducerConstants.PayoutServiceKafka.PAYOUT_BOOTSTRAP_SERVERS)
    private String payoutServers;

    @Value(ConsumerConstants.CommonKafka.AVRO_SCHEMA_REGISTRY_URL)
    private String avroSchemaRegistryUrl;

    @Value(ConsumerConstants.IngestionServiceConfluentKafka.AVRO_SCHEMA_REGISTRY_URL)
    private String confluentAvroSchemaRegistryUrl;

    @Value(ConsumerConstants.IngestionServiceConfluentKafka.SASL_USERNAME)
    private String ingestionConfluentKafkaSaslUsername;

    @Value(ConsumerConstants.IngestionServiceConfluentKafka.SASL_PASSWORD)
    private String ingestionConfluentKafkaSaslPassword;

    @Value(ConsumerConstants.IngestionServiceKafka.BOOTSTRAP_SERVERS)
    private String ingestionBootstrapServers;

    @Value(ConsumerConstants.IngestionServiceKafka.OFFSET_COMMIT_TIME)
    private String offsetCommitTime;

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

    private ConsumerFactory<String, String> ingestionConfluentKafkaConsumerFactory() {
        String confluent_jaas_config = String.format(
                "org.apache.kafka.common.security.plain.PlainLoginModule required\n" +
                        "    username=\"%s\"\n" +
                        "    password=\"%s\";",
                ingestionConfluentKafkaSaslUsername,ingestionConfluentKafkaSaslPassword);

        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ingestionConfluentKafkaBootstrapServers);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        configs.put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, confluentAvroSchemaRegistryUrl);
        configs.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        configs.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        configs.put(SaslConfigs.SASL_JAAS_CONFIG, confluent_jaas_config);
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

    @Bean(name = ConsumerConstants.IngestionServiceConfluentKafka.CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> ingestionConfluentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(ingestionConfluentKafkaConsumerFactory());
        concurrentKafkaListenerContainerFactory.setBatchListener(false);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        log.info("ingestion confluent kafka consumer created with configs {}",
                concurrentKafkaListenerContainerFactory.getConsumerFactory().getConfigurationProperties());
        return concurrentKafkaListenerContainerFactory;
    }

    @Bean(name = ConsumerConstants.IngestionServiceKafka.BATCH_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> ingestionBatchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(ingestionKafkaConsumerFactory());
        concurrentKafkaListenerContainerFactory.setBatchListener(true);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        log.info("ingestion kafka consumer created with configs {}",
                concurrentKafkaListenerContainerFactory.getConsumerFactory().getConfigurationProperties());
        return concurrentKafkaListenerContainerFactory;
    }

    @Bean(name = ConsumerConstants.IngestionServiceConfluentKafka.BATCH_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> ingestionBatchConfluentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(ingestionConfluentKafkaConsumerFactory());
        concurrentKafkaListenerContainerFactory.setBatchListener(true);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);


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

    private ConsumerFactory<String, String> adServiceKafkaConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, adServiceBootstrapServers);
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

    @Bean(name = ConsumerConstants.AdServiceKafka.CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String> adServiceKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> concurrentKafkaListenerContainerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        concurrentKafkaListenerContainerFactory.setConsumerFactory(adServiceKafkaConsumerFactory());
        concurrentKafkaListenerContainerFactory.setBatchListener(false);
        concurrentKafkaListenerContainerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);

        log.info("Ad Service kafka consumer created with configs {}",
                concurrentKafkaListenerContainerFactory.getConsumerFactory().getConfigurationProperties());
        return concurrentKafkaListenerContainerFactory;
    }

    @Bean
    @Primary
    public Map<String, Object> producerConfigs() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, adServiceBootstrapServers);
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

    @Bean(ProducerConstants.PayoutServiceKafka.PRODUCER_PAYOUT_CONFIG)
    public Map<String, Object> payoutProducerConfig() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, payoutServers);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return configs;
    }

    @Bean(ProducerConstants.PayoutServiceKafka.PRODUCER_PAYOUT_FACTORY)
    public DefaultKafkaProducerFactory<String, String> payoutProducerFactory() {
        return new DefaultKafkaProducerFactory<>(payoutProducerConfig());
    }

    @Bean(ProducerConstants.PayoutServiceKafka.PAYOUT_KAFKA_TEMPLATE)
    public KafkaTemplate<String, String> payoutKafkaTemplate() {
        return new KafkaTemplate<>(payoutProducerFactory());
    }

}