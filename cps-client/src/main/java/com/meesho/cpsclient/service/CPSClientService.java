package com.meesho.cpsclient.service;

import com.meesho.baseclient.pojos.BaseHTTPHandler;
import com.meesho.baseclient.pojos.ServiceRequest;
import com.meesho.baseclient.pojos.ServiceResponse;
import com.meesho.baseclient.pojos.ServiceRestConfig;
import com.meesho.cpsclient.request.*;
import com.meesho.cpsclient.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

/**
 * @author shubham.aggarwal
 * 27/07/21
 */
@RequiredArgsConstructor
public class CPSClientService extends BaseHTTPHandler {

    private static final String GET_SUPPLIER_PERFORMANCE = "/api/v1/supplier/performance";
    private static final String GET_CAMPAIGN_PERFORMANCE = "/api/v1/campaign/performance";
    private static final String GET_CAMPAIGN_CATALOG_PERFORMANCE = "/api/v1/campaign-catalog/performance";
    private static final String GET_BUDGET_UTILISED = "/api/v1/campaign/budget-utilised";

    private final ServiceRestConfig serviceRestConfig;
    private final RestTemplate restTemplate;

    public HttpHeaders getHeaders(ServiceRequest<?> serviceRequest) {
        HttpHeaders headers = super.getHeaders(serviceRequest);
        headers.add(HttpHeaders.AUTHORIZATION, serviceRestConfig.getAuthToken());
        return headers;
    }

    public ServiceResponse<SupplierPerformanceResponse> getSupplierPerformance(
            ServiceRequest<SupplierPerformanceRequest> supplierPerformanceRequest) {
        SupplierPerformanceResponse response = restTemplate.postForObject(
                serviceRestConfig.getURL(GET_SUPPLIER_PERFORMANCE),
                new HttpEntity<>(supplierPerformanceRequest.getRequest(), getHeaders(supplierPerformanceRequest)),
                SupplierPerformanceResponse.class);
        return ServiceResponse.ofSuccess(response);
    }

    public ServiceResponse<CampaignPerformanceResponse> getCampaignPerformance(
            ServiceRequest<CampaignPerformanceRequest> campaignPerformanceRequest) {
        CampaignPerformanceResponse response = restTemplate.postForObject(
                serviceRestConfig.getURL(GET_CAMPAIGN_PERFORMANCE),
                new HttpEntity<>(
                        campaignPerformanceRequest.getRequest(), getHeaders(campaignPerformanceRequest)),
                CampaignPerformanceResponse.class);
        return ServiceResponse.ofSuccess(response);
    }

    public ServiceResponse<CampaignCatalogPerformanceResponse> getCampaignCatalogPerformance(
            ServiceRequest<CampaignCatalogPerformanceRequest> campaignCatalogPerformanceRequest) {
        CampaignCatalogPerformanceResponse response = restTemplate.postForObject(
                serviceRestConfig.getURL(GET_CAMPAIGN_CATALOG_PERFORMANCE),
                new HttpEntity<>(
                        campaignCatalogPerformanceRequest.getRequest(),
                        getHeaders(campaignCatalogPerformanceRequest)),
                CampaignCatalogPerformanceResponse.class);
        return ServiceResponse.ofSuccess(response);
    }

    public ServiceResponse<BudgetUtilisedResponse> getCampaignBudgetUtilised(
            ServiceRequest<BudgetUtilisedRequest> budgetUtilisedRequest) {
        BudgetUtilisedResponse response = restTemplate.postForObject(
                serviceRestConfig.getURL(GET_BUDGET_UTILISED),
                new HttpEntity<>(budgetUtilisedRequest.getRequest(), getHeaders(budgetUtilisedRequest)),
                BudgetUtilisedResponse.class);
        return ServiceResponse.ofSuccess(response);
    }

}
