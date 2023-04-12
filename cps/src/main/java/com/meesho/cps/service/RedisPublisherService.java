package com.meesho.cps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.exception.MessageParsingException;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.constants.MessageIntent;
import com.meesho.cps.data.entity.redis.RedisPubSubEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.meesho.cps.constants.TelegrafConstants.PUB_SUB_TAGS;
import static com.meesho.cps.constants.TelegrafConstants.REDIS_PUB_SUB_KEY;

@Service
@Slf4j
public class RedisPublisherService {

    @Autowired
    @Qualifier("pubSubStandaloneRedisTemplate")
    private RedisTemplate<String, byte[]> redisTemplate;

    @Autowired
    ChannelTopic genericRedisNotificationsConsumerTopic;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Value(ConsumerConstants.GenericRedisNotificationsConsumer.PUBSUB_ENABLE)
    private boolean genericRedisNotificationsPublishSubscribeEnable;

    @Autowired
    @Qualifier("commonAsyncTaskExecutor")
    private Executor commonAsyncTaskExecutor;

    public void publishAdViewCampaignCatalogRefreshEvent(List<Long> catalogIds) {
        try {
            String adCatalogsData = catalogIds.stream().map(Object::toString)
                    .collect(Collectors.joining(ConsumerConstants.GenericRedisNotificationsConsumer.DELIMITER_USED));

            RedisPubSubEvent messageContentContainingCatalogs = RedisPubSubEvent
                    .builder()
                    .messageIntent(MessageIntent.UPDATE_AD_VIEW_CAMPAIGN_CATALOG)
                    .recordsAsString(adCatalogsData)
                    .build();

            sendMessageAsync(messageContentContainingCatalogs);

        } catch (Exception e) {
            log.error("Error in publishing AdViewCampaignCatalogRefreshEvent ", e);
        }
    }

    public void sendMessageAsync(RedisPubSubEvent redisPubSubEventObject) {
        if(!genericRedisNotificationsPublishSubscribeEnable)
            return;

        telegrafMetricsHelper.increment(REDIS_PUB_SUB_KEY, PUB_SUB_TAGS, "publish", redisPubSubEventObject.getMessageIntent().name());

        byte[] messageToBeSent;
        try {
            messageToBeSent = objectMapper.writeValueAsBytes(redisPubSubEventObject);
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException in publishAdViewCampaignCatalogRefreshEvent", e);
            throw new MessageParsingException(e.getMessage());
        }
        sendMessageAsync(genericRedisNotificationsConsumerTopic, messageToBeSent);
    }

    private void sendMessageAsync(ChannelTopic topic, byte[] message){
        commonAsyncTaskExecutor.execute(() -> sendMessage(topic, message));
    }

    private void sendMessage(ChannelTopic topic, byte[] message){
        redisTemplate.convertAndSend(topic.getTopic(), message);
    }

}
