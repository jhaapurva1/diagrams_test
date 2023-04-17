package com.meesho.cps.config;

import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.listener.redis.GenericRedisNotificationMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisMessageListenersConfig {

    @Value(ConsumerConstants.GenericRedisNotificationsConsumer.TOPIC)
    private String genericRedisNotificationsConsumerTopic;

    @Value(ConsumerConstants.GenericRedisNotificationsConsumer.PUBSUB_ENABLE)
    private boolean genericRedisNotificationsPublishSubscribeEnable;

    @Autowired
    private GenericRedisNotificationMessageListener genericRedisNotificationMessageListener;

    @Bean
    MessageListenerAdapter genericRedisMessageListenerAdapter() {
        return new MessageListenerAdapter(genericRedisNotificationMessageListener);
    }

    @Bean
    ChannelTopic genericRedisNotificationsConsumerTopic() {
        return new ChannelTopic(genericRedisNotificationsConsumerTopic);
    }

    @Bean
    RedisMessageListenerContainer redisContainer(@Qualifier("pubSubStandaloneConnectionFactory") RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container
                = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        if(genericRedisNotificationsPublishSubscribeEnable)
            container.addMessageListener(genericRedisMessageListenerAdapter(), genericRedisNotificationsConsumerTopic());
        return container;
    }
}
