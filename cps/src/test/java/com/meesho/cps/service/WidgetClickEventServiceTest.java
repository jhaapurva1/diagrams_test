package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ad.client.response.SupplierCampaignCatalogMetaDataResponse;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.constants.AdInteractionStatus;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.helper.InteractionEventAttributionHelper;
import com.meesho.cps.service.external.AdService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.any;

import static com.meesho.cps.constants.TelegrafConstants.*;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
public class WidgetClickEventServiceTest {
    @Mock
    TelegrafMetricsHelper telegrafMetricsHelper;

    @Mock
    ClickBillHandlerImpl clickBillHandler;

    @Mock
    InteractionBillHandlerImpl interactionBillHandler;

    @Mock
    AdBillFactory adBillFactory;

    @Mock
    AdService adService;

    @Mock
    CampaignPerformanceHelper campaignHelper;

    @Mock
    InteractionEventAttributionHelper interactionEventAttributionHelper;

    @Mock
    CampaignCatalogDateMetricsRepository campaignCatalogDateMetricsRepository;

    @Mock
    UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Mock
    UserCatalogInteractionCacheDao userCatalogInteractionCacheDao;

    @InjectMocks
    WidgetClickEventService widgetClickEventService;

    @Before
    public void setUp() {
        Mockito.doReturn(DateTimeUtils.getCurrentLocalDateTimeInIST().toLocalDate()).when(campaignHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        Mockito.doNothing().when(interactionEventAttributionHelper).publishPrismEvent(any());
        Mockito.doNothing().when(interactionEventAttributionHelper).sendBudgetExhaustedEvent(any(), any());
        Mockito.doNothing().when(interactionEventAttributionHelper).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.doNothing().when(updatedCampaignCatalogCacheDao).add(any());
        Mockito.doReturn(1L).when(userCatalogInteractionCacheDao).get(any(), any(), any(), any(), any());
        Mockito.doNothing().when(userCatalogInteractionCacheDao).set(any(), any(), any(), any(), any(), any());
        Mockito.doNothing().when(interactionEventAttributionHelper).incrementInteractionCount(anyLong(), anyLong(), any(), anyString());
    }

    public AdWidgetClickEvent getAdWidgetClickEvent() {
        return AdWidgetClickEvent.builder()
                .eventId("id").eventName("name").userId("user").eventTimestamp(1L).eventTimeIso("1")
                .properties(AdWidgetClickEvent.Properties.builder()
                        .isAdWidget(true)
                        .campaignId(1L)
                        .catalogId(1L)
                        .appVersionCode(1)
                        .origin("origin")
                        .screen("screen")
                        .primaryRealEstate("catalog_search_results")
                        .build())
                .build();
    }

    public SupplierCampaignCatalogMetaDataResponse getSampleSupplierCampaignCatalogMetaDataResponse(int billVersion) {
        SupplierCampaignCatalogMetaDataResponse.CatalogMetadata catalogMetadata = SupplierCampaignCatalogMetaDataResponse.CatalogMetadata.builder()
                .catalogId(3L)
                .campaignActive(true)
                .catalogBudget(BigDecimal.valueOf(50))
                .campaignDetails(CampaignDetails.builder()
                        .campaignId(1L)
                        .campaignType(CampaignType.DAILY_BUDGET.getValue())
                        .cpc(new BigDecimal("0.92"))
                        .billVersion(billVersion)
                        .budget(new BigDecimal("900"))
                        .build())
                .build();
        SupplierCampaignCatalogMetaDataResponse.SupplierMetadata supplierMetadata = SupplierCampaignCatalogMetaDataResponse.SupplierMetadata.builder().supplierId(3L)
                .budgetUtilisationLimit(BigDecimal.valueOf(100)).build();
        return SupplierCampaignCatalogMetaDataResponse.builder().catalogMetadata(catalogMetadata)
                .supplierMetadata(supplierMetadata).build();
    }

    @Test
    public void testHandleIsAdWidgetFalse() throws ExternalRequestFailedException {
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent();
        adWidgetClickEvent.getProperties().setIsAdWidget(false);
        widgetClickEventService.handle(adWidgetClickEvent);

        Mockito.verify(telegrafMetricsHelper, Mockito.times(1)).increment(INTERACTION_EVENT_KEY, String.format(INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(), INVALID,
                AdInteractionInvalidReason.NOT_AD_WIDGET));
    }

    @Test
    public void testHandleBillVersionOneBudgetExhaustedBeforeEventProcessing() throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(true).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
    }

    @Test
    public void testHandleBillVersionOneBudgetExceededTotalBudget() throws ExternalRequestFailedException {
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).catalogBudgetUtilised(BigDecimal.valueOf(50)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(10)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        Mockito.doReturn(LocalDate.now()).when(campaignHelper).getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(0)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).publishPrismEvent(any());
    }

    @Test
    public void testHandleBillVersionOneBudgetExceededSupplierWeeklyBudget() throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).catalogBudgetUtilised(BigDecimal.valueOf(50)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
    }

    @Test
    public void testHandleBillVersionOneBudgetExceededTotalBudgetAndSupplierWeeklyBudget() throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).catalogBudgetUtilised(BigDecimal.valueOf(50)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
    }

    @Test
    public void testHandleBillVersionOneBudgetNotExceeded() throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(100)).catalogBudgetUtilised(BigDecimal.valueOf(50)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(10)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent();
        widgetClickEventService.handle(adWidgetClickEvent);
        Mockito.verify(interactionEventAttributionHelper, times(0)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).publishPrismEvent(any());
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_CPC_KEY, 92, INTERACTION_EVENT_CPC_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin());
    }

    @Test
    public void testHandleBillVersionTwoBudgetExhaustedBeforeEventProcessing() throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(true).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
    }

    @Test
    public void testHandleBillVersionTwoDuplicateEvent() throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(false).when(interactionEventAttributionHelper).checkIfInteractionNeedsToBeConsidered(any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent();
        widgetClickEventService.handle(adWidgetClickEvent);
        Mockito.verify(interactionEventAttributionHelper, times(0)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(0)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionBillHandler, times(1)).performWindowDeDuplication();
        Mockito.verify(interactionEventAttributionHelper, times(1)).checkIfInteractionNeedsToBeConsidered(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).publishPrismEvent(any());
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.DUPLICATE.name());
    }

    @Test
    public void testHandleBillVersionTwoSendBudgetExhaustedEvents() throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).catalogBudgetUtilised(BigDecimal.valueOf(100)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(true).when(interactionEventAttributionHelper).checkIfInteractionNeedsToBeConsidered(any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent();
        widgetClickEventService.handle(adWidgetClickEvent);
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionBillHandler, times(1)).performWindowDeDuplication();
        Mockito.verify(interactionEventAttributionHelper, times(1)).checkIfInteractionNeedsToBeConsidered(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).publishPrismEvent(any());
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_CPC_KEY, 92, INTERACTION_EVENT_CPC_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin());
    }
}
