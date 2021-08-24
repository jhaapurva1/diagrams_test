package com.meesho.cps.service.external;

import com.meesho.ad.client.request.CampaignCatalogMetadataRequest;
import com.meesho.ad.client.request.CampaignMetadataRequest;
import com.meesho.ad.client.response.CampaignCatalogMetadataResponse;
import com.meesho.ad.client.response.CampaignDetails;
import com.meesho.ad.client.response.CampaignMetadataResponse;
import com.meesho.ad.client.service.AdsCatalogService;
import com.meesho.baseclient.pojos.ServiceRequest;
import com.meesho.baseclient.pojos.ServiceResponse;
import com.meesho.cps.config.external.AdServiceClientConfig;
import com.meesho.cps.constants.BeanNames;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Service
@Slf4j
@DigestLogger(metricType = MetricType.HTTP, tagSet = "external_service=AdService")
public class AdService {

    @Autowired
    @Qualifier(BeanNames.RestClients.AD_SERVICE)
    private RestTemplate restTemplate;

    @Autowired
    private AdServiceClientConfig adServiceClientConfig;

    private AdsCatalogService adsCatalogService;

    @PostConstruct
    public void init() {
        adsCatalogService = new AdsCatalogService(adServiceClientConfig.getRestConfig(), restTemplate);
    }

    @Cacheable(value = "campaignMetadata", cacheManager = "adServiceGetCampaignCatalogMetadataCacheManager",
            unless = "#result == null ")
    public CampaignCatalogMetadataResponse.CatalogMetadata getCampaignMetadataFromCache(Long catalogId) {
        List<CampaignCatalogMetadataResponse.CatalogMetadata> catalogMetadataList =
                getCampaignCatalogMetadata(Arrays.asList(catalogId));
        if (CollectionUtils.isEmpty(catalogMetadataList)) {
            return null;
        } else {
            return catalogMetadataList.get(0);
        }
    }

    public List<CampaignCatalogMetadataResponse.CatalogMetadata> getCampaignCatalogMetadata(List<Long> catalogIds) {
        log.info("getCampaignCatalogMetadata request, catalogIds {}", catalogIds);
        CampaignCatalogMetadataRequest request =
                CampaignCatalogMetadataRequest.builder().catalogIds(catalogIds).build();

        ServiceRequest<CampaignCatalogMetadataRequest> serviceRequest = ServiceRequest.of(request);

        ServiceResponse<CampaignCatalogMetadataResponse> response = null;
        try {
            response = adsCatalogService.fetchCampaignCatalogMetadata(serviceRequest);
        } catch (Exception e) {
            log.error("fetch campaignCatalogMetadata call failed", e);
            return null;
        }

        if (HttpStatus.OK.value() != response.getHttpStatus()) {
            log.error("getCampaignCatalogMetadata call failed with error {}, message {}", response.getHttpStatus(),
                    response.getMessage());
            return null;
        }

        if (Objects.isNull(response.getResponse()) ||
                CollectionUtils.isEmpty(response.getResponse().getCampaignDetailsList())) {
            log.error("getCampaignCatalogMetadata invalid response, response {}", response.getResponse());
            return null;
        }

        return response.getResponse().getCampaignDetailsList();
    }

    public List<CampaignDetails> getCampaignMetadata(List<Long> campaignIds) {
        log.info("getCampaignMetadata request, catalogIds {}", campaignIds);
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
