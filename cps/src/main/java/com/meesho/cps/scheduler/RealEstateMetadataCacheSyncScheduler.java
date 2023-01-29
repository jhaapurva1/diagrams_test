package com.meesho.cps.scheduler;

import com.meesho.ads.lib.scheduler.AbstractScheduler;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.db.redis.dao.RealEstateMetadataCacheDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@Component
public class RealEstateMetadataCacheSyncScheduler extends AbstractScheduler {

    @Autowired
    RealEstateMetadataCacheDao realEstateMetadataCacheDao;


    @Override
    public String getType() {
        return SchedulerType.REAL_ESTATE_METADATA_CACHE_SYNC.name();
    }

    @Override
    public Long process(String country, int limit, ZonedDateTime startTime, int processBatchSize) throws Exception {
        return realEstateMetadataCacheDao.syncUpdatedEntities(Country.valueOf(country));
    }

    @Override
    public Long process(int limit, ZonedDateTime startTime, int processBatchSize) throws Exception {
        return null;
    }

}
