package com.meesho.cps.db.redis.dao.impl;

import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.DBConstants;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Service
@Slf4j
@DigestLogger(metricType = MetricType.REDIS, tagSet = "className=userCatalogInteractionCache")
public class UserCatalogInteractionCacheDaoImpl implements UserCatalogInteractionCacheDao {

    @Autowired
    private RedisTemplate<String, Long> redisTemplate;

    @Autowired
    private ApplicationProperties applicationProperties;

    private String appendPrefix(String userId, Long catalogId, String origin, String screen) {
        return String.format(DBConstants.Redis.USER_CATALOG_INTERACTIONS_PREFIX, userId, catalogId, origin, screen);
    }

    @Override
    public Long get(String userId, Long catalogId, String origin, String screen) {
        String key = appendPrefix(userId, catalogId, origin, screen);
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Exception getting for key: {}", key, e);
            return null;
        }
    }

    @Override
    public void set(String userId, Long catalogId, String origin, String screen, Long timestamp) {
        String key = appendPrefix(userId, catalogId, origin, screen);
        try {
            redisTemplate.opsForValue()
                    .set(key, timestamp, applicationProperties.getUserCatalogInteractionTTLSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Exception getting for key: {}", key, e);
        }
    }

}
