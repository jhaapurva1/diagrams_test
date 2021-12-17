package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.mysql.RealEstateMetadata;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.redis.dao.RealEstateMetadataCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.service.external.PrismService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

/**
 * @author shubham.aggarwal
 * 11/08/21
 */
@RunWith(MockitoJUnitRunner.class)
public class CatalogInteractionEventServiceTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private CampaignCatalogDateMetricsRepository campaignCatalogMetricsRepository;

    @Mock
    private RealEstateMetadataCacheDao realEstateMetadataCacheDao;

    @Mock
    private AdBillFactory adBillFactory;

    @Mock
    private AdService adService;

    @Mock
    private PrismService prismService;

    @Mock
    private ClickBillHandlerImpl clickBillHandler;

    @Mock
    private InteractionBillHandlerImpl interactionBillHandler;

    @Mock
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Mock
    private CampaignPerformanceHelper campaignPerformanceHelper;

    @InjectMocks
    private CatalogInteractionEventService catalogInteractionEventService;

    public List<CampaignCatalogMetadataResponse.CatalogMetadata> getSampleCatalogMetadataList() {
        List<CampaignCatalogMetadataResponse.CatalogMetadata> list = new ArrayList<>();
        list.add(CampaignCatalogMetadataResponse.CatalogMetadata.builder()
                .catalogId(3L)
                .campaignActive(true)
                .campaignDetails(CampaignDetails.builder()
                        .campaignId(1L)
                        .campaignType(CampaignType.DAILY_BUDGET.getValue())
                        .cpc(new BigDecimal("0.92"))
                        .billVersion(1)
                        .budget(new BigDecimal("900"))
                        .build())
                .build());
        return list;
    }

    public List<CampaignCatalogMetadataResponse.CatalogMetadata> getSampleCatalogMetadataListBillVersion2() {
        List<CampaignCatalogMetadataResponse.CatalogMetadata> list = new ArrayList<>();
        list.add(CampaignCatalogMetadataResponse.CatalogMetadata.builder()
                .catalogId(3L)
                .campaignActive(true)
                .campaignDetails(CampaignDetails.builder()
                        .campaignId(1L)
                        .campaignType(CampaignType.DAILY_BUDGET.getValue())
                        .cpc(new BigDecimal("0.92"))
                        .billVersion(2)
                        .budget(new BigDecimal("900"))
                        .build())
                .build());
        return list;
    }

    public AdInteractionEvent getSampleAdInteractionEvent() {
        AdInteractionEvent adInteractionEvent = new AdInteractionEvent();
        adInteractionEvent.setEventId("3234");
        adInteractionEvent.setEventName("ad_click");
        adInteractionEvent.setEventTimestamp(System.currentTimeMillis());
        AdInteractionEvent.Properties properties = new AdInteractionEvent.Properties();
        properties.setScreen("fsdf");
        properties.setOrigin("sdfdf");
        properties.setId(3L);
        adInteractionEvent.setProperties(properties);
        adInteractionEvent.setUserId("1");
        return adInteractionEvent;
    }

    public AdInteractionEvent getSampleAdInteractionEventWishlisted() {
        AdInteractionEvent adInteractionEvent = new AdInteractionEvent();
        adInteractionEvent.setEventId("3234");
        adInteractionEvent.setEventName("ad_wishlisted");
        adInteractionEvent.setEventTimestamp(System.currentTimeMillis());
        AdInteractionEvent.Properties properties = new AdInteractionEvent.Properties();
        properties.setScreen("fsdf");
        properties.setOrigin("sdfdf");
        properties.setId(3L);
        adInteractionEvent.setProperties(properties);
        adInteractionEvent.setUserId("1");

        return adInteractionEvent;
    }

    public AdInteractionEvent getSampleAdInteractionEventShared() {
        AdInteractionEvent adInteractionEvent = new AdInteractionEvent();
        adInteractionEvent.setEventId("3234");
        adInteractionEvent.setEventName("ad_shared");
        adInteractionEvent.setEventTimestamp(System.currentTimeMillis());
        AdInteractionEvent.Properties properties = new AdInteractionEvent.Properties();
        properties.setScreen("fsdf");
        properties.setOrigin("sdfdf");
        properties.setId(3L);
        adInteractionEvent.setProperties(properties);
        adInteractionEvent.setUserId("1");
        return adInteractionEvent;
    }

    public CampaignCatalogDateMetrics getSampleCampaignCatalogMetrics() {
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
        campaignCatalogDateMetrics.setCatalogId(3L);
        campaignCatalogDateMetrics.setCampaignId(1L);
        campaignCatalogDateMetrics.setDate(LocalDate.now());
        campaignCatalogDateMetrics.setClickCount(20L);
        return campaignCatalogDateMetrics;
    }

    public CampaignCatalogDateMetrics getSampleCampaignCatalogMetricsForBillShares() {
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
        campaignCatalogDateMetrics.setCatalogId(3L);
        campaignCatalogDateMetrics.setCampaignId(1L);
        campaignCatalogDateMetrics.setDate(LocalDate.now());
        campaignCatalogDateMetrics.setClickCount(2L);
        campaignCatalogDateMetrics.setSharesCount(3L);
        campaignCatalogDateMetrics.setWishlistCount(4L);
        return campaignCatalogDateMetrics;
    }

    public RealEstateMetadata getSampleRealEstateMetadata() {
        RealEstateMetadata realEstateMetadata = new RealEstateMetadata();
        realEstateMetadata.setClickMultiplier(new BigDecimal(2));
        return realEstateMetadata;
    }

    public LocalDate getSampleDate() {
        return DateTimeUtils.getCurrentLocalDateTimeInIST().toLocalDate();
    }

    @Test
    public void checkIfInteractionNeedsToBeConsideredSuccess() {
        long currentTime = System.currentTimeMillis();
        long previousInteractionTime = (System.currentTimeMillis()) - 3 * 86400000; // 3 days
        Mockito.doReturn(2 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        boolean toConsider =
                catalogInteractionEventService.checkIfInteractionNeedsToBeConsidered(previousInteractionTime,
                        currentTime);
        Assert.assertTrue(toConsider);
    }

    @Test
    public void checkIfInteractionNeedsToBeConsideredFailed() {
        long currentTime = System.currentTimeMillis();
        long previousInteractionTime = (System.currentTimeMillis()) - 3 * 86400000; // 3 days
        Mockito.doReturn(4 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        boolean toConsider =
                catalogInteractionEventService.checkIfInteractionNeedsToBeConsidered(previousInteractionTime,
                        currentTime);
        Assert.assertFalse(toConsider);
    }

    @Test
    public void checkIfInteractionNeedsToBeConsideredPreviousNull() {
        long currentTime = System.currentTimeMillis();
        Mockito.doReturn(4 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        boolean toConsider = catalogInteractionEventService.checkIfInteractionNeedsToBeConsidered(null, currentTime);
        Assert.assertTrue(toConsider);
    }

    @Test
    public void testForBillVersionOne() throws ExternalRequestFailedException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataList()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleCampaignCatalogMetrics()).when(campaignCatalogMetricsRepository).get(any(), any(), any());
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionOneStopCampaign() throws ExternalRequestFailedException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataList()).when(adService).getCampaignCatalogMetadata(any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetrics();
        campaignCatalogDateMetrics.setClickCount(22L);
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(campaignCatalogDateMetrics).when(campaignCatalogMetricsRepository).get(any(), any(), any());
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionSharesForClick() throws ExternalRequestFailedException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleCampaignCatalogMetricsForBillShares())
                .when(campaignCatalogMetricsRepository)
                .get(any(), any(), any());
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionSharesForClickNotUpdatingRedis() throws ExternalRequestFailedException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleCampaignCatalogMetricsForBillShares())
                .when(campaignCatalogMetricsRepository)
                .get(any(), any(), any());
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionSharesForShares() throws ExternalRequestFailedException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleCampaignCatalogMetricsForBillShares())
                .when(campaignCatalogMetricsRepository)
                .get(any(), any(), any());
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEventShared());
    }

    @Test
    public void testForBillVersionSharesForClickStopCampaign() throws ExternalRequestFailedException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setClickCount(14L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(campaignCatalogDateMetrics).when(campaignCatalogMetricsRepository).get(any(), any(), any());
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionSharesForSharesStopCampaign() throws ExternalRequestFailedException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setSharesCount(15L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(campaignCatalogDateMetrics).when(campaignCatalogMetricsRepository).get(any(), any(), any());
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEventShared());
    }

    @Test
    public void testForBillVersionSharesForWishlistStopCampaign() throws ExternalRequestFailedException {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setWishlistCount(16L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(campaignCatalogDateMetrics).when(campaignCatalogMetricsRepository).get(any(), any(), any());
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper)
                .getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEventWishlisted());
    }

}
