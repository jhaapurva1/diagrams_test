package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.elasticsearch.ESDailyIndexDocument;
import com.meesho.cps.data.entity.elasticsearch.ESMonthlyIndexDocument;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.internal.CampaignCatalogDate;
import com.meesho.cps.db.elasticsearch.ElasticSearchRepository;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.service.external.PrismService;
import com.meesho.cps.transformer.ESDocumentTransformer;
import com.meesho.cps.transformer.PrismEventTransformer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DayWisePerformanceMetricsService {

    @Autowired
    private ElasticSearchRepository elasticSearchRepository;

    @Autowired
    private CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Autowired
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Autowired
    private AdService adService;

    @Autowired
    private PrismService prismService;

    public void handleMessage(List<CampaignCatalogDate> messages) throws Exception {
        log.info("Processing campaignCatalogDate event {}", messages);

        // delete keys from updatedCampaignCatalogSet. Deleting before reading from hbase, to avoid race conditions with live updates happening to the set
        updatedCampaignCatalogCacheDao.delete(messages);

        Set<Long> campaignIds = messages.stream().map(CampaignCatalogDate::getCampaignId).collect(Collectors.toSet());

        List<CampaignDetails> campaignDetails = adService.getCampaignMetadata(new ArrayList<>(campaignIds));
        if (CollectionUtils.isEmpty(campaignDetails)) {
            throw new ExternalRequestFailedException("Failed to get campaignMetadata from AdService");
        }
        Map<Long, CampaignDetails> campaignIdAndCampaignDetailsMap = campaignDetails.stream()
                .collect(Collectors.toMap(CampaignDetails::getCampaignId, Function.identity()));

        Map<String, List<CampaignCatalogDate>> messagesGroupedByCampaignCatalogMonth =  messages.stream()
                .collect(Collectors.groupingBy(campaignCatalogDate -> campaignCatalogDate.getCampaignId() + "_" +
                        campaignCatalogDate.getCatalogId() + "_" + getMonthPrefix(campaignCatalogDate.getDate())));

        List<ESMonthlyIndexDocument> monthlyIndexDocumentList = new ArrayList<>();
        List<ESDailyIndexDocument> dailyIndexDocumentList = new ArrayList<>();
        for (Map.Entry<String, List<CampaignCatalogDate>> entry : messagesGroupedByCampaignCatalogMonth.entrySet()) {
            // campaign, catalog and month should be same for all entry.values()
            Set<String> dateSet = getDateSet(entry.getValue());
            CampaignCatalogDate campaignCatalogDate = entry.getValue().get(0);
            Long campaignId = campaignCatalogDate.getCampaignId();
            Long catalogId = campaignCatalogDate.getCatalogId();
            if (!campaignIdAndCampaignDetailsMap.containsKey(campaignId)) {
                log.info("Couldn't get supplierId from ad server for campaignId {}, catalogId {} skipping indexing",
                        campaignId, catalogId);
                continue;
            }
            Long supplierId = campaignIdAndCampaignDetailsMap.get(campaignId).getSupplierId();
            String monthPrefix = getMonthPrefix(campaignCatalogDate.getDate());
            List<CampaignCatalogDateMetrics> campaignCatalogDateMetrics =
                    campaignCatalogDateMetricsRepository.scan(campaignId, catalogId, monthPrefix);
            monthlyIndexDocumentList.add(ESDocumentTransformer.getMonthIndexDocument(campaignCatalogDateMetrics,
                    monthPrefix, campaignId, catalogId, supplierId));
            dailyIndexDocumentList.addAll(ESDocumentTransformer.getDailyIndexDocument(campaignCatalogDateMetrics,
                    campaignId, catalogId, dateSet, supplierId));
        }

        elasticSearchRepository.bulkIndexMonthlyDocs(monthlyIndexDocumentList);
        elasticSearchRepository.bulkIndexDailyDocs(dailyIndexDocumentList);
        if (!CollectionUtils.isEmpty(dailyIndexDocumentList)) {
            prismService.publishEvent(Constants.PrismEventNames.DAY_WISE_PERF_EVENTS,
                    PrismEventTransformer.getDayWisePerformanceEvent(dailyIndexDocumentList));
        }
    }

    private Set<String> getDateSet(List<CampaignCatalogDate> messages) {
        Set<String> dateSet = new HashSet<>();
        messages.forEach(message -> {
            dateSet.add(message.getDate());
        });
        return dateSet;
    }

    private String getMonthPrefix(String date) {
        return date.substring(0, date.length() - 3);
    }

}
