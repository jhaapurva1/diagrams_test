package com.meesho.cps.db.redis.dao.impl;

import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.meesho.cps.constants.DBConstants.Redis.UPDATED_CAMPAIGN_CATALOGS;

/**
 * @author shubham.aggarwal
 * 18/11/21
 */
@Service
@Slf4j
@DigestLogger(metricType = MetricType.REDIS, tagSet = "className=UpdateCampaignCatalogCacheDaoImpl")
public class UpdatedCampaignCatalogCacheDaoImpl implements UpdatedCampaignCatalogCacheDao {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Override
    public void add(List<CampaignCatalogDate> campaignCatalogDates) {
        if (CollectionUtils.isEmpty(campaignCatalogDates)) {
            return;
        }
        for (CampaignCatalogDate campaignCatalogDate : campaignCatalogDates) {
            Pair<String, String> setNameAndValue = getKey(campaignCatalogDate.getCampaignId(),
                    campaignCatalogDate.getCatalogId(), campaignCatalogDate.getDate());
            try {
                redisTemplate.opsForSet().add(setNameAndValue.getFirst(), setNameAndValue.getSecond());
            } catch (Exception e) {
                log.error("Failed to update key in redis set for campaignCatalogDate {}", campaignCatalogDate, e);
            }
        }
    }

    @Override
    public void delete(List<CampaignCatalogDate> campaignCatalogDates) {
        Map<Long, List<CampaignCatalogDate>> campaignCatalogDatesGroupByCampaignId = campaignCatalogDates.stream()
                .collect(Collectors.groupingBy(CampaignCatalogDate::getCampaignId));

        for (Map.Entry<Long, List<CampaignCatalogDate>> entry : campaignCatalogDatesGroupByCampaignId.entrySet()) {
            String setName = getSetName(entry.getKey());
            String[] keysArray = entry.getValue().stream()
                    .map(x -> getKey(x.getCampaignId(), x.getCatalogId(), x.getDate()).getSecond())
                    .toArray(String[]::new);
            try {
                redisTemplate.opsForSet().remove(setName, keysArray);
            } catch (Exception e) {
                log.error("Failed to delete from redis set, keys {}", keysArray, e);
            }
        }
    }

    @Override
    public List<CampaignCatalogDate> getAllUpdatedCampaignCatalogs() {
        try {
            List<CampaignCatalogDate> results = new ArrayList<>();
            for (int i = 0; i<applicationProperties.getRedisUpdatedCampaignCatalogsSetPartitionCount(); i++) {
                ScanOptions scanOptions = ScanOptions.scanOptions()
                        .count(applicationProperties.getRedisUpdatedCampaignCatalogSetBatchSize())
                        .build();
                Cursor<String> cursor = redisTemplate.opsForSet()
                        .scan(String.format(UPDATED_CAMPAIGN_CATALOGS, i), scanOptions);
                while (cursor.hasNext()) {
                    results.add(getObject(cursor.next()));
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to get keys from redis set", e);
            return new ArrayList<>();
        }
    }

    private String getSetName(Long campaignId) {
        int slotId = campaignId.intValue() % applicationProperties.getRedisUpdatedCampaignCatalogsSetPartitionCount();
        return String.format(UPDATED_CAMPAIGN_CATALOGS, slotId);
    }

    private Pair<String, String> getKey(Long campaignId, Long catalogId, String date) {
        String value = campaignId + "_" + catalogId + "_" + date;
        return Pair.of(getSetName(campaignId), value);
    }

    private CampaignCatalogDate getObject(String campaignCatalogDateString) {
        String[] splitString = campaignCatalogDateString.split("_");
        Long campaignId = Long.parseLong(splitString[0]);
        Long catalogId = Long.parseLong(splitString[1]);
        String date = splitString[2];
        return new CampaignCatalogDate(campaignId, catalogId, date);
    }

}
