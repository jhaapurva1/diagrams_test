package com.meesho.cps.helper;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.mysql.CampaignPerformance;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.service.BillHandler;
import com.meesho.cps.service.external.AdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Service
@Slf4j
public class CampaignPerformanceHelper {

    @Autowired
    CampaignCatalogMetricsRepository campaignCatalogMetricsRepository;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    private AdBillFactory adBillFactory;

    @Autowired
    private AdService adService;

    public void updateCampaignPerformanceFromHbase(List<CampaignPerformance> entitiesToBeUpdated,
                                                   Map<Long, CampaignDetails> campaignIdAndCampaignDetailsMap) {
        List<Pair<Long, Long>> campaignCatalogIds = entitiesToBeUpdated.stream()
                .map(e -> Pair.of(e.getCampaignId(), e.getCatalogId()))
                .collect(Collectors.toList());

        List<CampaignCatalogMetrics> campaignCatalogMetricsList =
                campaignCatalogMetricsRepository.getAll(campaignCatalogIds);

        campaignCatalogMetricsList = campaignCatalogMetricsList.stream()
                .filter(x -> Objects.nonNull(x.getCampaignId()) && Objects.nonNull(x.getCatalogId()))
                .collect(Collectors.toList());

        Map<Pair<Long, Long>, CampaignCatalogMetrics> catalogMetricsMap = campaignCatalogMetricsList.stream()
                .collect(Collectors.toMap(ccm -> Pair.of(ccm.getCampaignId(), ccm.getCatalogId()),
                        Function.identity()));

        entitiesToBeUpdated.forEach(campaignPerformance -> {
            BillHandler billHandler = adBillFactory.getBillHandlerForBillVersion(
                    campaignIdAndCampaignDetailsMap.get(campaignPerformance.getCampaignId()).getBillVersion());

            CampaignCatalogMetrics campaignCatalogMetrics = catalogMetricsMap.get(
                    Pair.of(campaignPerformance.getCampaignId(), campaignPerformance.getCatalogId()));
            if (Objects.nonNull(campaignCatalogMetrics)) {
                campaignPerformance.setTotalViews(campaignCatalogMetrics.getViewCount());
                campaignPerformance.setTotalClicks(billHandler.getTotalInteractions(campaignCatalogMetrics).longValue());
                campaignPerformance.setBudgetUtilised(campaignCatalogMetrics.getBudgetUtilised());
            }
        });
    }

    public LocalDate getLocalDateForDailyCampaignFromLocalDateTime(LocalDateTime eventTime) {
        if (beforeResetTimeOfDailyBudgetForCampaign(eventTime)) {
            eventTime = eventTime.minusDays(1);
        }
        return eventTime.toLocalDate();
    }

    public boolean beforeResetTimeOfDailyBudgetForCampaign(LocalDateTime eventTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DailyBudgetConstants.TIME_FORMAT);
        LocalTime resetTime;
        try {
            resetTime = LocalTime.parse(applicationProperties.getDailyBudgetResetTime(), formatter);
        } catch (DateTimeParseException ex) {
            log.error("Error in parsing reset time for daily budget");
            resetTime = LocalTime.MIN;
        }
        return eventTime.toLocalTime().isBefore(resetTime);
    }

}
