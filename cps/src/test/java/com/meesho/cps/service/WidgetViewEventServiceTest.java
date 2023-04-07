package com.meesho.cps.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ad.client.response.AdViewEventMetadataResponse;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.AdInteractionInvalidReason;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import com.meesho.cps.data.internal.CampaignCatalogViewCount;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.instrumentation.metric.statsd.StatsdMetricManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static com.meesho.cps.constants.TelegrafConstants.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;

@RunWith(MockitoJUnitRunner.class)
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

    @Before
    public void setUp() throws ExternalRequestFailedException {
        Mockito.doNothing().when(statsdMetricManager).incrementCounter(any(), any());
        Mockito.doReturn(getCampaignCatalogMetadataMap(true)).when(catalogViewEventService).getCampaignCatalogMetadataFromCatalogIds(any());
        Mockito.doNothing().when(catalogViewEventService).writeToHbase(any());
        Mockito.doReturn(localDate).when(campaignPerformanceHelper).getLocalDateForDailyCampaignFromLocalDateTime(any());
        ReflectionTestUtils.setField(widgetViewEventService, "ingestionViewEventsDeadQueueTopic", "topic");
    }

    public AdWidgetViewEvent getAdWidgetViewEvent() {
        return AdWidgetViewEvent.builder()
                .eventId("id")
                .eventName("name")
                .eventTimestamp(1L)
                .eventTimeIso("1")
                .userId("user")
                .properties(AdWidgetViewEvent.Properties.builder()
                        .appVersionCode(1)
                        .campaignIds(Collections.singletonList(1L))
                        .catalogIds(Collections.singletonList(1L))
                        .origins(Collections.singletonList("origin"))
                        .screens(Collections.singletonList("screen"))
                        .primaryRealEstates(Collections.singletonList("catalog_search_results"))
                        .build())
                .build();
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

    @Test
    public void testHandleNotAdWidget() {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent();
        adWidgetViewEvent.getProperties().setPrimaryRealEstates(Collections.singletonList("non-search"));
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), INVALID,
                AdInteractionInvalidReason.NOT_AD_WIDGET));
    }

    @Test
    public void testHandleEmptyCampaignCatalogMetadataResponseMap() throws ExternalRequestFailedException {
        Mockito.doReturn(Collections.EMPTY_MAP).when(catalogViewEventService).getCampaignCatalogMetadataFromCatalogIds(any());
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent();
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(any(), any());
    }

    @Test
    public void testHandleInActiveCampaign() throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent();
        Mockito.doReturn(getCampaignCatalogMetadataMap(false)).when(catalogViewEventService).getCampaignCatalogMetadataFromCatalogIds(any());
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), INVALID,
                AdInteractionInvalidReason.CAMPAIGN_INACTIVE));

    }

    @Test
    public void testHandleActiveCampaign() throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent();
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));

    }

    @Test
    public void testHandleActiveCampaignNotWriteToHBase() throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent();
        Mockito.doReturn(100L).when(applicationProperties).getBatchInterval();
        widgetViewEventService.handle(adWidgetViewEvent);
        Mockito.verify(catalogViewEventService, times(0)).writeToHbase(any());
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));

    }

    @Test
    public void testHandleActiveCampaignWriteToHbaseNewEntry() throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent();
        Mockito.doReturn(0L).when(applicationProperties).getBatchInterval();
        widgetViewEventService.handle(adWidgetViewEvent);
        CampaignCatalogViewCount campaignCatalogViewCount = CampaignCatalogViewCount.builder().campaignId(1L).catalogId(1L).count(1).date(localDate).build();
        Mockito.verify(catalogViewEventService, times(1)).writeToHbase(Collections.singletonList(campaignCatalogViewCount));
        Mockito.verify(statsdMetricManager, times(1)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));

    }

    @Test
    public void testHandleActiveCampaignWriteToHbaseUpdateEntry() throws ExternalRequestFailedException, InterruptedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent();
        Mockito.doReturn(10L).when(applicationProperties).getBatchInterval();
        widgetViewEventService.handle(adWidgetViewEvent);
        Thread.sleep(10);
        widgetViewEventService.handle(adWidgetViewEvent);
        CampaignCatalogViewCount campaignCatalogViewCount = CampaignCatalogViewCount.builder().campaignId(1L).catalogId(1L).count(2).date(localDate).build();
        Mockito.verify(catalogViewEventService, times(1)).writeToHbase(Collections.singletonList(campaignCatalogViewCount));
        Mockito.verify(statsdMetricManager, times(2)).incrementCounter(WIDGET_VIEW_EVENT_KEY, String.format(VIEW_EVENT_TAGS,
                adWidgetViewEvent.getEventName(), adWidgetViewEvent.getProperties().getScreens(), adWidgetViewEvent.getProperties().getOrigins(), VALID, NAN));

    }

    @Test(expected = Exception.class)
    public void testHandleCampaignCatalogMetadataResponseThrowsException() throws ExternalRequestFailedException {
        AdWidgetViewEvent adWidgetViewEvent = getAdWidgetViewEvent();
        Mockito.doThrow(Exception.class).when(catalogViewEventService).getCampaignCatalogMetadataFromCatalogIds(any());
        widgetViewEventService.handle(adWidgetViewEvent);
    }
}
