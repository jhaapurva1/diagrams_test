package com.meesho.cps.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.CpcData;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.constants.UserInteractionType;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.kafka.BudgetExhaustedEvent;
import com.meesho.cps.data.entity.kafka.CatalogBudgetExhaustEvent;
import com.meesho.cps.data.entity.kafka.SupplierWeeklyBudgetExhaustedEvent;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CatalogCPCDiscount;
import com.meesho.cps.data.entity.mongodb.collection.SupplierWeekWiseMetrics;
import com.meesho.cps.db.mongodb.dao.*;
import com.meesho.cps.service.KafkaService;
import com.meesho.cps.service.external.PrismService;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private CampaignDateWiseMetricsDao campaignDateWiseMetricsDao;

    @Autowired
    private CampaignMetricsDao campaignMetricsDao;

    @Autowired
    private SupplierWeekWiseMetricsDao supplierWeekWiseMetricsDao;

    @Autowired
    private CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Autowired
    private CatalogCPCDiscountDao catalogCPCDiscountDao;

    @Autowired
    KafkaService kafkaService;

    @Autowired
    ObjectMapper objectMapper;

    @Value(Constants.Kafka.BUDGET_EXHAUSTED_MQ_ID)
    Long budgetExhaustedMqID;

    @Value(Constants.Kafka.SUPPLIER_WEEKLY_BUDGET_EXHAUSTED_TOPIC)
    private String suppliersWeeklyBudgetExhaustedTopic;

    @Value(Constants.Kafka.CATALOG_BUDGET_EXHAUSTED_TOPIC)
    private String catalogBudgetExhaustedTopic;

    public void publishPrismEvent(AdInteractionPrismEvent adInteractionPrismEvent) {
        log.info("publishPrismEvent: {}", adInteractionPrismEvent);
        List<AdInteractionPrismEvent> prismEvents = new ArrayList<>(Arrays.asList(adInteractionPrismEvent));
//        prismService.publishEvent(Constants.PrismEventNames.AD_INTERACTIONS, prismEvents);
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
        CampaignType campaignType = CampaignType.fromValue(campaignDetails.getCampaignType());
        BigDecimal budgetUtilised = BigDecimal.ZERO;
        if (CampaignType.DAILY_BUDGET.equals(campaignType)
                || CampaignType.SMART_CAMPAIGN.equals(campaignType)) {
            CampaignDateWiseMetrics campaignDateWiseMetrics = campaignDateWiseMetricsDao.findByCampaignIdAndDate(campaignDetails.getCampaignId(), eventDate.toString());
            if (Objects.isNull(campaignDateWiseMetrics)) {
                campaignDateWiseMetricsDao.save(CampaignDateWiseMetrics.builder().campaignId(campaignDetails.getCampaignId()).date(eventDate.toString()).budgetUtilised(BigDecimal.ZERO).build());
            } else {
                budgetUtilised = campaignDateWiseMetrics.getBudgetUtilised();
            }
        } else {
            CampaignMetrics campaignMetrics = campaignMetricsDao.findByCampaignId(campaignDetails.getCampaignId());
            if (Objects.isNull(campaignMetrics)) {
                campaignMetricsDao.save(CampaignMetrics.builder().campaignId(campaignDetails.getCampaignId()).budgetUtilised(BigDecimal.ZERO).build());
            } else {
                budgetUtilised = campaignMetrics.getBudgetUtilised();
            }
        }
        return budgetUtilised;
    }

    private BigDecimal getAndInitialiseSupplierWeeklyUtilisedBudget(Long supplierId, LocalDate weekStartDate) {
        log.info("get and initialize supplier weekly budget utilized: {} {}", supplierId, weekStartDate);
        BigDecimal budgetUtilised = BigDecimal.ZERO;
        SupplierWeekWiseMetrics supplierWeekWiseMetrics = supplierWeekWiseMetricsDao.findBySupplierIdAndWeekStartDate(supplierId, weekStartDate.toString());
        if (Objects.isNull(supplierWeekWiseMetrics)) {
            supplierWeekWiseMetricsDao.save(SupplierWeekWiseMetrics.builder().supplierId(supplierId).budgetUtilised(BigDecimal.ZERO)
                    .weekStartDate(weekStartDate.toString()).build());
        } else {
            budgetUtilised = supplierWeekWiseMetrics.getBudgetUtilised();
        }
        return budgetUtilised;
    }

    public void sendBudgetExhaustedEvent(Long campaignId, Long catalogId) {
        BudgetExhaustedEvent budgetExhaustedEvent = BudgetExhaustedEvent.builder().catalogId(catalogId).campaignId(campaignId).build();
        try {
//            kafkaService.sendMessageToMq(budgetExhaustedMqID, String.valueOf(campaignId),
//                    objectMapper.writeValueAsString(budgetExhaustedEvent));
        } catch (Exception e) {
            log.error("Exception while sending budgetExhausted event {}", budgetExhaustedEvent, e);
        }
    }

    public void sendCatalogBudgetExhaustEvent(Long campaignId, Long catalogId) {
        CatalogBudgetExhaustEvent catalogBudgetExhaustEvent = CatalogBudgetExhaustEvent.builder().campaignId(campaignId).catalogId(catalogId).build();
        try {
//            kafkaService.sendMessage(catalogBudgetExhaustedTopic, String.valueOf(catalogId),
//                    objectMapper.writeValueAsString(catalogBudgetExhaustEvent));
        } catch (Exception e) {
            log.error("Exception while sending catalogBudgetExhausted event {}", catalogBudgetExhaustEvent, e);
        }
    }

    public void sendSupplierBudgetExhaustedEvent(Long supplierId, Long catalogId) {
        SupplierWeeklyBudgetExhaustedEvent supplierWeeklyBudgetExhaustedEvent =
                SupplierWeeklyBudgetExhaustedEvent.builder().supplierId(supplierId).catalogId(catalogId).build();
        try {
//            kafkaService.sendMessage(suppliersWeeklyBudgetExhaustedTopic, String.valueOf(supplierId),
//                    objectMapper.writeValueAsString(supplierWeeklyBudgetExhaustedEvent));
        } catch (Exception e) {
            log.error("Exception while sending supplierWeeklyBudgetExhausted event {}", supplierWeeklyBudgetExhaustedEvent, e);
        }
    }

    public BudgetUtilisedData modifyAndGetBudgetUtilised(BigDecimal cpc, Long supplierId, Long campaignId, Long catalogId, LocalDate date,
                                                                   CampaignType campaignType, String eventName) {
        log.info("modifyAndGetBudgetUtilised: {} {} {} {} {} {}", cpc, campaignId, catalogId, date, campaignType);
        BigDecimal catalogBudgetUtilised = campaignCatalogDateMetricsDao.incrementBudgetUtilisedAndInteractionCount(supplierId, campaignId, catalogId, date.toString(), cpc, getInteractionTypeFromEventName(eventName));
        BigDecimal campaignBudgetUtilised = null;
        if (CampaignType.DAILY_BUDGET.equals(campaignType)
                || CampaignType.SMART_CAMPAIGN.equals(campaignType)) {
            campaignBudgetUtilised = campaignDateWiseMetricsDao.incrementCampaignDailyBudgetUtilised(campaignId, date.toString(), cpc);
        }
        else {
            campaignBudgetUtilised = campaignMetricsDao.incrementCampaignBudgetUtilised(campaignId, cpc);
        }
        return BudgetUtilisedData.builder().catalogBudgetUtilised(catalogBudgetUtilised).campaignBudgetUtilised(campaignBudgetUtilised).build();
    }

    public BigDecimal modifyAndGetSupplierWeeklyBudgetUtilised(Long supplierId, LocalDate weekStartDate, BigDecimal cpc) {
        log.info("modifyAndGetSupplierWeeklyBudgetUtilised: {} {} {}", supplierId, weekStartDate, cpc);
        return supplierWeekWiseMetricsDao.incrementSupplierWeeklyBudgetUtilised(supplierId, weekStartDate.toString(), cpc);
    }

    private UserInteractionType getInteractionTypeFromEventName(String eventName) {
        switch (eventName) {
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_SHARED_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_SHARED_EVENT_NAME:
                return UserInteractionType.SHARE;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_WISHLISTED_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_WISHLISTED_EVENT_NAME:
                return UserInteractionType.WISHLIST;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_CLICK_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_CLICK_EVENT_NAME:
                return UserInteractionType.CLICK;
        }
        return null;
    }

    public void incrementInteractionCount(Long supplierId, Long campaignId, Long catalogId, LocalDate date, String eventName) {
        log.info("supplierId {}, campaignId {}, catalogId {}, date{}, eventName {}", supplierId, campaignId, catalogId, date, eventName);
        // CAUTION: Please do not change the order of cases here since action is the combination of multiple cases
        switch (eventName) {
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_SHARED_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_SHARED_EVENT_NAME:
                campaignCatalogDateMetricsDao.incrementInteractionCount(supplierId, campaignId, catalogId, date.toString(), UserInteractionType.SHARE);
                break;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_WISHLISTED_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_WISHLISTED_EVENT_NAME:
                campaignCatalogDateMetricsDao.incrementInteractionCount(supplierId, campaignId, catalogId, date.toString(), UserInteractionType.WISHLIST);
                break;
            case ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_CLICK_EVENT_NAME:
            case ConsumerConstants.IngestionInteractionEvents.AD_CLICK_EVENT_NAME:
                campaignCatalogDateMetricsDao.incrementInteractionCount(supplierId, campaignId, catalogId, date.toString(), UserInteractionType.CLICK);
                break;
        }
    }
    //This returns the cpc to be considered for charging.
    public BigDecimal getChargeableCpc(BigDecimal servingTimeCpc, CampaignDetails campaignDetails, Long catalogId) {
        BigDecimal chargeableCPC = Objects.isNull(servingTimeCpc) ? campaignDetails.getCpc() : servingTimeCpc;
        CatalogCPCDiscount catalogCPCDiscount = catalogCPCDiscountDao.get(catalogId);
        if (Objects.nonNull(catalogCPCDiscount) && Objects.nonNull(chargeableCPC)
                && catalogCPCDiscount.getDiscount().compareTo(BigDecimal.ZERO) >= 0 && catalogCPCDiscount.getDiscount().compareTo(BigDecimal.ONE) <= 0) {
            chargeableCPC = chargeableCPC.multiply(BigDecimal.ONE.subtract(catalogCPCDiscount.getDiscount()));
        }
        return chargeableCPC;
    }

    public HashMap<String, BigDecimal> getMultipliedCpcData(BigDecimal chargeableCpc,
        String realEstate, WidgetEventHelper widgetEventHelper) {
        HashMap<String, BigDecimal> multipliedCpcData = new HashMap<>();
        if (Objects.isNull(chargeableCpc)) {
            multipliedCpcData.put(CpcData.MULTIPLIED_CPC, null);
            multipliedCpcData.put(CpcData.MULTIPLIER, null);
            return multipliedCpcData;
        }
        BigDecimal multipliedCpc = chargeableCpc;
        BigDecimal multiplier = BigDecimal.ONE;
        if (Objects.nonNull(widgetEventHelper)) {
            multipliedCpc = chargeableCpc.multiply(widgetEventHelper.getCpcMultiplier());
            multiplier = widgetEventHelper.getCpcMultiplier();
        }
        multipliedCpcData.put(CpcData.MULTIPLIED_CPC, multipliedCpc);
        multipliedCpcData.put(CpcData.MULTIPLIER, multiplier);
        return multipliedCpcData;
    }
}
