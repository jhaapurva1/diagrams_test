package com.meesho.cps.service;

import com.meesho.ad.client.data.AdsMetadata;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ad.client.response.SupplierCampaignCatalogMetaDataResponse;
import com.meesho.ads.lib.helper.TelegrafMetricsHelper;
import com.meesho.ads.lib.utils.DateTimeUtils;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cps.data.entity.internal.BudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.db.mongodb.dao.CampaignCatalogDateMetricsDao;
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
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.*;

/**
 * @author shubham.aggarwal
 * 11/08/21
 */
@RunWith(MockitoJUnitRunner.class)
public class CatalogInteractionEventServiceTest {

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private AdBillFactory adBillFactory;

    @Mock
    private AdService adService;

    @Spy
    private ClickBillHandlerImpl clickBillHandler;

    @Spy
    private InteractionBillHandlerImpl interactionBillHandler;

    @Mock
    private UserCatalogInteractionCacheDao userCatalogInteractionCacheDao;

    @Mock
    private TelegrafMetricsHelper telegrafMetricsHelper;

    @Mock
    private CampaignPerformanceHelper campaignPerformanceHelper;

    @Mock
    private UpdatedCampaignCatalogCacheDao updatedCampaignCatalogCacheDao;

    @Mock
    private InteractionEventAttributionHelper interactionEventAttributionHelper;

    @Mock
    private CampaignCatalogDateMetricsDao campaignCatalogDateMetricsDao;

    @InjectMocks
    private CatalogInteractionEventService catalogInteractionEventService;

