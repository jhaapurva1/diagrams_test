package com.meesho.cps.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.kafka.BudgetExhaustedEvent;
import com.meesho.cps.data.entity.kafka.CatalogBudgetExhaustEvent;
import com.meesho.cps.data.entity.kafka.SupplierWeeklyBudgetExhaustedEvent;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
import com.meesho.cps.service.KafkaService;
import com.meesho.cps.service.external.PrismService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class InteractionEventAttributionHelper {
    @Autowired
    PrismService prismService;

    @Autowired
    ApplicationProperties applicationProperties;

    @Autowired
    CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Autowired
    CampaignMetricsRepository campaignMetricsRepository;

    @Autowired
    SupplierWeekWiseMetricsRepository supplierWeekWiseMetricsRepository;

    @Autowired
    CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Autowired
    KafkaService kafkaService;

    @Autowired
    ObjectMapper objectMapper;

    @Value(Constants.Kafka.BUDGET_EXHAUSTED_TOPIC)
    String budgetExhaustedTopic;

    @Value(Constants.Kafka.SUPPLIER_WEEKLY_BUDGET_EXHAUSTED_TOPIC)
    private String suppliersWeeklyBudgetExhaustedTopic;

    @Value(Constants.Kafka.CATALOG_BUDGET_EXHAUSTED_TOPIC)
    private String catalogBudgetExhaustedTopic;


    public void publishPrismEvent(AdInteractionPrismEvent adInteractionPrismEvent) {
        log.info("publishPrismEvent: {}", adInteractionPrismEvent);
        List<AdInteractionPrismEvent> prismEvents = new ArrayList<>(Arrays.asList(adInteractionPrismEvent));
        prismService.publishEvent(Constants.PrismEventNames.AD_INTERACTIONS, prismEvents);
    }

    /**
     * @param previousInteractionTime in millis
     * @param interactionTime         in millis
     * @return boolean
     */
    public boolean checkIfInteractionNeedsToBeConsidered(Long previousInteractionTime, Long interactionTime) {
        log.info("check if interaction needs to be considered: {} {}", previousInteractionTime, interactionTime);
        int windowTimeInMillis = applicationProperties.getUserCatalogInteractionWindowInSeconds() * 1000;
        return Objects.isNull(previousInteractionTime) || ((previousInteractionTime <= interactionTime) &&
                (interactionTime - previousInteractionTime >= windowTimeInMillis));
    }

    public boolean initialiseAndCheckIsBudgetExhausted(CampaignDetails campaignDetails, LocalDate weekStartDate, LocalDate eventDate, BigDecimal weeklyBudgetUtilisationLimit, Long catalogId) {
        BigDecimal supplierWeeklyBudgetUtilised = this.getAndInitialiseSupplierWeeklyUtilisedBudget(campaignDetails.getSupplierId(), weekStartDate);
        if (Objects.nonNull(weeklyBudgetUtilisationLimit) && supplierWeeklyBudgetUtilised.compareTo(weeklyBudgetUtilisationLimit) >= 0) {
            sendSupplierBudgetExhaustedEvent(campaignDetails.getSupplierId(), catalogId);
            return true;
        }
        BigDecimal campaignBudgetUtilised = this.getAndInitialiseCampaignBudgetUtilised(campaignDetails, eventDate);
        if (campaignBudgetUtilised.compareTo(campaignDetails.getBudget()) >= 0) {
            sendBudgetExhaustedEvent(campaignDetails.getCampaignId(), catalogId);
            return true;
        }
        return false;
    }

    private BigDecimal getAndInitialiseCampaignBudgetUtilised(CampaignDetails campaignDetails, LocalDate eventDate) {
        log.info("get and initialize campaign budget: {} {}", campaignDetails, eventDate);
        CampaignType campaignType = CampaignType.fromValue(campaignDetails.getCampaignType());
        BigDecimal budgetUtilised = BigDecimal.ZERO;
        if (CampaignType.DAILY_BUDGET.equals(campaignType)
                || CampaignType.SMART_CAMPAIGN.equals(campaignType)) {
            CampaignDatewiseMetrics campaignDatewiseMetrics = campaignDatewiseMetricsRepository.get(campaignDetails.getCampaignId(), eventDate);
            if (Objects.isNull(campaignDatewiseMetrics)) {
                campaignDatewiseMetricsRepository.put(CampaignDatewiseMetrics.builder().campaignId(campaignDetails.getCampaignId()).date(eventDate).budgetUtilised(BigDecimal.ZERO).build());
            } else {
                budgetUtilised = campaignDatewiseMetrics.getBudgetUtilised();
            }
        } else {
            CampaignMetrics campaignMetrics = campaignMetricsRepository.get(campaignDetails.getCampaignId());
            if (Objects.isNull(campaignMetrics)) {
                campaignMetricsRepository.put(CampaignMetrics.builder().campaignId(campaignDetails.getCampaignId()).budgetUtilised(BigDecimal.ZERO).build());
            } else {
                budgetUtilised = campaignMetrics.getBudgetUtilised();
            }
        }
        return budgetUtilised;
    }

    private BigDecimal getAndInitialiseSupplierWeeklyUtilisedBudget(Long supplierId, LocalDate weekStartDate) {
        log.info("get and initialize supplier weekly budget utilized: {} {}", supplierId, weekStartDate);
        BigDecimal budgetUtilised = BigDecimal.ZERO;
        SupplierWeekWiseMetrics supplierWeekWiseMetrics = supplierWeekWiseMetricsRepository.get(supplierId, weekStartDate);
        if (Objects.isNull(supplierWeekWiseMetrics)) {
            supplierWeekWiseMetricsRepository.put(SupplierWeekWiseMetrics.builder().supplierId(supplierId).budgetUtilised(BigDecimal.ZERO)
                    .weekStartDate(weekStartDate).build());
        } else {
            budgetUtilised = supplierWeekWiseMetrics.getBudgetUtilised();
        }
        return budgetUtilised;
    }

    public void sendBudgetExhaustedEvent(Long campaignId, Long catalogId) {
        BudgetExhaustedEvent budgetExhaustedEvent = BudgetExhaustedEvent.builder().catalogId(catalogId).campaignId(campaignId).build();
        try {
            kafkaService.sendMessage(budgetExhaustedTopic, String.valueOf(campaignId),
                    objectMapper.writeValueAsString(budgetExhaustedEvent));
        } catch (Exception e) {
            log.error("Exception while sending budgetExhausted event {}", budgetExhaustedEvent, e);
        }
    }

    public void sendCatalogBudgetExhaustEvent(Long campaignId, Long catalogId) {
        CatalogBudgetExhaustEvent catalogBudgetExhaustEvent = CatalogBudgetExhaustEvent.builder().campaignId(campaignId).catalogId(catalogId).build();
        try {
            kafkaService.sendMessage(catalogBudgetExhaustedTopic, String.valueOf(catalogId),
                    objectMapper.writeValueAsString(catalogBudgetExhaustEvent));
        } catch (Exception e) {
            log.error("Exception while sending catalogBudgetExhausted event {}", catalogBudgetExhaustEvent, e);
        }
    }

    public void sendSupplierBudgetExhaustedEvent(Long supplierId, Long catalogId) {
        SupplierWeeklyBudgetExhaustedEvent supplierWeeklyBudgetExhaustedEvent =
                SupplierWeeklyBudgetExhaustedEvent.builder().supplierId(supplierId).catalogId(catalogId).build();
        try {
            kafkaService.sendMessage(suppliersWeeklyBudgetExhaustedTopic, String.valueOf(supplierId),
                    objectMapper.writeValueAsString(supplierWeeklyBudgetExhaustedEvent));
        } catch (Exception e) {
            log.error("Exception while sending supplierWeeklyBudgetExhausted event {}", supplierWeeklyBudgetExhaustedEvent, e);
        }
    }

    public BudgetUtilisedData modifyAndGetBudgetUtilised(BigDecimal cpc, Long campaignId, Long catalogId, LocalDate date,
                                                                   CampaignType campaignType) {
        log.info("modifyAndGetBudgetUtilised: {} {} {} {} {} {}", cpc, campaignId, catalogId, date, campaignType);
        BigDecimal catalogBudgetUtilised = campaignCatalogDateMetricsRepository.incrementBudgetUtilised(campaignId, catalogId, date, cpc);
        BigDecimal campaignBudgetUtilised = null;
        if (CampaignType.DAILY_BUDGET.equals(campaignType)
                || CampaignType.SMART_CAMPAIGN.equals(campaignType)) {
            campaignBudgetUtilised = campaignDatewiseMetricsRepository.incrementBudgetUtilised(campaignId, date, cpc);
        }
        else {
            campaignBudgetUtilised = campaignMetricsRepository.incrementBudgetUtilised(campaignId, cpc);
        }
        return BudgetUtilisedData.builder().catalogBudgetUtilised(catalogBudgetUtilised).campaignBudgetUtilised(campaignBudgetUtilised).build();
    }

    public BigDecimal modifyAndGetSupplierWeeklyBudgetUtilised(Long supplierId, LocalDate weekStartDate, BigDecimal cpc) {
        log.info("modifyAndGetSupplierWeeklyBudgetUtilised: {} {} {}", supplierId, weekStartDate, cpc);
        return supplierWeekWiseMetricsRepository.incrementBudgetUtilised(supplierId, weekStartDate, cpc);
    }

    public void incrementInteractionCount(Long campaignId, Long catalogId, LocalDate date, String eventName) {
        log.info("campaignId {}, catalogId {}, date{}, eventName {}", campaignId, catalogId, date, eventName);
        // CAUTION: Please do not change the order of cases here since action is the combination of multiple cases
        switch (eventName) {
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_SHARED_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_SHARED_EVENT_NAME:
                campaignCatalogDateMetricsRepository.incrementSharesCount(campaignId, catalogId, date);
                break;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_WISHLISTED_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_WISHLISTED_EVENT_NAME:
                campaignCatalogDateMetricsRepository.incrementWishlistCount(campaignId, catalogId, date);
                break;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_CLICK_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_CLICK_EVENT_NAME:
                campaignCatalogDateMetricsRepository.incrementClickCount(campaignId, catalogId, date);
                break;
        }
    }

    public BigDecimal getChargeableCpc(BigDecimal cpc, CampaignDetails campaignDetails) {
        return Objects.nonNull(campaignDetails.getCpc()) ? campaignDetails.getCpc() : cpc;
    }
}
