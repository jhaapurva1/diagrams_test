package com.meesho.cps.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.scheduler.AbstractScheduler;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.service.KafkaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 29/11/21
 */
@Component
public class DayWisePerformanceEventsScheduler extends AbstractScheduler {

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Value(ConsumerConstants.DayWisePerformanceEventsConsumer.TOPIC)
    String dayWisePerformanceEventsConsumerTopic;

    @Value(ConsumerConstants.DayWisePerformanceEventsConsumer.CAMPAIGN_CATALOG_DATE_BATCH_SIZE)
    private Integer CAMPAIGN_CATALOG_DATE_BATCH_SIZE;

    @Override
    public String getType() {
        return SchedulerType.DAY_WISE_PERF_EVENTS.name();
    }

    @Override
    public Long process(String country, int limit, ZonedDateTime startTime, int processBatchSize) throws Exception {
        List<CampaignCatalogDate> campaignCatalogDates = updatedCampaignCatalogCacheDao.getAllUpdatedCampaignCatalogs();

        // group campaignCatalogDateIds on campaignCatalogMonth level
        Map<String, List<CampaignCatalogDate>> campaignCatalogDateGroupByMonth = campaignCatalogDates.stream()
                .collect(Collectors.groupingBy(campaignCatalogDate -> campaignCatalogDate.getCampaignId() + "_" +
                        campaignCatalogDate.getCatalogId() + "_" +
                        campaignCatalogDate.getDate().substring(0, campaignCatalogDate.getDate().length() - 3)));


        List<CampaignCatalogDate> campaignCatalogDateIds = new ArrayList<>();

        for (List<CampaignCatalogDate> campaignCatalogDateIdsOfAMonth : campaignCatalogDateGroupByMonth.values()) {
            if (campaignCatalogDateIds.size() < CAMPAIGN_CATALOG_DATE_BATCH_SIZE) {
                campaignCatalogDateIds.addAll(campaignCatalogDateIdsOfAMonth);
            } else {
                kafkaService.sendMessage(dayWisePerformanceEventsConsumerTopic, null,
                        objectMapper.writeValueAsString(campaignCatalogDateIds));
                campaignCatalogDateIds = campaignCatalogDateIdsOfAMonth;
            }
        }

        if(campaignCatalogDateIds.size()> 0)
            kafkaService.sendMessage(dayWisePerformanceEventsConsumerTopic, null,
                    objectMapper.writeValueAsString(campaignCatalogDateIds));

        return (long) campaignCatalogDates.size();
    }

    @Override
    public Long process(int limit, ZonedDateTime startTime, int processBatchSize) throws Exception {
        return null;
    }

}
