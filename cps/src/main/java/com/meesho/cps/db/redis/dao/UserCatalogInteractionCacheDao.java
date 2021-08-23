package com.meesho.cps.db.redis.dao;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public interface UserCatalogInteractionCacheDao {

    Long get(String userId, Long catalogId, String origin, String screen);

    void set(String userId, Long catalogId, String origin, String screen, Long timestamp);

}
