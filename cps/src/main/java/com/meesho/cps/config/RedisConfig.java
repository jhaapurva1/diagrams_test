package com.meesho.cps.config;

import com.meesho.cps.constants.DBConstants;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collections;

/**
 * @author shubham.aggarwal
 * 04/08/21
 */
@Configuration
@Slf4j
public class RedisConfig {

    @Value(DBConstants.Redis.HOST)
    private String host;

    @Value(DBConstants.Redis.PORT)
    private Integer port;

    @Value(DBConstants.Redis.COMMAND_TIMEOUT)
    private Long commandTimeout;

    @Value(DBConstants.Redis.SHUTDOWN_TIMEOUT)
    private Long shutdownTimeout;

    @Value(DBConstants.Redis.CLIENT_NAME)
    private String clientName;

    @Bean
    public ClusterClientOptions clientOptions() {
        return ClusterClientOptions.builder()
                .topologyRefreshOptions(
                        ClusterTopologyRefreshOptions.builder().enableAllAdaptiveRefreshTriggers().build())
                .autoReconnect(true)
                .build();
    }

    @Bean
    @Primary
    public ClientResources clientResources() {
        return DefaultClientResources.create();
    }

    @Bean
    public RedisClusterConfiguration redisConfiguration() {
        return new RedisClusterConfiguration(Collections.singletonList(host + ":" + port));
    }

    @Bean
    @Primary
    public LettuceClientConfiguration lettucePoolingClientConfiguration(ClientOptions clientOptions,
                                                                        ClientResources clientResources) {
        clientResources.eventBus().get().subscribe(e -> log.info("Redis Cluster Event : ".concat(e.toString())));
        return LettuceClientConfiguration.builder()
                .clientName(clientName)
                .clientOptions(clientOptions)
                .readFrom(ReadFrom.ANY)
                .commandTimeout(Duration.ofMillis(this.commandTimeout))
                .shutdownTimeout(Duration.ofMillis(this.shutdownTimeout))
                .clientResources(clientResources)
                .build();
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(RedisClusterConfiguration redisClusterConfiguration,
                                                         LettuceClientConfiguration lettuceClientConfiguration) {
        return new LettuceConnectionFactory(redisClusterConfiguration, lettuceClientConfiguration);

    }

    @Bean
    public RedisTemplate<String, byte[]> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    public RedisTemplate<String, Long> redisLongTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Long> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Long.class));
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
