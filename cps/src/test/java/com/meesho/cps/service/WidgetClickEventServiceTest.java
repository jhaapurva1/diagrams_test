package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.constants.AdInteractionStatus;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.AdWidgetValidationHelper;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.helper.ClickAttributionHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.service.external.PrismService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    ClickAttributionHelper clickAttributionHelper;

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
        Mockito.doNothing().when(clickAttributionHelper).publishPrismEvent(any());
        Mockito.doNothing().when(campaignCatalogDateMetricsRepository).incrementClickCount(any(), any(), any());
        Mockito.doNothing().when(clickAttributionHelper).sendBudgetExhaustedEvent(any(), any());
        Mockito.doNothing().when(clickAttributionHelper).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.doNothing().when(updatedCampaignCatalogCacheDao).add(any());
        Mockito.doReturn(1L).when(userCatalogInteractionCacheDao).get(any(), any(), any(), any(), any());
        Mockito.doNothing().when(userCatalogInteractionCacheDao).set(any(), any(), any(), any(), any(), any());

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
                        .primaryRealEstate("search")
                        .build())
                .build();
    }

    public CampaignCatalogMetadataResponse getSampleCatalogMetadataResponse(int billVersion) {
        List<CampaignCatalogMetadataResponse.CatalogMetadata> list = new ArrayList<>();
        list.add(CampaignCatalogMetadataResponse.CatalogMetadata.builder()
                .catalogId(3L)
                .campaignActive(true)
                .campaignDetails(CampaignDetails.builder()
                        .campaignId(1L)
                        .campaignType(CampaignType.DAILY_BUDGET.getValue())
                        .cpc(new BigDecimal("0.92"))
                        .billVersion(billVersion)
                        .budget(new BigDecimal("900"))
                        .build())
                .build());
        List<CampaignCatalogMetadataResponse.SupplierMetadata> supplierMetadataList = new ArrayList<>();
        supplierMetadataList.add(CampaignCatalogMetadataResponse.SupplierMetadata.builder().supplierId(3L)
                .utilizationBudget(BigDecimal.valueOf(100)).build());
        return CampaignCatalogMetadataResponse.builder().campaignDetailsList(list)
                .supplierDetailsList(supplierMetadataList).build();
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
        Mockito.doReturn(getSampleCatalogMetadataResponse(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(true).when(clickAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
    }

    @Test
    public void testHandleBillVersionOneBudgetExceededTotalBudget() throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleCatalogMetadataResponse(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(clickAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(1000)).when(clickAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(10)).when(clickAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
        Mockito.verify(clickAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(clickAttributionHelper, times(0)).sendSupplierBudgetExhaustedEvent(any(), any());
    }

    @Test
    public void testHandleBillVersionOneBudgetExceededSupplierWeeklyBudget() throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleCatalogMetadataResponse(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(clickAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(10)).when(clickAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(clickAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
        Mockito.verify(clickAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(clickAttributionHelper, times(0)).sendBudgetExhaustedEvent(any(), any());
    }

    @Test
    public void testHandleBillVersionOneBudgetExceededTotalBudgetAndSupplierWeeklyBudget() throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleCatalogMetadataResponse(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(clickAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(1000)).when(clickAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(clickAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
        Mockito.verify(clickAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(clickAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
    }

    @Test
    public void testHandleBillVersionOneBudgetNotExceeded() throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleCatalogMetadataResponse(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(clickAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(100)).when(clickAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(10)).when(clickAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent();
        widgetClickEventService.handle(adWidgetClickEvent);
        Mockito.verify(clickAttributionHelper, times(0)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(clickAttributionHelper, times(0)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(clickAttributionHelper, times(1)).publishPrismEvent(any());
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_CPC_KEY, 92, INTERACTION_EVENT_CPC_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin());
    }

    @Test
    public void testHandleBillVersionTwoBudgetExhaustedBeforeEventProcessing() throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(true).when(clickAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent());
    }

    @Test
    public void testHandleBillVersionTwoDuplicateEvent() throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(clickAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(false).when(clickAttributionHelper).checkIfInteractionNeedsToBeConsidered(any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent();
        widgetClickEventService.handle(adWidgetClickEvent);
        Mockito.verify(clickAttributionHelper, times(0)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(clickAttributionHelper, times(0)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionBillHandler, times(1)).performWindowDeDuplication();
        Mockito.verify(clickAttributionHelper, times(1)).checkIfInteractionNeedsToBeConsidered(any(), any());
        Mockito.verify(clickAttributionHelper, times(1)).publishPrismEvent(any());
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.INVALID.name(), AdInteractionInvalidReason.DUPLICATE.name());
    }

    @Test
    public void testHandleBillVersionTwoSendBudgetExhaustedEvents() throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(clickAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(1000)).when(clickAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(clickAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(true).when(clickAttributionHelper).checkIfInteractionNeedsToBeConsidered(any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent();
        widgetClickEventService.handle(adWidgetClickEvent);
        Mockito.verify(clickAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(clickAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionBillHandler, times(1)).performWindowDeDuplication();
        Mockito.verify(clickAttributionHelper, times(1)).checkIfInteractionNeedsToBeConsidered(any(), any());
        Mockito.verify(clickAttributionHelper, times(1)).publishPrismEvent(any());
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_CPC_KEY, 92, INTERACTION_EVENT_CPC_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin());
    }
}
