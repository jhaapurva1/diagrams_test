package com.meesho.cps.service.external;

import com.google.common.collect.Lists;
import com.meesho.ad.client.request.AdViewEventMetadataRequest;
import com.meesho.ad.client.request.CampaignCatalogMetadataRequest;
import com.meesho.ad.client.request.CampaignMetadataRequest;
import com.meesho.ad.client.request.SupplierCampaignCatalogMetaDataRequest;
import com.meesho.ad.client.response.AdViewEventMetadataResponse;
import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ad.client.response.SupplierCampaignCatalogMetaDataResponse;
import com.meesho.ad.client.response.CampaignMetadataResponse;
import com.meesho.ad.client.service.AdsCatalogService;
import com.meesho.baseclient.pojos.ServiceRequest;
import com.meesho.baseclient.pojos.ServiceResponse;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.config.external.AdServiceClientConfig;
import com.meesho.cps.constants.BeanNames;
import com.meesho.cps.exception.ExternalRequestFailedException;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Service
@Slf4j
@DigestLogger(metricType = MetricType.HTTP, tagSet = "external_service=AdService")
public class AdService {

    private AdsCatalogService adsCatalogService;
    private ApplicationProperties applicationProperties;
    private final Cache<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> adViewCampaignCatalogCache;

    @Autowired
    public AdService(
            AdServiceClientConfig adServiceClientConfig,
            ApplicationProperties applicationProperties,
            @Qualifier(BeanNames.RestClients.AD_SERVICE) RestTemplate restTemplate,
            @Qualifier("adViewCampaignCatalogCacheManager") CacheManager adViewCampaignCatalogCacheManager) {
        this.adViewCampaignCatalogCache = (Cache<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata>)
                adViewCampaignCatalogCacheManager.getCache("adViewCampaignCatalogCache").getNativeCache();
        this.applicationProperties = applicationProperties;
        this.adsCatalogService = new AdsCatalogService(adServiceClientConfig.getRestConfig(), restTemplate, null,
                null, null);
    }

    private void putAllCampaignCatalogMetadata(
            Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> campaignCatalogMetadataMap) {
        adViewCampaignCatalogCache.putAll(campaignCatalogMetadataMap);
    }

    private Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> getAllCampaignCatalogMetadata(List<Long> catalogIds) {
        return adViewCampaignCatalogCache.getAllPresent(catalogIds);
    }

    /**
     * Gets CampaignCatalogMetadata from Local cache
     *
     * @param catalogIds List
     * @return
     * @throws ExternalRequestFailedException
     */
    public List<AdViewEventMetadataResponse.CatalogCampaignMetadata> getCampaignMetadataFromCache(List<Long> catalogIds) throws ExternalRequestFailedException {

        Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> localCampaignCatalogs = getAllCampaignCatalogMetadata(catalogIds);
        List<AdViewEventMetadataResponse.CatalogCampaignMetadata> allCatalogCampaignCatalogMetadata = new ArrayList<>(localCampaignCatalogs.values());

        List<Long> missedCatalogIds = catalogIds.stream()
                .filter(x -> !localCampaignCatalogs.containsKey(x))
                .collect(Collectors.toList());

        List<List<Long>> partitionedCatalogIds = Lists.partition(missedCatalogIds, applicationProperties.getAdServiceFetchCCMBatchSize());

        for (List<Long> toProcessCatIds : partitionedCatalogIds) {
            AdViewEventMetadataResponse campaignCatalogMetadataResponse = getAdViewEventCatalogCampaignStatus(toProcessCatIds);
            List<AdViewEventMetadataResponse.CatalogCampaignMetadata> missedCampaignCatalogs = campaignCatalogMetadataResponse.getCampaignStatusList();
            Map<Long, AdViewEventMetadataResponse.CatalogCampaignMetadata> missedCampaignCatalogMap = missedCampaignCatalogs.stream()
                    .collect(Collectors.toMap(AdViewEventMetadataResponse.CatalogCampaignMetadata::getCatalogId, Function.identity()));
            putAllCampaignCatalogMetadata(missedCampaignCatalogMap);
            log.info("Set missed catalogIds in local cache {}", toProcessCatIds);
            allCatalogCampaignCatalogMetadata.addAll(missedCampaignCatalogs);
        }

        return allCatalogCampaignCatalogMetadata;
    }

