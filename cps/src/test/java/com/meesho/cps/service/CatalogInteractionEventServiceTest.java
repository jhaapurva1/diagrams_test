package com.meesho.cps.service;

import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.db.hbase.repository.CampaignCatalogDateMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignDatewiseMetricsRepository;
import com.meesho.cps.db.hbase.repository.CampaignMetricsRepository;
import com.meesho.cps.db.hbase.repository.SupplierWeekWiseMetricsRepository;
import com.meesho.cps.db.redis.dao.UpdatedCampaignCatalogCacheDao;
import com.meesho.cps.db.redis.dao.UserCatalogInteractionCacheDao;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.cps.factory.AdBillFactory;
import com.meesho.cps.helper.CampaignPerformanceHelper;
import com.meesho.cps.service.external.AdService;
import com.meesho.cps.service.external.PrismService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
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
    private AdBillFactory adBillFactory;

    @Mock
    private AdService adService;

    @Mock
    private PrismService prismService;

    @Spy
    private ClickBillHandlerImpl clickBillHandler;

    @Spy
    private InteractionBillHandlerImpl interactionBillHandler;

    @Mock
    private UserCatalogInteractionCacheDao userCatalogInteractionCacheDao;

    @Mock
    private CampaignDatewiseMetricsRepository campaignDatewiseMetricsRepository;

    @Mock
    private SupplierWeekWiseMetricsRepository supplierWeekWiseMetricsRepository;

    @Mock
    private CampaignMetricsRepository campaignMetricsRepository;

    @Mock
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Mock
    private CampaignPerformanceHelper campaignPerformanceHelper;

    @Mock
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @InjectMocks
    private CatalogInteractionEventService catalogInteractionEventService;

    @Before
    public void setUp() {
        Mockito.doNothing().when(prismService).publishEvent(any(), any());
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper).getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(1000)).when(campaignDatewiseMetricsRepository).incrementBudgetUtilised(any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(200)).when(supplierWeekWiseMetricsRepository).incrementBudgetUtilised(any(), any(), any());
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

    public AdInteractionEvent getSampleAdInteractionEvent(String eventName) {
        AdInteractionEvent adInteractionEvent = new AdInteractionEvent();
        adInteractionEvent.setEventId("3234");
        adInteractionEvent.setEventName(eventName);
        adInteractionEvent.setEventTimestamp(System.currentTimeMillis());
        AdInteractionEvent.Properties properties = new AdInteractionEvent.Properties();
        properties.setScreen("fsdf");
        properties.setOrigin("sdfdf");
        properties.setId(3L);
        adInteractionEvent.setProperties(properties);
        adInteractionEvent.setUserId("1");
        return adInteractionEvent;
    }

    public AdInteractionEvent getSampleNewAdInteractionEvent(String eventName) {
        AdInteractionEvent adInteractionEvent = new AdInteractionEvent();
        adInteractionEvent.setEventId("3234");
        adInteractionEvent.setEventName(eventName);
        adInteractionEvent.setEventTimestamp(System.currentTimeMillis());
        AdInteractionEvent.Properties properties = new AdInteractionEvent.Properties();
        properties.setScreen("fsdf");
        properties.setOrigin("sdfdf");
        properties.setId(4L);
        properties.setCampaignId(3L);
        properties.setCpc(new BigDecimal(4));
        adInteractionEvent.setProperties(properties);
        adInteractionEvent.setUserId("1");
        return adInteractionEvent;
    }

    public CampaignCatalogMetadataResponse getSampleCatalogMetadataResponseForNewRequests(int billVersion) {
        List<CampaignCatalogMetadataResponse.CatalogMetadata> list = new ArrayList<>();
        list.add(CampaignCatalogMetadataResponse.CatalogMetadata.builder()
                .campaignActive(true)
                .campaignDetails(CampaignDetails.builder()
                        .campaignId(3L)
                        .campaignType(CampaignType.DAILY_BUDGET.getValue())
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
        Mockito.doReturn(getSampleCatalogMetadataResponse(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionOneStopCampaign() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponse(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetrics();
        campaignCatalogDateMetrics.setClickCount(22L);
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForClick() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForClickNotUpdatingRedis() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForShares() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_shared"));
    }

    @Test
    public void testForBillVersionSharesForClickStopCampaign() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setClickCount(14L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForSharesStopCampaign() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setSharesCount(15L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_shared"));
    }

    @Test
    public void testForBillVersionSharesForWishlistStopCampaign() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponse(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setWishlistCount(16L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_wishlisted"));
    }

    @Test
    public void testForBillVersionOneNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponseForNewRequests(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionOneStopCampaignNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponseForNewRequests(1)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetrics();
        campaignCatalogDateMetrics.setClickCount(22L);
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(1);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForClickNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponseForNewRequests(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForClickNotUpdatingRedisNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponseForNewRequests(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForSharesNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponseForNewRequests(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_shared"));
    }

    @Test
    public void testForBillVersionSharesForClickStopCampaignNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponseForNewRequests(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setClickCount(14L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForSharesStopCampaignNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponseForNewRequests(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setSharesCount(15L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_shared"));
    }

    @Test
    public void testForBillVersionSharesForWishlistStopCampaignNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleCatalogMetadataResponseForNewRequests(2)).when(adService).getCampaignCatalogMetadata(any(), any(), any(), any());
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = getSampleCampaignCatalogMetricsForBillShares();
        campaignCatalogDateMetrics.setWishlistCount(16L);
        Mockito.doReturn(interactionBillHandler).when(adBillFactory).getBillHandlerForBillVersion(2);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_wishlisted"));
    }

}
