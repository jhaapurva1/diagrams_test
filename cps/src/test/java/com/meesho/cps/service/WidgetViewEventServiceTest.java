package com.meesho.cps.service;

import static com.meesho.cps.constants.TelegrafConstants.INVALID;
import static com.meesho.cps.constants.TelegrafConstants.NAN;
import static com.meesho.cps.constants.TelegrafConstants.VALID;
import static com.meesho.cps.constants.TelegrafConstants.VIEW_EVENT_TAGS;
import static com.meesho.cps.constants.TelegrafConstants.WIDGET_VIEW_EVENT_KEY;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;

import com.meesho.ad.client.response.AdViewEventMetadataResponse;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.constants.ConsumerConstants.AdWidgetRealEstates;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.instrumentation.metric.statsd.StatsdMetricManager;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

public class WidgetViewEventServiceTest {

    @Mock
    ApplicationProperties applicationProperties;

    @Mock
    StatsdMetricManager statsdMetricManager;

    @Mock
    CatalogViewEventService catalogViewEventService;

    @Mock
    CampaignPerformanceHelper campaignPerformanceHelper;

    LocalDate localDate = LocalDate.now();

    @InjectMocks
    WidgetViewEventService widgetViewEventService;

    @BeforeEach
    public void setUp() throws ExternalRequestFailedException {
        MockitoAnnotations.initMocks(this);
        Mockito.doNothing().when(statsdMetricManager).incrementCounter(any(), any());
        Mockito.doReturn(getCampaignCatalogMetadataMap(true)).when(catalogViewEventService).getCampaignCatalogMetadataFromCatalogIds(any());
        Mockito.doNothing().when(catalogViewEventService).writeToHbase(any());
        Mockito.doReturn(localDate).when(campaignPerformanceHelper).getLocalDateForDailyCampaignFromLocalDateTime(any());
        ReflectionTestUtils.setField(widgetViewEventService, "ingestionViewEventsDeadQueueTopic", "topic");
    }

    public AdWidgetViewEvent getAdWidgetViewEvent(String realEstate) {
        AdWidgetViewEvent adWidgetViewEvent = null;
        switch (realEstate) {
            case AdWidgetRealEstates.TEXT_SEARCH:
                adWidgetViewEvent = AdWidgetViewEvent.builder().eventId("id").eventName("name")
                    .eventTimestamp(1L).eventTimeIso("1").userId("user").properties(
                        AdWidgetViewEvent.Properties.builder().appVersionCode(1)
                            .campaignIds(Collections.singletonList(1L))
                            .catalogIds(Collections.singletonList(1L))
                            .origins(Collections.singletonList("origin"))
                            .screens(Collections.singletonList("screen"))
                            .sourceScreens(Collections.singletonList(AdWidgetRealEstates.TEXT_SEARCH))
                            .build()).build();
                break;
            case AdWidgetRealEstates.PDP:
                adWidgetViewEvent = AdWidgetViewEvent.builder().eventId("id").eventName("name")
                    .eventTimestamp(1L).eventTimeIso("1").userId("user").properties(
                        AdWidgetViewEvent.Properties.builder().appVersionCode(1)
                            .campaignIds(Collections.singletonList(1L))
                            .catalogIds(Collections.singletonList(1L))
                            .origins(Collections.singletonList("origin"))
                            .screens(Collections.singletonList("screen"))
                            .sourceScreens(Collections.singletonList(AdWidgetRealEstates.PDP)).build())
                    .build();
        }
        return adWidgetViewEvent;
    }

    public Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> getCampaignCatalogMetadataMap(Boolean isActive) {
        AdViewEventMetadataResponse.CatalogCampaignMetadata responseMetadata =
                AdViewEventMetadataResponse.CatalogCampaignMetadata.builder()
                        .campaignId(1L)
                        .catalogId(1L)
                        .isCampaignActive(isActive)
                        .build();
        return Collections.singletonMap(responseMetadata.getCatalogId(), responseMetadata);
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleNotAdWidget(String realEstate) {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent(realEstate);
        adWidgetViewEvent.getProperties().setSourceScreens(Collections.singletonList("non-search"));
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), INVALID,
                AdInteractionInvalidReason.NOT_AD_WIDGET));
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleEmptyCampaignCatalogMetadataResponseMap(String realEstate) throws ExternalRequestFailedException {
        Mockito.doReturn(Collections.EMPTY_MAP).when(catalogViewEventService).getCampaignCatalogMetadataFromCatalogIds(any());
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent(realEstate);
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleInActiveCampaign(String realEstate) throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent(realEstate);
        Mockito.doReturn(getCampaignCatalogMetadataMap(false)).when(catalogViewEventService).getCampaignCatalogMetadataFromCatalogIds(any());
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), INVALID,
                AdInteractionInvalidReason.CAMPAIGN_INACTIVE));

    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleActiveCampaign(String realEstate) throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent(realEstate);
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));

    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleActiveCampaignNotWriteToHBase(String realEstate) throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent(realEstate);
        Mockito.doReturn(100L).when(applicationProperties).getBatchInterval();
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(catalogViewEventService, times(0)).writeToHbase(any());
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));

    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleActiveCampaignWriteToHbaseNewEntry(String realEstate) throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent(realEstate);
        Mockito.doReturn(0L).when(applicationProperties).getBatchInterval();
        widgetViewEventService.handle(adWidgetViewEvent);
        CampaignCatalogViewCount campaignCatalogViewCount = CampaignCatalogViewCount.builder().campaignId(1L).catalogId(1L).count(1).date(localDate).build();
        Mockito.verify(catalogViewEventService, times(1)).writeToHbase(Collections.singletonList(campaignCatalogViewCount));
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));

    }

    @ParameterizedTest
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP })
    public void testHandleActiveCampaignWriteToHbaseUpdateEntry(String realEstate) throws ExternalRequestFailedException, InterruptedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent(realEstate);
        Mockito.doReturn(10L).when(applicationProperties).getBatchInterval();
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));
        Thread.sleep(10);
        widgetViewEventService.handle(adWidgetViewEvent);
        CampaignCatalogViewCount campaignCatalogViewCount = CampaignCatalogViewCount.builder().campaignId(1L).catalogId(1L).count(2).date(localDate).build();
        Mockito.verify(catalogViewEventService, times(1)).writeToHbase(Collections.singletonList(campaignCatalogViewCount));
        Mockito.verify(statsdMetricManager, times(2)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));

    }

    @ParameterizedTest()
    @ValueSource(strings = {AdWidgetRealEstates.TEXT_SEARCH, AdWidgetRealEstates.PDP})
    public void testHandleCampaignCatalogMetadataResponseThrowsException(String realEstate)
        throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent(realEstate);
        Mockito.doThrow(RuntimeException.class).when(catalogViewEventService)
            .getCampaignCatalogMetadataFromCatalogIds(any());
        Assertions.assertThrows(RuntimeException.class, () -> {
            widgetViewEventService.handle(adWidgetViewEvent);
        });
    }
}
