package com.meesho.cps.config;

import com.meesho.cps.constants.DBConstants;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class PubSubStandaloneRedisConfig {

    @Value(DBConstants.Redis.PUB_SUB_STANDALONE_HOST)
    private String host;

    @Value(DBConstants.Redis.PUB_SUB_STANDALONE_PORT)
    private Integer port;

    @Value(DBConstants.Redis.PUB_SUB_STANDALONE_PASSWORD)
    private String password;

    @Bean("pubSubStandaloneConfig")
    public RedisStandaloneConfiguration pubSubRedisConfiguration() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(host, port);
        redisStandaloneConfiguration.setPassword(password);
        return redisStandaloneConfiguration;
    }

    @Bean("pubSubStandaloneConnectionFactory")
    public RedisConnectionFactory pubSubRedisConnectionFactory(
            @Qualifier("pubSubStandaloneConfig") RedisStandaloneConfiguration redisStandaloneConfiguration) {
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean("pubSubStandaloneRedisTemplate")
    public RedisTemplate<String, byte[]> pubSubRedisTemplate(
            @Qualifier("pubSubStandaloneConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, byte[]> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setEnableDefaultSerializer(false);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

}
