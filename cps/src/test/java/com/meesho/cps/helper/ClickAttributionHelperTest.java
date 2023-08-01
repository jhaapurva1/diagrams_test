package com.meesho.cps.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.constants.FeedType;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.data.entity.mongodb.collection.SupplierWeekWiseMetrics;
import com.meesho.cps.db.mongodb.dao.CampaignCatalogDateMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignDateWiseMetricsDao;
import com.meesho.cps.db.mongodb.dao.CampaignMetricsDao;
import com.meesho.cps.db.mongodb.dao.SupplierWeekWiseMetricsDao;
import com.meesho.cps.service.KafkaService;
import com.meesho.cps.service.external.PrismService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ClickAttributionHelperTest {
    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private PrismService prismService;

    @Mock
    private CampaignDateWiseMetricsDao campaignDateWiseMetricsDao;

    @Mock
    private SupplierWeekWiseMetricsDao supplierWeekWiseMetricsDao;

    @Mock
    private CampaignMetricsDao campaignMetricsDao;

    @Mock
    private CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @Mock
    private KafkaService kafkaService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InteractionEventAttributionHelper interactionEventAttributionHelper;

    private Long campaignBudgetExhaustedMqID;
    private Long weeklyBudgetExhaustedMqID;


    @Before
    public void setUp() throws JsonProcessingException {
        campaignBudgetExhaustedMqID = 119L;
        weeklyBudgetExhaustedMqID = 123L;
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn("").when(objectMapper).writeValueAsString(any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(100)).realEstateBudgetUtilisedMap(Collections.emptyMap()).build()).when(campaignDateWiseMetricsDao).incrementCampaignAndRealEstateBudgetUtilised(any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(50)).when(campaignCatalogDateMetricsDao).incrementBudgetUtilisedAndInteractionCount(any(), any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).realEstateBudgetUtilisedMap(Collections.emptyMap()).build()).when(campaignMetricsDao).incrementCampaignAndRealEstateBudgetUtilised(any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(200)).when(supplierWeekWiseMetricsDao).incrementSupplierWeeklyBudgetUtilised(any(), any(), any());
        ReflectionTestUtils.setField(interactionEventAttributionHelper, "budgetExhaustedMqID", campaignBudgetExhaustedMqID);
        ReflectionTestUtils.setField(interactionEventAttributionHelper, "suppliersWeeklyBudgetExhaustedMqID", weeklyBudgetExhaustedMqID);
    }

    public AdInteractionPrismEvent getSampleAdInteractionPrismEvent() {
        return AdInteractionPrismEvent.builder().build();
    }

    @Test
    public void checkIfInteractionNeedsToBeConsideredSuccess() {
        long currentTime = System.currentTimeMillis();
        long previousInteractionTime = (System.currentTimeMillis()) - 3 * 86400000; // 3 days
        Mockito.doReturn(2 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        boolean toConsider =
                interactionEventAttributionHelper.checkIfInteractionNeedsToBeConsidered(previousInteractionTime,
                        currentTime);
        Assert.assertTrue(toConsider);
    }

    @Test
    public void checkIfInteractionNeedsToBeConsideredFailed() {
        long currentTime = System.currentTimeMillis();
        long previousInteractionTime = (System.currentTimeMillis()) - 3 * 86400000; // 3 days
        Mockito.doReturn(4 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        boolean toConsider =
                interactionEventAttributionHelper.checkIfInteractionNeedsToBeConsidered(previousInteractionTime,
                        currentTime);
        Assert.assertFalse(toConsider);
    }

    @Test
    public void checkIfInteractionNeedsToBeConsideredPreviousNull() {
        long currentTime = System.currentTimeMillis();
        Mockito.doReturn(4 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        boolean toConsider = interactionEventAttributionHelper.checkIfInteractionNeedsToBeConsidered(null, currentTime);
        Assert.assertTrue(toConsider);
    }

    @Test
    public void testPublishPrismEventSuccess() {
        AdInteractionPrismEvent adInteractionPrismEvent = getSampleAdInteractionPrismEvent();
        interactionEventAttributionHelper.publishPrismEvent(adInteractionPrismEvent);
        Mockito.verify(prismService, Mockito.times(1)).publishEvent(Constants.PrismEventNames.AD_INTERACTIONS, Collections.singletonList(adInteractionPrismEvent));
    }

    @Test
    public void testSendBudgetExhaustedEventSuccess() {
        interactionEventAttributionHelper.sendBudgetExhaustedEvent(1L, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessageToMq(campaignBudgetExhaustedMqID, String.valueOf(1L), "");
    }

    @Test(expected = Exception.class)
    public void testSendBudgetExhaustedEventException() {
        Mockito.doThrow(Exception.class).when(kafkaService).sendMessage(any(), any(), any());
        interactionEventAttributionHelper.sendBudgetExhaustedEvent(1L, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessageToMq(campaignBudgetExhaustedMqID, String.valueOf(1L), "");
    }


    @Test
    public void testSendSupplierBudgetExhaustedEventSuccess() {
        interactionEventAttributionHelper.sendSupplierBudgetExhaustedEvent(1L, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessageToMq(weeklyBudgetExhaustedMqID, String.valueOf(1L), "");

    }

    @Test(expected = Exception.class)
    public void testSendSupplierBudgetExhaustedEventException() {
        Mockito.doThrow(Exception.class).when(kafkaService).sendMessage(any(), any(), any());
        interactionEventAttributionHelper.sendSupplierBudgetExhaustedEvent(1L, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessageToMq(weeklyBudgetExhaustedMqID, String.valueOf(1L), "");
    }

    @Test
    public void testModifyAndGetBudgetUtilizedDailyBudget() {
        BudgetUtilisedData budgetUtilised = interactionEventAttributionHelper.modifyAndGetBudgetUtilised(BigDecimal.ONE, 1L, 1L, 1L, LocalDate.now(), CampaignType.DAILY_BUDGET, FeedType.FY, "ad_click");
        Mockito.verify(campaignCatalogDateMetricsDao, Mockito.times(1)).incrementBudgetUtilisedAndInteractionCount(any(), any(), any(), any(), any(), any());
        Mockito.verify(campaignDateWiseMetricsDao, Mockito.times(1)).incrementCampaignAndRealEstateBudgetUtilised(any(), any(), any(), any());
        Mockito.verify(campaignMetricsDao, Mockito.times(0)).incrementCampaignAndRealEstateBudgetUtilised(any(), any(), any());
        Assert.assertEquals(BigDecimal.valueOf(100), budgetUtilised.getCampaignBudgetUtilised());
        Assert.assertEquals(BigDecimal.valueOf(50), budgetUtilised.getCatalogBudgetUtilised());
    }

    @Test
    public void testModifyAndGetBudgetUtilizedTotalBudget() {
        BudgetUtilisedData budgetUtilised = interactionEventAttributionHelper.modifyAndGetBudgetUtilised(BigDecimal.ONE, 1L, 1L, 1L, LocalDate.now(), CampaignType.TOTAL_BUDGET, FeedType.FY, "ad_click");
        Mockito.verify(campaignCatalogDateMetricsDao, Mockito.times(1)).incrementBudgetUtilisedAndInteractionCount(any(), any(), any(), any(), any(), any());
        Mockito.verify(campaignDateWiseMetricsDao, Mockito.times(0)).incrementCampaignAndRealEstateBudgetUtilised(any(), any(), any(), any());
        Mockito.verify(campaignMetricsDao, Mockito.times(1)).incrementCampaignAndRealEstateBudgetUtilised(any(), any(), any());
        Assert.assertEquals(BigDecimal.valueOf(50), budgetUtilised.getCatalogBudgetUtilised());
    }

    @Test
    public void testModifyAndGetSupplierWeeklyBudget() {
        BigDecimal weeklyBudget = interactionEventAttributionHelper.modifyAndGetSupplierWeeklyBudgetUtilised(1L, LocalDate.now(), BigDecimal.ONE);
        Mockito.verify(supplierWeekWiseMetricsDao, Mockito.times(1)).incrementSupplierWeeklyBudgetUtilised(any(), any(), any());
        Assert.assertEquals(BigDecimal.valueOf(200), weeklyBudget);
    }

    @Test
    public void testInitialiseAndCheckIsBudgetExhaustedSupplierWeeklyBudgetSupplierNotInRepo() {
        Mockito.doReturn(null).when(supplierWeekWiseMetricsDao).findBySupplierIdAndWeekStartDate(1L, LocalDate.now().toString());
        Boolean isBudgetExhausted = interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(CampaignDetails.builder().supplierId(1L).build(), LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, 1L, FeedType.FY);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessageToMq(weeklyBudgetExhaustedMqID, String.valueOf(1L), "");
        Assert.assertTrue(isBudgetExhausted);
    }

    @Test
    public void testInitialiseAndCheckIsBudgetExhaustedSupplierWeeklyBudget() {
        Mockito.doReturn(SupplierWeekWiseMetrics.builder().budgetUtilised(BigDecimal.valueOf(11)).build()).when(supplierWeekWiseMetricsDao).findBySupplierIdAndWeekStartDate(1L, LocalDate.now().toString());
        Boolean isBudgetExhausted = interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(CampaignDetails.builder().supplierId(1L).build(), LocalDate.now(), LocalDate.now(), BigDecimal.TEN, 1L, FeedType.FY);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessageToMq(weeklyBudgetExhaustedMqID, String.valueOf(1L), "");
        Assert.assertTrue(isBudgetExhausted);
    }

    @Test
    public void testInitialiseAndCheckIsBudgetExhaustedCampaignDailyBudget() {
        CampaignDetails campaignDetails = CampaignDetails.builder().supplierId(1L).campaignId(1L).campaignType("DAILY_BUDGET").budget(BigDecimal.TEN).build();
        CampaignDateWiseMetrics campaignDatewiseMetrics = CampaignDateWiseMetrics.builder().budgetUtilised(BigDecimal.TEN)
                .realEstateBudgetUtilisedMap(Collections.emptyMap()).build();
        Mockito.doReturn(SupplierWeekWiseMetrics.builder().budgetUtilised(BigDecimal.ONE).build()).when(supplierWeekWiseMetricsDao).findBySupplierIdAndWeekStartDate(1L, LocalDate.now().toString());
        Mockito.doReturn(campaignDatewiseMetrics).when(campaignDateWiseMetricsDao).findByCampaignIdAndDate(1L, LocalDate.now().toString());
        Boolean isBudgetExhausted = interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(campaignDetails, LocalDate.now(), LocalDate.now(), BigDecimal.TEN, 1L, FeedType.FY);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessageToMq(campaignBudgetExhaustedMqID, String.valueOf(1L), "");
        Assert.assertTrue(isBudgetExhausted);
    }

    @Test
    public void testInitialiseAndCheckIsBudgetExhaustedCampaignTotalBudget() {
        CampaignDetails campaignDetails = CampaignDetails.builder().supplierId(1L).campaignId(1L).campaignType("TOTAL_BUDGET").budget(BigDecimal.TEN).build();
        CampaignMetrics campaignMetrics = CampaignMetrics.builder().budgetUtilised(BigDecimal.TEN)
                .realEstateBudgetUtilisedMap(Collections.emptyMap()).build();
        Mockito.doReturn(SupplierWeekWiseMetrics.builder().budgetUtilised(BigDecimal.ONE).build()).when(supplierWeekWiseMetricsDao).findBySupplierIdAndWeekStartDate(1L, LocalDate.now().toString());
        Mockito.doReturn(campaignMetrics).when(campaignMetricsDao).findByCampaignId(1L);
        Boolean isBudgetExhausted = interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(campaignDetails, LocalDate.now(), LocalDate.now(), BigDecimal.TEN, 1L, FeedType.FY);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessageToMq(campaignBudgetExhaustedMqID, String.valueOf(1L), "");
        Assert.assertTrue(isBudgetExhausted);
    }
}