    @Before
    public void setUp() {
        Mockito.doReturn(getSampleDate()).when(campaignPerformanceHelper).getLocalDateForDailyCampaignFromLocalDateTime(any());
        Mockito.doNothing().when(telegrafMetricsHelper).increment(any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(200)).when(interactionEventAttributionHelper).modifyAndGetSupplierWeeklyBudgetUtilised(any(), any(), any());
        Mockito.doReturn(getAdsMetaDataEncryptionKey()).when(applicationProperties).getAdsMetadataEncryptionKey();
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
                        .campaignBudgetUtilisedBenchmark(new BigDecimal(50))
                        .build())
                .build();
        SupplierCampaignCatalogMetaDataResponse.SupplierMetadata supplierMetadata = SupplierCampaignCatalogMetaDataResponse.SupplierMetadata.builder().supplierId(3L)
                .budgetUtilisationLimit(BigDecimal.valueOf(100)).build();
        return SupplierCampaignCatalogMetaDataResponse.builder().catalogMetadata(catalogMetadata)
                .supplierMetadata(supplierMetadata).build();
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
        properties.setAdsMetadata(AdsMetadata.builder().campaignId(3l).cpc(new BigDecimal(4).doubleValue()).build().encrypt(getAdsMetaDataEncryptionKey()));
        adInteractionEvent.setProperties(properties);
        adInteractionEvent.setUserId("1");
        return adInteractionEvent;
    }

    public SupplierCampaignCatalogMetaDataResponse getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(int billVersion) {
        SupplierCampaignCatalogMetaDataResponse.CatalogMetadata catalogMetadata = SupplierCampaignCatalogMetaDataResponse.CatalogMetadata.builder()
                .campaignActive(true)
                .catalogBudget(BigDecimal.valueOf(50))
                .campaignDetails(CampaignDetails.builder()
                        .campaignId(3L)
                        .campaignType(CampaignType.DAILY_BUDGET.getValue())
                        .billVersion(billVersion)
                        .budget(new BigDecimal("900"))
                        .build())
                .build();
        SupplierCampaignCatalogMetaDataResponse.SupplierMetadata supplierMetadata = SupplierCampaignCatalogMetaDataResponse.SupplierMetadata.builder().supplierId(3L)
                .budgetUtilisationLimit(BigDecimal.valueOf(100)).build();
        return SupplierCampaignCatalogMetaDataResponse.builder().catalogMetadata(catalogMetadata)
                .supplierMetadata(supplierMetadata).build();
    }

    public CampaignCatalogDateMetrics getSampleDocument() {
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
        campaignCatalogDateMetrics.setCatalogId(3L);
        campaignCatalogDateMetrics.setCampaignId(1L);
        campaignCatalogDateMetrics.setDate(LocalDate.now().toString());
        campaignCatalogDateMetrics.setClicks(20L);
        return campaignCatalogDateMetrics;
    }

    public CampaignCatalogDateMetrics getSampleDocumentForBillShares() {
        CampaignCatalogDateMetrics campaignCatalogDateMetrics = new CampaignCatalogDateMetrics();
        campaignCatalogDateMetrics.setCatalogId(3L);
        campaignCatalogDateMetrics.setCampaignId(1L);
        campaignCatalogDateMetrics.setDate(LocalDate.now().toString());
        campaignCatalogDateMetrics.setClicks(2L);
        campaignCatalogDateMetrics.setShares(3L);
        campaignCatalogDateMetrics.setWishlists(4L);
        return campaignCatalogDateMetrics;
    }

    public LocalDate getSampleDate() {
        return DateTimeUtils.getCurrentLocalDateTimeInIST().toLocalDate();
    }

    @Test
    public void testForBillVersionOne() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionOneStopCampaign() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        CampaignCatalogDateMetrics document = getSampleDocument();
        document.setClicks(22L);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForClick() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForClickNotUpdatingRedis() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForShares() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_shared"));
    }

    @Test
    public void testForBillVersionSharesForClickStopCampaign() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        CampaignCatalogDateMetrics document = getSampleDocumentForBillShares();
        document.setClicks(14L);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForSharesStopCampaign() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        CampaignCatalogDateMetrics document = getSampleDocumentForBillShares();
        document.setShares(15L);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_shared"));
    }

    @Test
    public void testForBillVersionSharesForWishlistStopCampaign() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        CampaignCatalogDateMetrics document = getSampleDocumentForBillShares();
        document.setWishlists(16L);
        catalogInteractionEventService.handle(getSampleAdInteractionEvent("ad_wishlisted"));
    }

    @Test
    public void testForBillVersionOneNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionOneStopCampaignNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(1)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        CampaignCatalogDateMetrics document = getSampleDocument();
        document.setClicks(22L);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForClickNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForClickNotUpdatingRedisNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForSharesNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_shared"));
    }

    @Test
    public void testForBillVersionSharesForClickStopCampaignNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        CampaignCatalogDateMetrics document = getSampleDocumentForBillShares();
        document.setClicks(14L);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_click"));
    }

    @Test
    public void testForBillVersionSharesForSharesStopCampaignNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        CampaignCatalogDateMetrics document = getSampleDocumentForBillShares();
        document.setShares(15L);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_shared"));
    }

    @Test
    public void testForBillVersionSharesForWishlistStopCampaignNew() throws ExternalRequestFailedException {
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        CampaignCatalogDateMetrics document = getSampleDocumentForBillShares();
        document.setWishlists(16L);
        catalogInteractionEventService.handle(getSampleNewAdInteractionEvent("ad_wishlisted"));
    }

    @Test
    public void testCatalogBudgetExhaust() throws ExternalRequestFailedException {
        Mockito.doReturn(BudgetUtilisedData.builder()
                        .campaignBudgetUtilised(BigDecimal.valueOf(800))
                        .realEstateBudgetUtilisedMap(Collections.emptyMap())
                        .catalogBudgetUtilised(BigDecimal.valueOf(100)).build())
                .when(interactionEventAttributionHelper)
                .modifyAndGetBudgetUtilised(any(), any(), any(), any(), any(), any(), any(), any());
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponseForNewRequests(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(any());
        Mockito.doNothing().when(interactionEventAttributionHelper).sendCatalogBudgetExhaustEvent(any(), any());
        Mockito.doReturn(BigDecimal.valueOf(0.42)).when(interactionEventAttributionHelper).getChargeableCpc(any(), any(), any());
        AdInteractionEvent adInteractionEvent = getSampleNewAdInteractionEvent("ad_click");
        Long catalogId = adInteractionEvent.getProperties().getId();
        catalogInteractionEventService.handle(adInteractionEvent);
        Mockito.verify(interactionEventAttributionHelper, Mockito.times(1)).sendCatalogBudgetExhaustEvent(any(), eq(catalogId));
    }

    @Test
    public void testCampaignBudgetPacing() throws ExternalRequestFailedException {
        Mockito.doReturn(BudgetUtilisedData.builder().campaignBudgetUtilised(BigDecimal.valueOf(600))
                        .catalogBudgetUtilised(BigDecimal.valueOf(100)).build())
                .when(interactionEventAttributionHelper).modifyAndGetBudgetUtilised(any(), any(), any(), any(), any(), any(), any(), any());
        Mockito.doReturn(getSampleSupplierCampaignCatalogMetaDataResponse(2)).when(adService).getSupplierCampaignCatalogMetadata(any(), any(), any(), any(), any());
        Mockito.doReturn(BigDecimal.valueOf(0.42)).when(interactionEventAttributionHelper).getChargeableCpc(any(), any(), any());
        Mockito.doReturn(clickBillHandler).when(adBillFactory).getBillHandlerForBillVersion(any());
        Mockito.doReturn(BigDecimal.valueOf(450)).when(interactionEventAttributionHelper).getExpectedCampaignBudgetUtilised(any(), any());
        AdInteractionEvent adInteractionEvent = getSampleNewAdInteractionEvent("ad_click");
        Long catalogId = adInteractionEvent.getProperties().getId();
        catalogInteractionEventService.handle(adInteractionEvent);
        Mockito.verify(interactionEventAttributionHelper, Mockito.times(1)).sendBudgetPacedEvent(any(), eq(catalogId));
    }

    private String getAdsMetaDataEncryptionKey() {
        return "default";
    }

}
