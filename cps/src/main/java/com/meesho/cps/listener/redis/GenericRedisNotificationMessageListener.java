package com.meesho.cps.listener.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.cps.constants.MessageIntent;
import com.meesho.cps.data.entity.redis.RedisPubSubEvent;
import com.meesho.cps.db.caffeine.BaseLocalCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static com.meesho.cps.constants.TelegrafConstants.PUB_SUB_TAGS;
import static com.meesho.cps.constants.TelegrafConstants.REDIS_PUB_SUB_KEY;

@Component
@Slf4j
public class GenericRedisNotificationMessageListener implements MessageListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private List<BaseLocalCache> baseLocalCacheList;

    @Autowired
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Autowired
    @Qualifier("commonAsyncTaskExecutor")
    private Executor commonAsyncTaskExecutor;

    private Map<MessageIntent, BaseLocalCache> messageToNotificationServiceCacheMap;

    @PostConstruct
    public void init() {
        messageToNotificationServiceCacheMap = baseLocalCacheList.stream().collect(Collectors.toMap(x -> x.getMessageIntentType(), x -> x));
    }

    public void onMessage(Message message, byte[] pattern) {
        commonAsyncTaskExecutor.execute(() -> processMessage(message));
    }

    private void processMessage(Message message) {
        try {
            RedisPubSubEvent messageReceived = objectMapper.readValue(message.getBody(), RedisPubSubEvent.class);
            MessageIntent messageIntentReceived = messageReceived.getMessageIntent();
            if (messageToNotificationServiceCacheMap.containsKey(messageIntentReceived)) {
                telegrafMetricsHelper.increment(REDIS_PUB_SUB_KEY, PUB_SUB_TAGS, "listen", messageIntentReceived.name());
                BaseLocalCache notificationServiceMappedToTheMessage = messageToNotificationServiceCacheMap.get(messageIntentReceived);
                notificationServiceMappedToTheMessage.backFill(notificationServiceMappedToTheMessage.transformEvent(messageReceived.getRecordsAsString()));
            } else {
                log.debug("Message couldn't be mapped to any service - {}", messageReceived);
            }
        } catch (IOException e) {
            log.error("Exception caught while reading message in GenericRedisNotificationMessageListener - ", e);
        } catch (Exception e) {
            log.error("Exception caught while processing message in GenericRedisNotificationMessageListener - ", e);
        }
    }

}
