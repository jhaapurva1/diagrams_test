package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.mysql.RealEstateMetadata;
import com.meesho.cps.db.hbase.repository.CampaignCatalogMetricsRepository;
import com.meesho.cps.db.redis.dao.RealEstateMetadataCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.factory.AdBillFactory;
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
import java.util.ArrayList;
import java.util.HashMap;
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
    private CampaignCatalogMetricsRepository campaignCatalogMetricsRepository;

    @Mock
    private RealEstateMetadataCacheDao realEstateMetadataCacheDao;

    @Mock
    private UserCatalogInteractionCacheDao userCatalogInteractionCacheDao;

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

    public CampaignCatalogMetrics getSampleCampaignCatalogMetrics() {
        CampaignCatalogMetrics campaignCatalogMetrics = new CampaignCatalogMetrics();
        campaignCatalogMetrics.setCatalogId(3L);
        campaignCatalogMetrics.setCampaignId(1L);
        campaignCatalogMetrics.setOriginWiseClickCount(new HashMap<>());
        campaignCatalogMetrics.setWeightedClickCount(new BigDecimal(20));
        return campaignCatalogMetrics;
    }

    public CampaignCatalogMetrics getSampleCampaignCatalogMetricsForBillShares() {
        CampaignCatalogMetrics campaignCatalogMetrics = new CampaignCatalogMetrics();
        campaignCatalogMetrics.setOriginWiseClickCount(new HashMap<>());
        campaignCatalogMetrics.setCatalogId(3L);
        campaignCatalogMetrics.setCampaignId(1L);
        campaignCatalogMetrics.setWeightedClickCount(new BigDecimal(2));
        campaignCatalogMetrics.setWeightedSharesCount(new BigDecimal(3));
        campaignCatalogMetrics.setWeightedWishlistCount(new BigDecimal(4));
        return campaignCatalogMetrics;
    }

    public RealEstateMetadata getSampleRealEstateMetadata() {
        RealEstateMetadata realEstateMetadata = new RealEstateMetadata();
        realEstateMetadata.setClickMultiplier(new BigDecimal(2));
        return realEstateMetadata;
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
    public void testForBillVersionOne() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataList()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(getSampleCampaignCatalogMetrics()).when(campaignCatalogMetricsRepository).get(1L, 3L);
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionOneStopCampaign() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataList()).when(adService).getCampaignCatalogMetadata(any());
        CampaignCatalogMetrics campaignCatalogMetrics = getSampleCampaignCatalogMetrics();
        campaignCatalogMetrics.setWeightedClickCount(new BigDecimal(22));
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        Mockito.doReturn(campaignCatalogMetrics).when(campaignCatalogMetricsRepository).get(1L, 3L);
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionSharesForClick() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(4 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        Mockito.doReturn(System.currentTimeMillis() - 5 * 86400000)
                .when(userCatalogInteractionCacheDao)
                .get(Mockito.anyString(), any(), any(), any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(getSampleCampaignCatalogMetricsForBillShares())
                .when(campaignCatalogMetricsRepository)
                .get(1L, 3L);
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doNothing().when(campaignCatalogMetricsRepository).put(any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionSharesForClickNotUpdatingRedis() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleCampaignCatalogMetricsForBillShares())
                .when(campaignCatalogMetricsRepository)
                .get(1L, 3L);
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionSharesForShares() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(getSampleCampaignCatalogMetricsForBillShares())
                .when(campaignCatalogMetricsRepository)
                .get(1L, 3L);
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doNothing().when(campaignCatalogMetricsRepository).put(any());
        catalogInteractionEventService.handle(getSampleAdInteractionEventShared());
    }

    @Test
    public void testForBillVersionSharesForClickStopCampaign() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        CampaignCatalogMetrics campaignCatalogMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogMetrics.setWeightedClickCount(new BigDecimal(14));
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(4 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        Mockito.doReturn(System.currentTimeMillis() - 5 * 86400000)
                .when(userCatalogInteractionCacheDao)
                .get(Mockito.anyString(), any(), any(), any());
        Mockito.doReturn(campaignCatalogMetrics).when(campaignCatalogMetricsRepository).get(1L, 3L);
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doNothing().when(campaignCatalogMetricsRepository).put(any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent());
    }

    @Test
    public void testForBillVersionSharesForSharesStopCampaign() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(4 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        CampaignCatalogMetrics campaignCatalogMetrics = getSampleCampaignCatalogMetricsForBillShares();
        Mockito.doReturn(System.currentTimeMillis() - 5 * 86400000)
                .when(userCatalogInteractionCacheDao)
                .get(Mockito.anyString(), any(), any(), any());
        campaignCatalogMetrics.setWeightedSharesCount(new BigDecimal(15));
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(campaignCatalogMetrics).when(campaignCatalogMetricsRepository).get(1L, 3L);
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doNothing().when(campaignCatalogMetricsRepository).put(any());
        catalogInteractionEventService.handle(getSampleAdInteractionEventShared());
    }

    @Test
    public void testForBillVersionSharesForWishlistStopCampaign() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleCatalogMetadataListBillVersion2()).when(adService).getCampaignCatalogMetadata(any());
        Mockito.doReturn(4 * 86400).when(applicationProperties).getUserCatalogInteractionWindowInSeconds();
        Mockito.doReturn(System.currentTimeMillis() - 5 * 86400000)
                .when(userCatalogInteractionCacheDao)
                .get(Mockito.anyString(), any(), any(), any());
        CampaignCatalogMetrics campaignCatalogMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogMetrics.setWeightedWishlistCount(new BigDecimal(16));
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        Mockito.doReturn(true).when(interactionBillHandler).performWindowDeDuplication();
        Mockito.doReturn(campaignCatalogMetrics).when(campaignCatalogMetricsRepository).get(1L, 3L);
        Mockito.doReturn(getSampleRealEstateMetadata())
                .when(realEstateMetadataCacheDao)
                .get(Mockito.anyString(), any());
        Mockito.doNothing().when(campaignCatalogMetricsRepository).put(any());
        catalogInteractionEventService.handle(getSampleAdInteractionEventWishlisted());
    }

}
