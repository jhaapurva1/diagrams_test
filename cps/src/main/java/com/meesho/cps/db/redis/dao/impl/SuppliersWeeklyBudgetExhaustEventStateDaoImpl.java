package com.meesho.cps.db.redis.dao.impl;

import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.db.redis.dao.SuppliersWeeklyBudgetExhaustEventStateDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

public class SuppliersWeeklyBudgetExhaustEventStateDaoImpl implements SuppliersWeeklyBudgetExhaustEventStateDao {

    private static final String KEY_PREFIX = "weekly_budget_exhaust_event_";
    private static final String VALUE = "";

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Override
    public boolean isEventAlreadyFired(Long supplierId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(getKey(supplierId)));
    }

    @Override
    public void setEventAsFired(Long supplierId) {
        redisTemplate.opsForValue().set(getKey(supplierId), VALUE,
                applicationProperties.getSuppliersWeeklyBudgetExhaustEventTtlSeconds(), TimeUnit.SECONDS);
    }

    private String getKey(Long supplierId) {
        return KEY_PREFIX + supplierId;
    }

}
