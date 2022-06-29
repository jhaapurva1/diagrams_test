package com.meesho.cps.db.redis.dao;

import com.meesho.cps.constants.AdInteractionUserType;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public interface UserCatalogInteractionCacheDao {

    Long get(String userId, Long id, String origin, String screen, AdInteractionUserType type);

    void set(String userId, Long id, String origin, String screen, Long timestamp, AdInteractionUserType type);

}
