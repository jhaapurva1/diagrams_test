package com.meesho.cps.db.redis.dao.impl;

import com.meesho.commons.enums.Country;
import com.meesho.cps.data.entity.mysql.RealEstateMetadata;
import com.meesho.cps.db.mysql.repository.RealEstateMetadataRepository;
import com.meesho.cps.db.redis.dao.RealEstateMetadataCacheDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Slf4j
@Service
public class RealEstateMetadataCacheDaoImpl implements RealEstateMetadataCacheDao {

    @Autowired
    RealEstateMetadataRepository realEstateMetadataRepository;

    private volatile Map<Country, Map<String, RealEstateMetadata>> cacheMap = new HashMap<>();

    private Map<Country, ZonedDateTime> lastUpdatedAtMap = new HashMap<>();

    @Override
    public void init() {
        for (Country country : Country.values()) {
            List<RealEstateMetadata> realEstateMetadataList =
                    realEstateMetadataRepository.findAllByCountryOrderByUpdatedAtAsc(country);
            cacheMap.putIfAbsent(country, new HashMap<>());
            for (RealEstateMetadata realEstateMetadata : realEstateMetadataList) {
                cacheMap.get(country).put(realEstateMetadata.getName(), realEstateMetadata);
                lastUpdatedAtMap.put(country, realEstateMetadata.getUpdatedAt());
            }
        }
    }


    @Override
    public RealEstateMetadata get(String name, Country country) {
        return cacheMap.get(country).get(name);
    }

    @Override
    public long syncUpdatedEntities(Country country) {
        long count = 0;
        ZonedDateTime lastUpdatedAt = lastUpdatedAtMap.get(country);
        Map<Country, Map<String, RealEstateMetadata>> newMap = new HashMap<>();
        List<RealEstateMetadata> realEstateMetadataList =
                realEstateMetadataRepository.findAllByUpdatedAtAfterAndCountryOrderByUpdatedAtAsc(lastUpdatedAt,
                        country);
        newMap.putIfAbsent(country, new HashMap<>());
        for (RealEstateMetadata realEstateMetadata : realEstateMetadataList) {
            newMap.get(country).put(realEstateMetadata.getName(), realEstateMetadata);
            lastUpdatedAtMap.put(country, realEstateMetadata.getUpdatedAt());
            count++;
        }
        cacheMap = newMap;
        return count;
    }

}