    public SupplierCampaignCatalogMetaDataResponse getSupplierCampaignCatalogMetadata(Long catalogId, Long campaignId, String userId,
                                                                                      String feedType, Long interactionTime) throws ExternalRequestFailedException {
        SupplierCampaignCatalogMetaDataRequest request = SupplierCampaignCatalogMetaDataRequest.builder()
                .campaignId(campaignId)
                .catalogId(catalogId)
                .feedType(feedType)
                .userId(userId)
                .interactionTime(interactionTime)
                .build();

        ServiceResponse<SupplierCampaignCatalogMetaDataResponse> response = null;
        try {
            response = adsCatalogService.fetchSupplierCampaignCatalogMetaData(ServiceRequest.of(request));
        } catch (Exception e) {
            log.error("fetch supplierCampaignCatalogMetadata call failed", e);
            throw new ExternalRequestFailedException(e.getMessage());
        }

        if (HttpStatus.OK.value() != response.getHttpStatus()) {
            log.error("getSupplierCampaignCatalogMetadata call failed with status code {}," +
                    " message {}", response.getHttpStatus(), response.getMessage());
            throw new ExternalRequestFailedException("non 2xx response");
        }

        if (Objects.isNull(response.getResponse()) || Objects.isNull(response.getResponse().getCatalogMetadata())) {
            log.error("getSupplierCampaignCatalogMetadata invalid response, response {}", response.getResponse());
            throw new ExternalRequestFailedException("null catalogMetaData");
        }

        return response.getResponse();
    }

    /**
     * Gets CampaignCatalogMetadata from Ad Service
     *
     * @param catalogIds List
     * @param campaignIds List
     * @return
     * @throws ExternalRequestFailedException
     */
    public CampaignCatalogMetadataResponse getCampaignCatalogMetadata(List<Long> catalogIds, List<Long> campaignIds, String userId, String feedType) throws ExternalRequestFailedException {
        log.info("getCampaignCatalogMetadata request, catalogIds {}", catalogIds);
        CampaignCatalogMetadataRequest request =
                CampaignCatalogMetadataRequest.builder().catalogIds(catalogIds).campaignIds(campaignIds).userId(userId).feedType(feedType).build();

        ServiceRequest<CampaignCatalogMetadataRequest> serviceRequest = ServiceRequest.of(request);

        ServiceResponse<CampaignCatalogMetadataResponse> response = null;
        try {
            response = adsCatalogService.fetchCampaignCatalogMetadata(serviceRequest);
        } catch (Exception e) {
            log.error("fetch campaignCatalogMetadata call failed", e);
            throw new ExternalRequestFailedException(e.getMessage());
        }

        if (HttpStatus.OK.value() != response.getHttpStatus()) {
            log.error("getCampaignCatalogMetadata call failed with error {}," +
                    " message {}", response.getHttpStatus(), response.getMessage());
            throw new ExternalRequestFailedException("non 2xx response");
        }

        if (Objects.isNull(response.getResponse()) ||
                CollectionUtils.isEmpty(response.getResponse().getCampaignDetailsList())) {
            log.error("getCampaignCatalogMetadata invalid response, response {}", response.getResponse());
            throw new ExternalRequestFailedException("empty response");
        }

        return response.getResponse();
    }

    public AdViewEventMetadataResponse getAdViewEventCatalogCampaignStatus(List<Long> catalogIds) throws ExternalRequestFailedException {
        log.info("getCampaignCatalogMetadata request, catalogIds {}", catalogIds);
        AdViewEventMetadataRequest request =
                AdViewEventMetadataRequest.builder().catalogIds(catalogIds).build();

        ServiceRequest<AdViewEventMetadataRequest> serviceRequest = ServiceRequest.of(request);

        ServiceResponse<AdViewEventMetadataResponse> response = null;
        try {
            response = adsCatalogService.fetchAdViewEventCatalogCampaignMetadata(serviceRequest);
        } catch (Exception e) {
            log.error("fetch campaignCatalogMetadata call failed", e);
            throw new ExternalRequestFailedException(e.getMessage());
        }

        if (HttpStatus.OK.value() != response.getHttpStatus()) {
            log.error("getCampaignCatalogMetadata call failed with error {}," +
                    " message {}", response.getHttpStatus(), response.getMessage());
            throw new ExternalRequestFailedException("non 2xx response");
        }

        if (Objects.isNull(response.getResponse()) ||
                CollectionUtils.isEmpty(response.getResponse().getCampaignStatusList())) {
            log.error("getCampaignCatalogMetadata invalid response, response {}", response.getResponse());
            throw new ExternalRequestFailedException("empty response");
        }

        return response.getResponse();
    }

    public List<CampaignDetails> getCampaignMetadata(List<Long> campaignIds) {
        log.info("getCampaignMetadata request, campaignIds {}", campaignIds);
        CampaignMetadataRequest request = CampaignMetadataRequest.builder().campaignIds(campaignIds).build();
        ServiceRequest<CampaignMetadataRequest> serviceRequest = ServiceRequest.of(request);
        ServiceResponse<CampaignMetadataResponse> response = null;

        try {
            response = adsCatalogService.fetchCampaignMetadata(serviceRequest);
        } catch (Exception e) {
            log.error("fetch campaignMetadata call failed", e);
            return null;
        }

        if (HttpStatus.OK.value() != response.getHttpStatus()) {
            log.error("getCampaignMetadata call failed with error {}, message {}", response.getHttpStatus(),
                    response.getMessage());
            return null;
        }

        if (Objects.isNull(response.getResponse()) ||
                CollectionUtils.isEmpty(response.getResponse().getCampaignDetailsList())) {
            log.error("getCampaignMetadata invalid response, response {}", response.getResponse());
            return null;
        }

        return response.getResponse().getCampaignDetailsList();
    }
}
