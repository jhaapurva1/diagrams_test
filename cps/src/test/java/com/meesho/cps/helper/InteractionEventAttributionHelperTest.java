package com.meesho.cps.helper;

import com.meesho.ad.client.constants.FeedType;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants.CpcData;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent.Properties;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.db.mongodb.dao.CampaignDateWiseMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignMetricsDao;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class InteractionEventAttributionHelperTest {

    @Mock
    private CampaignMetricsDao campaignMetricsDao;

    @Mock
    private CampaignDateWiseMetricsDao campaignDateWiseMetricsDao;

    @InjectMocks
    private InteractionEventAttributionHelper interactionEventAttributionHelper;

    @Test
    public void testForGetMultipliedCpcDataWhenChargeableCpcIsNull() {
        HashMap<String, BigDecimal> expectedMultipliedCpcData = new HashMap<>();
        expectedMultipliedCpcData.put(CpcData.MULTIPLIED_CPC, null);
        expectedMultipliedCpcData.put(CpcData.MULTIPLIER, null);
        HashMap<String, BigDecimal> multipliedCpcData = interactionEventAttributionHelper.getMultipliedCpcData(
                null, null, null);
        Assert.assertEquals(expectedMultipliedCpcData, multipliedCpcData);
    }

    @Test
    public void testForGetMultipliedCpcDataWhenWidgetEventHelperIsNull() {
        HashMap<String, BigDecimal> expectedMultipliedCpcData = new HashMap<>();
        expectedMultipliedCpcData.put(CpcData.MULTIPLIED_CPC, BigDecimal.TEN);
        expectedMultipliedCpcData.put(CpcData.MULTIPLIER, BigDecimal.ONE);
        HashMap<String, BigDecimal> multipliedCpcData = interactionEventAttributionHelper.getMultipliedCpcData(
                BigDecimal.TEN, null, null);
        Assert.assertEquals(expectedMultipliedCpcData, multipliedCpcData);
    }

    @Test
    public void testForGetMultipliedCpcDataWhenWidgetEventHelperIsNotNull() {

        HashMap<String, BigDecimal> expectedMultipliedCpcData = new HashMap<>();
        expectedMultipliedCpcData.put(CpcData.MULTIPLIED_CPC, new BigDecimal(100));
        expectedMultipliedCpcData.put(CpcData.MULTIPLIER, BigDecimal.TEN);
        WidgetEventHelper widgetEventHelper = new WidgetEventHelper();
        widgetEventHelper.setContext(AdWidgetClickEvent.builder()
                .properties(Properties.builder().sourceScreen(ConsumerConstants.AdWidgetRealEstates.PDP).build()).build());
        ReflectionTestUtils.setField(widgetEventHelper, "cpcMultiplier", BigDecimal.TEN);
        HashMap<String, BigDecimal> multipliedCpcData = interactionEventAttributionHelper.getMultipliedCpcData(
                BigDecimal.TEN, null, widgetEventHelper);
        Assert.assertEquals(expectedMultipliedCpcData, multipliedCpcData);
    }

    @Test
    public void testGetAndInitialiseCampaignBudgetUtilisedDailyBudgetCampaignNotInRepo() {
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignId(123L)
                .campaignType(CampaignType.DAILY_BUDGET.getValue()).build();
        Mockito.when(campaignDateWiseMetricsDao.findByCampaignIdAndDate(any(), any())).thenReturn(null);
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = FeedType.ACTIVE_REAL_ESTATE_TYPES.stream()
                .collect(Collectors.toMap(feedType -> feedType, feedType -> BigDecimal.ZERO));
        BudgetUtilisedData expectedResult = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(BigDecimal.ZERO)
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();
        BudgetUtilisedData actualResult = interactionEventAttributionHelper
                .getAndInitialiseCampaignBudgetUtilised(campaignDetails, LocalDate.now(), FeedType.ADS_ON_PDP);
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testGetAndInitialiseCampaignBudgetUtilisedDailyBudgetCampaignInRepoWithDataNotPresentForCurrentFeedType() {
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignId(123L)
                .campaignType(CampaignType.DAILY_BUDGET.getValue()).build();

        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMapPresent = new HashMap<>();
        for(FeedType realEstate : FeedType.ACTIVE_REAL_ESTATE_TYPES){
            realEstateBudgetUtilisedMapPresent.put(realEstate, null);
        }
        realEstateBudgetUtilisedMapPresent.put(FeedType.PRODUCT_RECO, new BigDecimal("100"));

        CampaignDateWiseMetrics campaignDatewiseMetrics = CampaignDateWiseMetrics.builder()
                .budgetUtilised(new BigDecimal("1000"))
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMapPresent)
                .build();

        Mockito.when(campaignDateWiseMetricsDao.findByCampaignIdAndDate(any(), any())).thenReturn(campaignDatewiseMetrics);

        Map<FeedType, BigDecimal> expectedRealEstateBudgetUtilisedMap = FeedType.ACTIVE_REAL_ESTATE_TYPES.stream()
                .collect(Collectors.toMap(feedType -> feedType, feedType -> BigDecimal.ZERO));
        expectedRealEstateBudgetUtilisedMap.put(FeedType.PRODUCT_RECO, new BigDecimal("100"));

        BudgetUtilisedData expectedResult = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(new BigDecimal("1000"))
                .realEstateBudgetUtilisedMap(expectedRealEstateBudgetUtilisedMap)
                .build();
        BudgetUtilisedData actualResult = interactionEventAttributionHelper
                .getAndInitialiseCampaignBudgetUtilised(campaignDetails, LocalDate.now(), FeedType.ADS_ON_PDP);
//        Mockito.verify(campaignDateWiseMetricsDao, Mockito.times(1)).save(any());
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testGetAndInitialiseCampaignBudgetUtilisedTotalBudgetCampaignNotInRepo() {
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignId(123L)
                .campaignType(CampaignType.TOTAL_BUDGET.getValue()).build();
        Mockito.when(campaignMetricsDao.findByCampaignId(any())).thenReturn(null);
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = FeedType.ACTIVE_REAL_ESTATE_TYPES.stream()
                .collect(Collectors.toMap(feedType -> feedType, feedType -> BigDecimal.ZERO));
        BudgetUtilisedData expectedResult = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(BigDecimal.ZERO)
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();
        BudgetUtilisedData actualResult = interactionEventAttributionHelper
                .getAndInitialiseCampaignBudgetUtilised(campaignDetails, LocalDate.now(), FeedType.ADS_ON_PDP);
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testGetAndInitialiseCampaignBudgetUtilisedTotalBudgetCampaignInRepoWithDataNotPresentForCurrentFeedType() {
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignId(123L)
                .campaignType(CampaignType.TOTAL_BUDGET.getValue()).build();

        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMapPresent = new HashMap<>();
        for(FeedType realEstate : FeedType.ACTIVE_REAL_ESTATE_TYPES){
            realEstateBudgetUtilisedMapPresent.put(realEstate, null);
        }
        realEstateBudgetUtilisedMapPresent.put(FeedType.PRODUCT_RECO, new BigDecimal("100"));

        CampaignMetrics campaignMetrics = CampaignMetrics.builder()
                .budgetUtilised(new BigDecimal("1000"))
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMapPresent)
                .build();

        Mockito.when(campaignMetricsDao.findByCampaignId(any())).thenReturn(campaignMetrics);

        Map<FeedType, BigDecimal> expectedRealEstateBudgetUtilisedMap = FeedType.ACTIVE_REAL_ESTATE_TYPES.stream()
                .collect(Collectors.toMap(feedType -> feedType, feedType -> BigDecimal.ZERO));
        expectedRealEstateBudgetUtilisedMap.put(FeedType.PRODUCT_RECO, new BigDecimal("100"));

        BudgetUtilisedData expectedResult = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(new BigDecimal("1000"))
                .realEstateBudgetUtilisedMap(expectedRealEstateBudgetUtilisedMap)
                .build();
        BudgetUtilisedData actualResult = interactionEventAttributionHelper
                .getAndInitialiseCampaignBudgetUtilised(campaignDetails, LocalDate.now(), FeedType.ADS_ON_PDP);
//        Mockito.verify(campaignMetricsDao, Mockito.times(1)).save(any());
        Assert.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testGetDefaultPoolRealEstates() {
        List<CampaignDetails.CampaignRealEstateBudgetPool> pools = Arrays.asList(CampaignDetails.CampaignRealEstateBudgetPool.builder()
                .candidates(Arrays.asList(FeedType.TEXT_SEARCH, FeedType.PRODUCT_RECO))
                .budgetLimit(new BigDecimal("10")).build(),
                CampaignDetails.CampaignRealEstateBudgetPool.builder()
                        .candidates(Arrays.asList(FeedType.FY, FeedType.CLP))
                        .budgetLimit(new BigDecimal("50")).build());
        List<FeedType> expected = FeedType.ACTIVE_REAL_ESTATE_TYPES.stream()
                .filter(realEstate -> !Arrays.asList(FeedType.TEXT_SEARCH, FeedType.FY,
                        FeedType.PRODUCT_RECO, FeedType.CLP).contains(realEstate))
                .collect(Collectors.toList());
        List<FeedType> actual = interactionEventAttributionHelper.getDefaultPoolRealEstates(pools,
                new ArrayList<>(FeedType.ACTIVE_REAL_ESTATE_TYPES));
        Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual));
    }

    @Test
    public void testGetPoolBudgetUtilised() {
        List<FeedType> poolCandidates = Arrays.asList(FeedType.FY, FeedType.CLP, FeedType.PRODUCT_RECO);
        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        realEstateBudgetUtilisedMap.put(FeedType.FY, new BigDecimal("20"));
        realEstateBudgetUtilisedMap.put(FeedType.CLP, new BigDecimal("10"));
        realEstateBudgetUtilisedMap.put(FeedType.PRODUCT_RECO, new BigDecimal("30"));
        BigDecimal budgetUtilised = interactionEventAttributionHelper.getPoolBudgetUtilised(poolCandidates,
                realEstateBudgetUtilisedMap);
        Assert.assertEquals(new BigDecimal("60"), budgetUtilised);
    }

    @Test
    public void testGetNewInactiveRealEstates() {
        List<FeedType> inactiveRealEstates = Arrays.asList(FeedType.FY, FeedType.PRODUCT_RECO);
        Set<FeedType> alreadyInactiveRealEstates = new HashSet<>(Arrays.asList(FeedType.CLP, FeedType.COLLECTION, FeedType.FY));
        List<FeedType> expected = Arrays.asList(FeedType.PRODUCT_RECO);
        List<FeedType> actual = interactionEventAttributionHelper.getNewInactiveRealEstates(inactiveRealEstates, alreadyInactiveRealEstates);
        Assert.assertTrue(expected.size() == actual.size() && expected.containsAll(actual));
    }

    @Test
    public void testFindInactiveRealEstatesForPoolWithGivenRealEstatePoolInactive() {
        List<CampaignDetails.CampaignRealEstateBudgetPool> campaignRealEstateBudgetPools = Arrays.asList(
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.PRODUCT_RECO, FeedType.TEXT_SEARCH)).build(),
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.FY, FeedType.CLP)).build()
        );
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignType(CampaignType.DAILY_BUDGET.getValue())
                .campaignId(123L)
                .budget(new BigDecimal("100"))
                .campaignRealEstateBudgetPools(campaignRealEstateBudgetPools).build();

        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        for(FeedType feedType : FeedType.ACTIVE_REAL_ESTATE_TYPES) {
            realEstateBudgetUtilisedMap.put(feedType, new BigDecimal("0"));
        }
        realEstateBudgetUtilisedMap.put(FeedType.PRODUCT_RECO, new BigDecimal("10"));
        realEstateBudgetUtilisedMap.put(FeedType.TEXT_SEARCH, new BigDecimal("30"));
        realEstateBudgetUtilisedMap.put(FeedType.FY, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.CLP, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.COLLECTION, new BigDecimal("40"));

        BudgetUtilisedData budgetUtilisedData = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(new BigDecimal("90"))
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();

        List<FeedType> actualInactiveRealEstates = interactionEventAttributionHelper.findInactiveRealEstates(budgetUtilisedData,
                campaignDetails, FeedType.PRODUCT_RECO);
        List<FeedType> expectedInactiveRealEstates = Arrays.asList(FeedType.PRODUCT_RECO, FeedType.TEXT_SEARCH);

        Assert.assertTrue(expectedInactiveRealEstates.size() == actualInactiveRealEstates.size() &&
                expectedInactiveRealEstates.containsAll(actualInactiveRealEstates));
    }

    @Test
    public void testFindInactiveRealEstatesForPoolWithGivenRealEstatePoolActive() {
        List<CampaignDetails.CampaignRealEstateBudgetPool> campaignRealEstateBudgetPools = Arrays.asList(
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.PRODUCT_RECO, FeedType.TEXT_SEARCH)).build(),
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.FY, FeedType.CLP)).build()
        );
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignType(CampaignType.DAILY_BUDGET.getValue())
                .campaignId(123L)
                .budget(new BigDecimal("100"))
                .campaignRealEstateBudgetPools(campaignRealEstateBudgetPools).build();

        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        for(FeedType feedType : FeedType.ACTIVE_REAL_ESTATE_TYPES) {
            realEstateBudgetUtilisedMap.put(feedType, new BigDecimal("0"));
        }
        realEstateBudgetUtilisedMap.put(FeedType.PRODUCT_RECO, new BigDecimal("10"));
        realEstateBudgetUtilisedMap.put(FeedType.TEXT_SEARCH, new BigDecimal("30"));
        realEstateBudgetUtilisedMap.put(FeedType.FY, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.CLP, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.COLLECTION, new BigDecimal("40"));

        BudgetUtilisedData budgetUtilisedData = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(new BigDecimal("90"))
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();

        List<FeedType> actualInactiveRealEstates = interactionEventAttributionHelper.findInactiveRealEstates(budgetUtilisedData,
                campaignDetails, FeedType.FY);
        List<FeedType> expectedInactiveRealEstates = Collections.emptyList();

        Assert.assertTrue(expectedInactiveRealEstates.size() == actualInactiveRealEstates.size() &&
                expectedInactiveRealEstates.containsAll(actualInactiveRealEstates));
    }

    @Test
    public void testFindInactiveRealEstatesForPoolWithGivenRealEstateDefaultPoolRE() {
        List<CampaignDetails.CampaignRealEstateBudgetPool> campaignRealEstateBudgetPools = Arrays.asList(
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.PRODUCT_RECO, FeedType.TEXT_SEARCH)).build(),
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.FY, FeedType.CLP)).build()
        );
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignType(CampaignType.DAILY_BUDGET.getValue())
                .campaignId(123L)
                .budget(new BigDecimal("100"))
                .campaignRealEstateBudgetPools(campaignRealEstateBudgetPools).build();

        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        for(FeedType feedType : FeedType.ACTIVE_REAL_ESTATE_TYPES) {
            realEstateBudgetUtilisedMap.put(feedType, new BigDecimal("0"));
        }
        realEstateBudgetUtilisedMap.put(FeedType.PRODUCT_RECO, new BigDecimal("10"));
        realEstateBudgetUtilisedMap.put(FeedType.TEXT_SEARCH, new BigDecimal("30"));
        realEstateBudgetUtilisedMap.put(FeedType.FY, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.CLP, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.COLLECTION, new BigDecimal("40"));

        BudgetUtilisedData budgetUtilisedData = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(new BigDecimal("90"))
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();

        List<FeedType> actualInactiveRealEstates = interactionEventAttributionHelper.findInactiveRealEstates(budgetUtilisedData,
                campaignDetails, FeedType.ADS_ON_PDP);
        List<FeedType> expectedInactiveRealEstates = FeedType.ACTIVE_REAL_ESTATE_TYPES.stream()
                .filter(feedType -> !Arrays.asList(FeedType.PRODUCT_RECO, FeedType.FY,
                                FeedType.TEXT_SEARCH, FeedType.CLP).contains(feedType))
                .collect(Collectors.toList());

        Assert.assertTrue(expectedInactiveRealEstates.size() == actualInactiveRealEstates.size() &&
                expectedInactiveRealEstates.containsAll(actualInactiveRealEstates));
    }

    @Test
    public void testFindInactiveRealEstatesForAllPoolsDailyBudget() {
        List<CampaignDetails.CampaignRealEstateBudgetPool> campaignRealEstateBudgetPools = Arrays.asList(
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.PRODUCT_RECO, FeedType.TEXT_SEARCH)).build(),
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.FY, FeedType.CLP)).build()
        );
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignType(CampaignType.DAILY_BUDGET.getValue())
                .campaignId(123L)
                .budget(new BigDecimal("100"))
                .campaignRealEstateBudgetPools(campaignRealEstateBudgetPools).build();

        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        for(FeedType feedType : FeedType.ACTIVE_REAL_ESTATE_TYPES) {
            realEstateBudgetUtilisedMap.put(feedType, BigDecimal.ZERO);
        }
        realEstateBudgetUtilisedMap.put(FeedType.PRODUCT_RECO, new BigDecimal("10"));
        realEstateBudgetUtilisedMap.put(FeedType.TEXT_SEARCH, new BigDecimal("30"));
        realEstateBudgetUtilisedMap.put(FeedType.FY, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.CLP, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.COLLECTION, new BigDecimal("40"));

        BudgetUtilisedData budgetUtilisedData = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(new BigDecimal("90"))
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap).build();
        List<FeedType> actualInactiveRealEstates = interactionEventAttributionHelper.findInactiveRealEstates(
                budgetUtilisedData, campaignDetails);
        List<FeedType> expectedInactiveRealEstates = FeedType.ACTIVE_REAL_ESTATE_TYPES.stream()
                .filter(feedType -> !Arrays.asList(FeedType.FY, FeedType.CLP).contains(feedType))
                .collect(Collectors.toList());

        Assert.assertTrue(expectedInactiveRealEstates.size() == actualInactiveRealEstates.size() &&
                expectedInactiveRealEstates.containsAll(actualInactiveRealEstates));
    }

    @Test
    public void testIsDefaultPoolBudgetRemaining() {
        List<CampaignDetails.CampaignRealEstateBudgetPool> campaignRealEstateBudgetPools = Arrays.asList(
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.PRODUCT_RECO, FeedType.TEXT_SEARCH)).build(),
                CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                        .candidates(Arrays.asList(FeedType.FY, FeedType.CLP)).build()
        );
        List<CampaignDetails.CampaignRealEstateBudgetPool> campaignRealEstateBudgetPoolsWithEnumFeedType = Arrays.asList(
                        CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                                .candidates(Arrays.asList(FeedType.PRODUCT_RECO, FeedType.TEXT_SEARCH)).build(),
                        CampaignDetails.CampaignRealEstateBudgetPool.builder().budgetLimit(new BigDecimal("20"))
                                .candidates(Arrays.asList(FeedType.FY, FeedType.CLP)).build()
        );
        CampaignDetails campaignDetails = CampaignDetails.builder()
                .campaignType(CampaignType.DAILY_BUDGET.getValue())
                .campaignId(123L)
                .budget(new BigDecimal("100"))
                .campaignRealEstateBudgetPools(campaignRealEstateBudgetPools).build();

        Map<FeedType, BigDecimal> realEstateBudgetUtilisedMap = new HashMap<>();
        for(FeedType feedType : FeedType.ACTIVE_REAL_ESTATE_TYPES) {
            realEstateBudgetUtilisedMap.put(feedType, new BigDecimal("0"));
        }
        realEstateBudgetUtilisedMap.put(FeedType.PRODUCT_RECO, new BigDecimal("10"));
        realEstateBudgetUtilisedMap.put(FeedType.TEXT_SEARCH, new BigDecimal("25"));
        realEstateBudgetUtilisedMap.put(FeedType.FY, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.CLP, new BigDecimal("5"));
        realEstateBudgetUtilisedMap.put(FeedType.COLLECTION, new BigDecimal("40"));

        BudgetUtilisedData budgetUtilisedData = BudgetUtilisedData.builder()
                .campaignBudgetUtilised(new BigDecimal("90"))
                .realEstateBudgetUtilisedMap(realEstateBudgetUtilisedMap)
                .build();
        Boolean isDefaultPoolBudgetRemaining = interactionEventAttributionHelper.isDefaultPoolBudgetRemaining(
                budgetUtilisedData, campaignDetails, campaignRealEstateBudgetPoolsWithEnumFeedType);
        Assert.assertTrue(isDefaultPoolBudgetRemaining);
    }
}
