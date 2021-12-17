package com.meesho.cps.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.scheduler.AbstractScheduler;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 29/11/21
 */
@Component
public class CampaignPerformanceESIndexingScheduler extends AbstractScheduler {

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Override
    public String getType() {
        return SchedulerType.CAMPAIGN_PERFORMANCE_ES_INDEXING.name();
    }

    @Override
    public Long process(String country, int limit, ZonedDateTime startTime, int processBatchSize) throws Exception {
        List<CampaignCatalogDate> campaignCatalogDates = updatedCampaignCatalogCacheDao.getAllUpdatedCampaignCatalogs();
        
        // group campaignCatalogDateIds on campaignCatalogMonth level
        Map<String, List<CampaignCatalogDate>> campaignCatalogDateGroupByMonth = campaignCatalogDates.stream()
                .collect(Collectors.groupingBy(campaignCatalogDate -> campaignCatalogDate.getCampaignId() + "_" +
                        campaignCatalogDate.getCatalogId() + "_" +
                        campaignCatalogDate.getDate().substring(0, campaignCatalogDate.getDate().length() - 3)));

        for (List<CampaignCatalogDate> campaignCatalogDateIdsOfAMonth : campaignCatalogDateGroupByMonth.values()) {
            kafkaService.sendMessage(ConsumerConstants.DayWisePerformanceEventsConsumer.TOPIC, null,
                    objectMapper.writeValueAsString(campaignCatalogDateIdsOfAMonth));
        }
        return (long) campaignCatalogDates.size();
    }

}
