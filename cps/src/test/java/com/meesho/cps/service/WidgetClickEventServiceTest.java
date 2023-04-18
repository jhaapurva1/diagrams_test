package com.meesho.cps.service;

import static com.meesho.cps.constants.TelegrafConstants.INTERACTION_EVENT_KEY;
import static com.meesho.cps.constants.TelegrafConstants.INTERACTION_EVENT_TAGS;
import static com.meesho.cps.constants.TelegrafConstants.INVALID;
import static com.meesho.cps.constants.TelegrafConstants.NAN;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;

import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ad.client.response.SupplierCampaignCatalogMetaDataResponse;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.constants.AdInteractionStatus;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.constants.Constants.CpcData;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.helper.InteractionEventAttributionHelper;
import com.meesho.cps.helper.WidgetEventHelper;
import com.meesho.cps.service.external.AdService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

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

    private BigDecimal commonCpcValue;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(widgetClickEventService, "widgetEventHelper", new WidgetEventHelper());
        commonCpcValue = new BigDecimal("0.92");
        HashMap<String, BigDecimal> multipliedCpcData = new HashMap<>();
        multipliedCpcData.put(CpcData.MULTIPLIED_CPC, commonCpcValue);
        multipliedCpcData.put(CpcData.MULTIPLIER, BigDecimal.ONE);
        Mockito.doReturn(commonCpcValue).when(interactionEventAttributionHelper)
            .getChargeableCpc(any(), any());
        Mockito.doReturn(multipliedCpcData).when(interactionEventAttributionHelper)
            .getMultipliedCpcData(any(), any(), any());
        Mockito.doReturn(DateTimeUtils.getCurrentLocalDateTimeInIST().toLocalDate()).when(campaignHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        Mockito.doNothing().when(interactionEventAttributionHelper).publishPrismEvent(any());
        Mockito.doNothing().when(interactionEventAttributionHelper).sendBudgetExhaustedEvent(any(), any());
        Mockito.doNothing().when(interactionEventAttributionHelper).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.doNothing().when(updatedCampaignCatalogCacheDao).add(any());
        Mockito.doReturn(1L).when(userCatalogInteractionCacheDao).get(any(), any(), any(), any(), any());
        Mockito.doNothing().when(userCatalogInteractionCacheDao).set(any(), any(), any(), any(), any(), any());
    }

    public AdWidgetClickEvent getAdWidgetClickEvent(String realEstate) {
        AdWidgetClickEvent adWidgetClickEvent = null;
        switch (realEstate) {
            case AdWidgetRealEstates.TEXT_SEARCH:
                adWidgetClickEvent = AdWidgetClickEvent.builder().eventId("id").eventName("name")
                    .userId("user").eventTimestamp(1L).eventTimeIso("1").properties(
                        AdWidgetClickEvent.Properties.builder().isAdWidget(true).campaignId(1L)
                            .catalogId(1L).appVersionCode(1).origin("origin").screen("screen")
                            .sourceScreen("catalog_search_results").build()).build();
                break;
            case AdWidgetRealEstates.PDP:
                adWidgetClickEvent = AdWidgetClickEvent.builder().eventId("id").eventName("name")
                    .userId("user").eventTimestamp(1L).eventTimeIso("1").properties(
                        AdWidgetClickEvent.Properties.builder().isAdWidget(true).campaignId(1L)
                            .catalogId(1L).appVersionCode(1).origin("origin").screen("screen")
                            .sourceScreen("single_catalog").build()).build();
        }
        return adWidgetClickEvent;
    }


    public SupplierCampaignCatalogMetaDataResponse getSampleSupplierCampaignCatalogMetaDataResponse(int billVersion) {
        SupplierCampaignCatalogMetaDataResponse.CatalogMetadata catalogMetadata = SupplierCampaignCatalogMetaDataResponse.CatalogMetadata.builder()
                .catalogId(3L)
                .campaignActive(true)
                .catalogBudget(BigDecimal.valueOf(50))
                .campaignDetails(CampaignDetails.builder()
                        .campaignId(1L)
                        .campaignType(CampaignType.DAILY_BUDGET.getValue())
                        .cpc(commonCpcValue)
                        .billVersion(billVersion)
                        .budget(new BigDecimal("900"))
                        .build())
                .build();
        SupplierCampaignCatalogMetaDataResponse.SupplierMetadata supplierMetadata = SupplierCampaignCatalogMetaDataResponse.SupplierMetadata.builder().supplierId(3L)
                .budgetUtilisationLimit(BigDecimal.valueOf(100)).build();
        return SupplierCampaignCatalogMetaDataResponse.builder().catalogMetadata(catalogMetadata)
                .supplierMetadata(supplierMetadata).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleIsAdWidgetFalse(String realEstate) throws ExternalRequestFailedException {
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent(realEstate);
        adWidgetClickEvent.getProperties().setIsAdWidget(false);
        widgetClickEventService.handle(adWidgetClickEvent);

        Mockito.verify(telegrafMetricsHelper, Mockito.times(1)).increment(INTERACTION_EVENT_KEY, String.format(INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(), INVALID,
                AdInteractionInvalidReason.NOT_AD_WIDGET));
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleBillVersionOneBudgetExhaustedBeforeEventProcessing(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(true).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent(realEstate));
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleBillVersionOneBudgetExceededTotalBudget(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).catalogBudgetUtilised(BigDecimal.valueOf(50)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(10)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent(realEstate));
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(0)).sendSupplierBudgetExhaustedEvent(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleBillVersionOneBudgetExceededSupplierWeeklyBudget(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).catalogBudgetUtilised(BigDecimal.valueOf(50)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent(realEstate));
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleBillVersionOneBudgetExceededTotalBudgetAndSupplierWeeklyBudget(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).catalogBudgetUtilised(BigDecimal.valueOf(50)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent(realEstate));
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleBillVersionOneBudgetNotExceeded(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(100)).catalogBudgetUtilised(BigDecimal.valueOf(50)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(10)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent(realEstate);
        widgetClickEventService.handle(adWidgetClickEvent);
        Mockito.verify(interactionEventAttributionHelper, times(0)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(0)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).publishPrismEvent(any());
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleBillVersionTwoBudgetExhaustedBeforeEventProcessing(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(true).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        widgetClickEventService.handle(getAdWidgetClickEvent(realEstate));
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleBillVersionTwoDuplicateEvent(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(false).when(interactionEventAttributionHelper).checkIfInteractionNeedsToBeConsidered(any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent(realEstate);
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

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleBillVersionTwoSendBudgetExhaustedEvents(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(false).when(interactionEventAttributionHelper).initialiseAndCheckIsBudgetExhausted(any(), any(), any(), any(), any());
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(1000)).catalogBudgetUtilised(BigDecimal.valueOf(100)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(110)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(true).when(interactionEventAttributionHelper).checkIfInteractionNeedsToBeConsidered(any(), any());
        AdWidgetClickEvent adWidgetClickEvent = getAdWidgetClickEvent(realEstate);
        widgetClickEventService.handle(adWidgetClickEvent);
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).sendSupplierBudgetExhaustedEvent(any(), any());
        Mockito.verify(interactionBillHandler, times(1)).performWindowDeDuplication();
        Mockito.verify(interactionEventAttributionHelper, times(1)).checkIfInteractionNeedsToBeConsidered(any(), any());
        Mockito.verify(interactionEventAttributionHelper, times(1)).publishPrismEvent(any());
        Mockito.verify(telegrafMetricsHelper, times(1)).increment(INTERACTION_EVENT_KEY, INTERACTION_EVENT_TAGS,
                adWidgetClickEvent.getEventName(), adWidgetClickEvent.getProperties().getScreen(), adWidgetClickEvent.getProperties().getOrigin(),
                AdInteractionStatus.VALID.name(), NAN);
    }
}
