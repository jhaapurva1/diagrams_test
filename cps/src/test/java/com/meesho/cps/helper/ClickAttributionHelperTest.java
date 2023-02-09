package com.meesho.cps.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.entity.hbase.SupplierWeekWiseMetrics;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
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
    private CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Mock
    private SupplierWeekWiseMetricsRepository supplierWeekWiseMetricsRepository;

    @Mock
    private CampaignMetricsRepository campaignMetricsRepository;

    @Mock
    private CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Mock
    private KafkaService kafkaService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private InteractionEventAttributionHelper interactionEventAttributionHelper;


    @Before
    public void setUp() throws JsonProcessingException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(null).when(kafkaService).sendMessage("weeklyBudgetExhaustedTopic", String.valueOf(1L), "");
        Mockito.doReturn(null).when(kafkaService).sendMessage("campaignBudgetExhaustedTopic", String.valueOf(1L), "");
        Mockito.doReturn("").when(objectMapper).writeValueAsString(any());
        Mockito.doReturn(BigDecimal.valueOf(100)).when(campaignDatewiseMetricsRepository).incrementBudgetUtilised(any(), any(), any());
        Mockito.doNothing().when(campaignCatalogDateMetricsRepository).incrementBudgetUtilised(any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(1000)).when(campaignMetricsRepository).incrementBudgetUtilised(any(), any());
        Mockito.doReturn(BigDecimal.valueOf(200)).when(supplierWeekWiseMetricsRepository).incrementBudgetUtilised(any(), any(), any());
        ReflectionTestUtils.setField(interactionEventAttributionHelper, "budgetExhaustedTopic", "campaignBudgetExhaustedTopic");
        ReflectionTestUtils.setField(interactionEventAttributionHelper, "suppliersWeeklyBudgetExhaustedTopic", "weeklyBudgetExhaustedTopic");
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
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessage("campaignBudgetExhaustedTopic", String.valueOf(1L), "");
    }

    @Test(expected = Exception.class)
    public void testSendBudgetExhaustedEventException() {
        Mockito.doThrow(Exception.class).when(kafkaService).sendMessage(any(), any(), any());
        interactionEventAttributionHelper.sendBudgetExhaustedEvent(1L, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessage("campaignBudgetExhaustedTopic", String.valueOf(1L), "");
    }


    @Test
    public void testSendSupplierBudgetExhaustedEventSuccess() {
        interactionEventAttributionHelper.sendSupplierBudgetExhaustedEvent(1L, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessage("weeklyBudgetExhaustedTopic", String.valueOf(1L), "");

    }

    @Test(expected = Exception.class)
    public void testSendSupplierBudgetExhaustedEventException() {
        Mockito.doThrow(Exception.class).when(kafkaService).sendMessage(any(), any(), any());
        interactionEventAttributionHelper.sendSupplierBudgetExhaustedEvent(1L, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessage("weeklyBudgetExhaustedTopic", String.valueOf(1L), "");
    }

    @Test
    public void testModifyAndGetBudgetUtilizedDailyBudget() {
        BigDecimal budget = interactionEventAttributionHelper.modifyAndGetBudgetUtilised(BigDecimal.ONE, 1L, 1L, LocalDate.now(), CampaignType.DAILY_BUDGET);
        Mockito.verify(campaignCatalogDateMetricsRepository, Mockito.times(1)).incrementBudgetUtilised(any(), any(), any(), any());
        Mockito.verify(campaignDatewiseMetricsRepository, Mockito.times(1)).incrementBudgetUtilised(any(), any(), any());
        Mockito.verify(campaignMetricsRepository, Mockito.times(0)).incrementBudgetUtilised(any(), any());
        Assert.assertEquals(BigDecimal.valueOf(100), budget);
    }

    @Test
    public void testModifyAndGetBudgetUtilizedTotalBudget() {
        BigDecimal budget = interactionEventAttributionHelper.modifyAndGetBudgetUtilised(BigDecimal.ONE, 1L, 1L, LocalDate.now(), CampaignType.TOTAL_BUDGET);
        Mockito.verify(campaignCatalogDateMetricsRepository, Mockito.times(1)).incrementBudgetUtilised(any(), any(), any(), any());
        Mockito.verify(campaignDatewiseMetricsRepository, Mockito.times(0)).incrementBudgetUtilised(any(), any(), any());
        Mockito.verify(campaignMetricsRepository, Mockito.times(1)).incrementBudgetUtilised(any(), any());
        Assert.assertEquals(BigDecimal.valueOf(1000), budget);
    }

    @Test
    public void testModifyAndGetSupplierWeeklyBudget() {
        BigDecimal weeklyBudget = interactionEventAttributionHelper.modifyAndGetSupplierWeeklyBudgetUtilised(1L, LocalDate.now(), BigDecimal.ONE);
        Mockito.verify(supplierWeekWiseMetricsRepository, Mockito.times(1)).incrementBudgetUtilised(any(), any(), any());
        Assert.assertEquals(BigDecimal.valueOf(200), weeklyBudget);
    }

    @Test
    public void testInitialiseAndCheckIsBudgetExhaustedSupplierWeeklyBudgetSupplierNotInRepo() {
        Mockito.doReturn(null).when(supplierWeekWiseMetricsRepository).get(1L, LocalDate.now());
        Boolean isBudgetExhausted = interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(CampaignDetails.builder().supplierId(1L).build(), LocalDate.now(), LocalDate.now(), BigDecimal.ZERO, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessage("weeklyBudgetExhaustedTopic", String.valueOf(1L), "");
        Assert.assertTrue(isBudgetExhausted);
    }

    @Test
    public void testInitialiseAndCheckIsBudgetExhaustedSupplierWeeklyBudget() {
        Mockito.doReturn(SupplierWeekWiseMetrics.builder().budgetUtilised(BigDecimal.valueOf(11)).build()).when(supplierWeekWiseMetricsRepository).get(1L, LocalDate.now());
        Boolean isBudgetExhausted = interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(CampaignDetails.builder().supplierId(1L).build(), LocalDate.now(), LocalDate.now(), BigDecimal.TEN, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessage("weeklyBudgetExhaustedTopic", String.valueOf(1L), "");
        Assert.assertTrue(isBudgetExhausted);
    }

    @Test
    public void testInitialiseAndCheckIsBudgetExhaustedCampaignDailyBudget() {
        CampaignDetails campaignDetails = CampaignDetails.builder().supplierId(1L).campaignId(1L).campaignType("DAILY_BUDGET").budget(BigDecimal.TEN).build();
        CampaignDatewiseMetrics campaignDatewiseMetrics = CampaignDatewiseMetrics.builder().budgetUtilised(BigDecimal.TEN).build();
        Mockito.doReturn(SupplierWeekWiseMetrics.builder().budgetUtilised(BigDecimal.ONE).build()).when(supplierWeekWiseMetricsRepository).get(1L, LocalDate.now());
        Mockito.doReturn(campaignDatewiseMetrics).when(campaignDatewiseMetricsRepository).get(1L, LocalDate.now());
        Boolean isBudgetExhausted = interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(campaignDetails, LocalDate.now(), LocalDate.now(), BigDecimal.TEN, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessage("campaignBudgetExhaustedTopic", String.valueOf(1L), "");
        Assert.assertTrue(isBudgetExhausted);
    }

    @Test
    public void testInitialiseAndCheckIsBudgetExhaustedCampaignTotalBudget() {
        CampaignDetails campaignDetails = CampaignDetails.builder().supplierId(1L).campaignId(1L).campaignType("TOTAL_BUDGET").budget(BigDecimal.TEN).build();
        CampaignMetrics campaignMetrics = CampaignMetrics.builder().budgetUtilised(BigDecimal.TEN).build();
        Mockito.doReturn(SupplierWeekWiseMetrics.builder().budgetUtilised(BigDecimal.ONE).build()).when(supplierWeekWiseMetricsRepository).get(1L, LocalDate.now());
        Mockito.doReturn(campaignMetrics).when(campaignMetricsRepository).get(1L);
        Boolean isBudgetExhausted = interactionEventAttributionHelper.initialiseAndCheckIsBudgetExhausted(campaignDetails, LocalDate.now(), LocalDate.now(), BigDecimal.TEN, 1L);
        Mockito.verify(kafkaService, Mockito.times(1)).sendMessage("campaignBudgetExhaustedTopic", String.valueOf(1L), "");
        Assert.assertTrue(isBudgetExhausted);
    }
}
