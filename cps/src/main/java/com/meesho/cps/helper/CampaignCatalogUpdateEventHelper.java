package com.meesho.cps.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.kafka.CampaignCatalogUpdateEvent;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Service
@Slf4j
public class CampaignCatalogUpdateEventHelper {

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private ObjectMapper objectMapper;

    public void onCampaignCatalogUpdate(CampaignCatalogUpdateEvent.CatalogData catalogData) throws Exception {
        log.info("Received CampaignCatalogUpdateEvent event {}", catalogData);
        Long catalogId = catalogData.getId();
        Long campaignId = catalogData.getCampaignId();
        LocalDate date = LocalDate.now();
        CampaignCatalogDate campaignCatalogDate = new CampaignCatalogDate(campaignId, catalogId, date.toString());
        // push kafka event here to sync data in ES
        kafkaService.sendMessage(ConsumerConstants.DayWisePerformanceEventsConsumer.TOPIC, null,
                objectMapper.writeValueAsString(Arrays.asList(campaignCatalogDate)));
    }

}
