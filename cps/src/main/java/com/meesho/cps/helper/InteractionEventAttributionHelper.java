package com.meesho.cps.helper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.constants.FeedType;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.constants.Constants.CpcData;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.constants.UserInteractionType;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.internal.CampaignBudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.*;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CatalogCPCDiscount;
import com.meesho.cps.data.entity.mongodb.collection.SupplierWeekWiseMetrics;
import com.meesho.cps.db.mongodb.dao.*;
import com.meesho.cps.service.KafkaService;
import com.meesho.cps.service.external.PrismService;

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

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

    @Value(Constants.Kafka.CAMPAIGN_REAL_ESTATE_BUDGET_EXHAUSTED_TOPIC)
    private String campaignRealEstateBudgetExhaustedTopic;

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

    public boolean initialiseAndCheckIsBudgetExhausted(CampaignDetails campaignDetails, LocalDate weekStartDate,
                                                       LocalDate eventDate, BigDecimal weeklyBudgetUtilisationLimit,
                                                       Long catalogId, FeedType feedType) {
        BigDecimal supplierWeeklyBudgetUtilised = this.getAndInitialiseSupplierWeeklyUtilisedBudget(campaignDetails.getSupplierId(), weekStartDate);
        if (Objects.nonNull(weeklyBudgetUtilisationLimit) && supplierWeeklyBudgetUtilised.compareTo(weeklyBudgetUtilisationLimit) >= 0) {
            sendSupplierBudgetExhaustedEvent(campaignDetails.getSupplierId(), catalogId);
            return true;
        }
        CampaignBudgetUtilisedData campaignBudgetUtilisedData = this.getAndInitialiseCampaignBudgetUtilised(campaignDetails,
                eventDate, feedType);
        if (campaignBudgetUtilisedData.getTotalBudgetUtilised().compareTo(campaignDetails.getBudget()) >= 0) {
            sendBudgetExhaustedEvent(campaignDetails.getCampaignId(), catalogId);
            return true;
        }
        if(!FeedType.UNKNOWN.equals(feedType)) {
            Set<FeedType> alreadyInactiveRealEstates = Objects.nonNull(campaignDetails.getInactiveRealEstates()) ?
                    campaignDetails.getInactiveRealEstates() : Collections.emptySet();
            List<FeedType> inactiveRealEstates = findInactiveRealEstates(campaignBudgetUtilisedData, campaignDetails, feedType);
            List<FeedType> newInactiveRealEstates = getNewInactiveRealEstates(inactiveRealEstates, alreadyInactiveRealEstates);
            newInactiveRealEstates.remove(FeedType.UNKNOWN);
            if (!CollectionUtils.isEmpty(newInactiveRealEstates)) {
                sendCampaignRealEstateBudgetExhaustedEvent(campaignDetails.getCampaignId(), newInactiveRealEstates);
                return true;
            }
        }
        return false;
    }

    public CampaignBudgetUtilisedData getAndInitialiseCampaignBudgetUtilised(CampaignDetails campaignDetails,
                                                                             LocalDate eventDate,
                                                                             FeedType feedType) {
        log.info("get and initialize campaign budget: {} {} {}", campaignDetails, eventDate, feedType);
        CampaignType campaignType = CampaignType.fromValue(campaignDetails.getCampaignType());
        BigDecimal budgetUtilised = BigDecimal.ZERO;
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = getRealEstateBudgetUtilisedMapWithDefaultValues();

        if (CampaignType.DAILY_BUDGET.equals(campaignType)
                || CampaignType.SMART_CAMPAIGN.equals(campaignType)) {
            CampaignDateWiseMetrics campaignDateWiseMetrics = campaignDateWiseMetricsDao.findByCampaignIdAndDate(campaignDetails.getCampaignId(), eventDate.toString());
            if (Objects.isNull(campaignDateWiseMetrics)) {
                campaignDateWiseMetricsDao.save(CampaignDateWiseMetrics.builder()
                        .campaignId(campaignDetails.getCampaignId())
                        .date(eventDate.toString())
                        .budgetUtilised(BigDecimal.ZERO)
                        .realEstateBudgetUtilisedMap(getRealEstateBudgetUtilisedMapWithDefaultValues())
                        .build());
            } else {
                budgetUtilised = campaignDateWiseMetrics.getBudgetUtilised();
                realEstateBudgetUtilisedMap = getRealEstateBudgetUtilisedMapWithNonNullValues(
                        campaignDateWiseMetrics.getRealEstateBudgetUtilisedMap());
            }
        } else {
            CampaignMetrics campaignMetrics = campaignMetricsDao.findByCampaignId(campaignDetails.getCampaignId());
            if (Objects.isNull(campaignMetrics)) {
                campaignMetricsDao.save(CampaignMetrics.builder()
                        .campaignId(campaignDetails.getCampaignId())
                        .budgetUtilised(BigDecimal.ZERO)
                        .realEstateBudgetUtilisedMap(getRealEstateBudgetUtilisedMapWithDefaultValues())
                        .build());
            } else {
                budgetUtilised = campaignMetrics.getBudgetUtilised();
                realEstateBudgetUtilisedMap = getRealEstateBudgetUtilisedMapWithNonNullValues(
                        campaignMetrics.getRealEstateBudgetUtilisedMap());
            }
        }
        return CampaignBudgetUtilisedData.builder()
                .totalBudgetUtilised(budgetUtilised)
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();
    }

    private Map<FeedType, BigDecimal> getRealEstateBudgetUtilisedMapWithNonNullValues(
            Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap) {
        return  realEstateBudgetUtilisedMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Objects.nonNull(entry.getValue()) ? entry.getValue() : BigDecimal.ZERO)
                );
    }

    private Map<FeedType, BigDecimal> getRealEstateBudgetUtilisedMapWithDefaultValues() {
        return FeedType.ACTIVE_REAL_ESTATE_TYPES.stream()
                .collect(Collectors.toMap(realEstate -> realEstate, realEstate -> BigDecimal.ZERO));
    }

    public void sendCampaignRealEstateBudgetExhaustedEvent(Long campaignId, List<FeedType> inactiveRealEstates) {
        CampaignRealEstateBudgetExhaustedEvent campaignRealEstateBudgetExhaustedEvent = CampaignRealEstateBudgetExhaustedEvent.builder()
                .campaignId(campaignId)
                .realEstates(inactiveRealEstates).build();
        try {
//            kafkaService.sendMessage(campaignRealEstateBudgetExhaustedTopic, String.valueOf(campaignId),
//                    objectMapper.writeValueAsString(campaignRealEstateBudgetExhaustedEvent));
        } catch (Exception e) {
            log.error("Exception while sending campaignRealEstateBudgetExhausted event {}", campaignRealEstateBudgetExhaustedEvent, e);
        }
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
                                                         CampaignType campaignType, FeedType feedType, String eventName) {
        log.info("modifyAndGetBudgetUtilised: {} {} {} {} {} {}", cpc, campaignId, catalogId, date, campaignType, feedType);
        BigDecimal catalogBudgetUtilised = campaignCatalogDateMetricsDao.incrementBudgetUtilisedAndInteractionCount(supplierId, campaignId, catalogId, date.toString(), cpc, getInteractionTypeFromEventName(eventName));
        BigDecimal campaignBudgetUtilised = null;
        if (CampaignType.DAILY_BUDGET.equals(campaignType)
                || CampaignType.SMART_CAMPAIGN.equals(campaignType)) {
            CampaignBudgetUtilisedData campaignBudgetUtilisedData = campaignDateWiseMetricsDao.
                    incrementCampaignAndRealEstateBudgetUtilised(campaignId, date.toString(), cpc, feedType);
            campaignBudgetUtilised = campaignBudgetUtilisedData.getTotalBudgetUtilised();
        }
        else {
            CampaignBudgetUtilisedData campaignBudgetUtilisedData = campaignMetricsDao
                    .incrementCampaignAndRealEstateBudgetUtilised(campaignId, cpc, feedType);
            campaignBudgetUtilised = campaignBudgetUtilisedData.getTotalBudgetUtilised();
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

    //This returns the cpc to be considered for charging.
    public BigDecimal getChargeableCpc(BigDecimal servingTimeCpc, CampaignDetails campaignDetails, Long catalogId) {
        BigDecimal chargeableCPC = Objects.isNull(servingTimeCpc) ? campaignDetails.getCpc() : servingTimeCpc;
        CatalogCPCDiscount catalogCPCDiscount = catalogCPCDiscountDao.get(catalogId);
        try {
            if (Objects.nonNull(catalogCPCDiscount) && Objects.nonNull(chargeableCPC)
                    && catalogCPCDiscount.getDiscount().compareTo(BigDecimal.ZERO) >= 0 && catalogCPCDiscount.getDiscount().compareTo(BigDecimal.ONE) <= 0) {
                chargeableCPC = chargeableCPC.multiply(BigDecimal.ONE.subtract(catalogCPCDiscount.getDiscount()));
            }
        } catch (Exception e) {
            log.error("Error while computing chargeable cpc. CampaignDetails - {}, catalogId - {}, servingTimeCpc - {}, catalogCPCDiscount - {}, {}", campaignDetails, catalogId, servingTimeCpc, catalogCPCDiscount, e.getStackTrace());
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

    public List<FeedType> findInactiveRealEstates(CampaignBudgetUtilisedData campaignBudgetUtilisedData,
                                                  CampaignDetails campaignDetails,
                                                  FeedType realEstate) {
        BigDecimal totalBudget = campaignDetails.getBudget();
        BigDecimal totalBudgetUtilised = campaignBudgetUtilisedData.getTotalBudgetUtilised();
        List<CampaignDetails.CampaignRealEstateBudgetPool> nonDefaultpools = Objects.nonNull(campaignDetails.getCampaignRealEstateBudgetPools()) ?
                campaignDetails.getCampaignRealEstateBudgetPools() : Collections.emptyList();
        List<FeedType> defaultPoolCandidates = getDefaultPoolRealEstates(nonDefaultpools, new ArrayList<>(FeedType.ACTIVE_REAL_ESTATE_TYPES));
        Map<FeedType, CampaignDetails.CampaignRealEstateBudgetPool> realEstateToPoolMap = getRealEstateToPoolMap(nonDefaultpools);

        if(totalBudgetUtilised.compareTo(totalBudget) >= 0) {
            return defaultPoolCandidates.contains(realEstate) ? defaultPoolCandidates :
                    realEstateToPoolMap.get(realEstate).getCandidates();
        }

        if(isDefaultPoolBudgetRemaining(campaignBudgetUtilisedData, campaignDetails, nonDefaultpools)) {
            return Collections.emptyList();
        }

        if(defaultPoolCandidates.contains(realEstate)) {
            return defaultPoolCandidates;
        }

        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = campaignBudgetUtilisedData.getRealEstateBudgetUtilisedMap();
        CampaignDetails.CampaignRealEstateBudgetPool currentPool = realEstateToPoolMap.get(realEstate);
        BigDecimal currentPoolBudgetUtilised = getPoolBudgetUtilised(currentPool.getCandidates(), realEstateBudgetUtilisedMap);
        if(currentPool.getBudgetLimit().compareTo(currentPoolBudgetUtilised) <= 0) {
            return new ArrayList<>(currentPool.getCandidates());
        }
        return Collections.emptyList();
    }

    private Map<FeedType, CampaignDetails.CampaignRealEstateBudgetPool> getRealEstateToPoolMap(
            List<CampaignDetails.CampaignRealEstateBudgetPool> pools) {
        Map<FeedType, CampaignDetails.CampaignRealEstateBudgetPool> realEstateToPoolMap = new HashMap<>();
        for(CampaignDetails.CampaignRealEstateBudgetPool pool : pools) {
            for(FeedType candidate : pool.getCandidates()) {
                realEstateToPoolMap.put(candidate, pool);
            }
        }
        return realEstateToPoolMap;
    }

    public Boolean isDefaultPoolBudgetRemaining(CampaignBudgetUtilisedData campaignBudgetUtilisedData,
                                                CampaignDetails campaignDetails,
                                                List<CampaignDetails.CampaignRealEstateBudgetPool> nonDefaultPools) {
        BigDecimal totalBudget = campaignDetails.getBudget();
        BigDecimal totalBudgetUtilised = campaignBudgetUtilisedData.getTotalBudgetUtilised();
        if(totalBudgetUtilised.compareTo(totalBudget) >= 0) {
            return false;
        }
        if(CollectionUtils.isEmpty(nonDefaultPools)){
            return true;
        }
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = campaignBudgetUtilisedData.getRealEstateBudgetUtilisedMap();
        BigDecimal remainingDefaultPoolBudget = totalBudget;
        for(CampaignDetails.CampaignRealEstateBudgetPool nonDefaultPool : nonDefaultPools) {
            BigDecimal poolBudgetLimit = nonDefaultPool.getBudgetLimit();
            BigDecimal poolBudgetUtilised = getPoolBudgetUtilised(nonDefaultPool.getCandidates(),
                    realEstateBudgetUtilisedMap);
            remainingDefaultPoolBudget = remainingDefaultPoolBudget.subtract(
                    poolBudgetLimit.max(poolBudgetUtilised));
        }
        List<FeedType> defaultPoolCandidates = getDefaultPoolRealEstates(nonDefaultPools, new ArrayList<>(FeedType.ACTIVE_REAL_ESTATE_TYPES));
        BigDecimal defaultPoolBudgetUtilised = getPoolBudgetUtilised(defaultPoolCandidates, realEstateBudgetUtilisedMap);
        remainingDefaultPoolBudget = remainingDefaultPoolBudget.subtract(defaultPoolBudgetUtilised);
        return remainingDefaultPoolBudget.compareTo(BigDecimal.ZERO) > 0;
    }

    public List<FeedType> getDefaultPoolRealEstates(List<CampaignDetails.CampaignRealEstateBudgetPool> nonDefaultPools,
                                                    List<FeedType> realEstates) {
        Set<FeedType> nonDefaultPoolsCandidates = nonDefaultPools.stream()
                .map(CampaignDetails.CampaignRealEstateBudgetPool::getCandidates)
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        return realEstates.stream()
                .filter(feedType -> !nonDefaultPoolsCandidates.contains(feedType))
                .collect(Collectors.toList());
    }

    public BigDecimal getPoolBudgetUtilised(List<FeedType> candidates, Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap) {
        BigDecimal budgetUtilised = BigDecimal.ZERO;
        for(FeedType candidate : candidates) {
            budgetUtilised = budgetUtilised.add(realEstateBudgetUtilisedMap.get(candidate));
        }
        return budgetUtilised;
    }

    public List<FeedType> findInactiveRealEstates(CampaignDetails campaignDetails, LocalDate eventDate) {
        BigDecimal totalBudget = campaignDetails.getBudget();
        List<CampaignDetails.CampaignRealEstateBudgetPool> nonDefaultpools = Objects.nonNull(campaignDetails.getCampaignRealEstateBudgetPools()) ?
                campaignDetails.getCampaignRealEstateBudgetPools() : Collections.emptyList();
        BigDecimal totalBudgetUtilised = null;
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = null;

        if(CampaignType.DAILY_BUDGET.equals(CampaignType.fromValue(campaignDetails.getCampaignType())) ||
                CampaignType.SMART_CAMPAIGN.equals(CampaignType.fromValue(campaignDetails.getCampaignType()))) {
            CampaignDateWiseMetrics campaignDatewiseMetrics = campaignDateWiseMetricsDao.findByCampaignIdAndDate(
                    campaignDetails.getCampaignId(), eventDate.toString());
            totalBudgetUtilised = campaignDatewiseMetrics.getBudgetUtilised();
            realEstateBudgetUtilisedMap = campaignDatewiseMetrics.getRealEstateBudgetUtilisedMap();
        }
        else {
            CampaignMetrics campaignMetrics = campaignMetricsDao.findByCampaignId(campaignDetails.getCampaignId());
            totalBudgetUtilised = campaignMetrics.getBudgetUtilised();
            realEstateBudgetUtilisedMap = campaignMetrics.getRealEstateBudgetUtilisedMap();
        }

        if(totalBudgetUtilised.compareTo(totalBudget) >= 0) {
            return Arrays.asList(FeedType.values());
        }

        CampaignBudgetUtilisedData campaignBudgetUtilisedData = getCampaignBudgetUtilisedData(totalBudgetUtilised,
                realEstateBudgetUtilisedMap);

        if(isDefaultPoolBudgetRemaining(campaignBudgetUtilisedData, campaignDetails, nonDefaultpools)) {
            return Collections.emptyList();
        }

        List<FeedType> defaultPoolCandidates = getDefaultPoolRealEstates(nonDefaultpools,
                new ArrayList<>(FeedType.ACTIVE_REAL_ESTATE_TYPES));
        Set<FeedType> inactiveRealEstates = new HashSet<>(defaultPoolCandidates);
        for(CampaignDetails.CampaignRealEstateBudgetPool pool : nonDefaultpools) {
            if(pool.getBudgetLimit().compareTo(getPoolBudgetUtilised(pool.getCandidates(),
                    realEstateBudgetUtilisedMap)) <= 0) {
                inactiveRealEstates.addAll(pool.getCandidates());
            }
        }
        return new ArrayList<>(inactiveRealEstates);
    }

    private CampaignBudgetUtilisedData getCampaignBudgetUtilisedData(BigDecimal totalBudgetUtilised,
                                                                     Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap) {
        return CampaignBudgetUtilisedData.builder()
                .totalBudgetUtilised(totalBudgetUtilised)
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey,
                                entry -> Objects.nonNull(entry.getValue()) ? entry.getValue() : BigDecimal.ZERO)
                        )
                )
                .build();
    }

    public List<FeedType> getNewInactiveRealEstates(List<FeedType> inactiveRealEstates, Set<FeedType> alreadyInactiveRealEstates) {
        return inactiveRealEstates.stream()
                .filter(realEstate -> !alreadyInactiveRealEstates.contains(realEstate))
                .collect(Collectors.toList());
    }
}
