package com.meesho.cps.db.redis.dao;

import com.meesho.cps.constants.AdUserInteractionType;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public interface UserCatalogInteractionCacheDao {

    Long get(String userId, Long id, String origin, String screen, AdUserInteractionType type);

    void set(String userId, Long id, String origin, String screen, Long timestamp, AdUserInteractionType type);

}
