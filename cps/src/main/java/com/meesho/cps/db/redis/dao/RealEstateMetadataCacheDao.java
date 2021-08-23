package com.meesho.cps.db.redis.dao;

import com.meesho.commons.enums.Country;
import com.meesho.cps.data.entity.mysql.RealEstateMetadata;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public interface RealEstateMetadataCacheDao {

    void init();

    RealEstateMetadata get(String name, Country country);

    long syncUpdatedEntities(Country country);

}
