package com.meesho.cps;

import com.google.common.collect.Lists;
import com.meesho.baseclient.pojos.ServiceRequest;
import com.meesho.baseclient.pojos.ServiceResponse;
import com.meesho.baseclient.pojos.ServiceRestConfig;
import com.meesho.cps.constants.CampaignType;
import com.meesho.cpsclient.request.*;
import com.meesho.cpsclient.response.*;
import com.meesho.cpsclient.service.CPSClientService;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
public class CPSClientTest {

    public static void main(String[] args) {
        RestTemplate restTemplate = new RestTemplate();

        ServiceRestConfig restConfig = new ServiceRestConfig();
        restConfig.setHost("172.31.17.193");
        restConfig.setPort(7090);
        restConfig.setAuthToken("dev");

        SupplierPerformanceRequest request = SupplierPerformanceRequest.builder().supplierId(1L).build();
        CampaignPerformanceRequest campaignPerformanceRequest = CampaignPerformanceRequest.builder()
            .campaignDetails(Lists.newArrayList(CampaignPerformanceRequest.CampaignDetails.builder()
                .campaignId(1L)
                .catalogIds(Lists.newArrayList(11L))
                .build()))
            .build();

        CPSClientService cpsClientService = new CPSClientService(restConfig, restTemplate, null);
        ServiceResponse<SupplierPerformanceResponse> supplierPerformanceResponseServiceResponse =
                cpsClientService.getSupplierPerformance(ServiceRequest.of(request));
        SupplierPerformanceResponse supplierPerformanceResponse =
                supplierPerformanceResponseServiceResponse.getResponse();
        System.out.println(supplierPerformanceResponse);

        ServiceResponse<CampaignPerformanceResponse> campaignPerformance =
                cpsClientService.getCampaignPerformance(ServiceRequest.of(campaignPerformanceRequest));
        CampaignPerformanceResponse campaignPerformanceResponse = campaignPerformance.getResponse();
        System.out.println(campaignPerformanceResponse);


        CampaignCatalogPerformanceRequest campaignCatalogPerformanceRequest =
                CampaignCatalogPerformanceRequest.builder()
                        .campaignId(1L)
                        .catalogIds(Lists.newArrayList(1L, 2L))
                        .build();
        ServiceResponse<CampaignCatalogPerformanceResponse> campaignCatalogPerformanceResponseServiceResponse =
                cpsClientService.getCampaignCatalogPerformance(ServiceRequest.of(campaignCatalogPerformanceRequest));
        CampaignCatalogPerformanceResponse campaignCatalogPerformanceResponse =
                campaignCatalogPerformanceResponseServiceResponse.getResponse();
        System.out.println(campaignCatalogPerformanceResponse);

        BudgetUtilisedRequest.CampaignData campaignData = BudgetUtilisedRequest.CampaignData.builder()
                .campaignId(1L)
                .campaignType(CampaignType.TOTAL_BUDGET.getValue())
                .build();
        List<BudgetUtilisedRequest.CampaignData> campaignDataList = new ArrayList<>();
        BudgetUtilisedRequest budgetUtilisedRequest = BudgetUtilisedRequest.builder()
                .campaignDataList(campaignDataList)
                .build();
        ServiceResponse<BudgetUtilisedResponse> budgetUtilisedResponseServiceResponse =
                cpsClientService.getCampaignBudgetUtilised(ServiceRequest.of(budgetUtilisedRequest));
        BudgetUtilisedResponse budgetUtilisedResponse = budgetUtilisedResponseServiceResponse.getResponse();
        System.out.println(budgetUtilisedResponse);

        FetchActiveCampaignsRequest fetchActiveCampaignsRequest = FetchActiveCampaignsRequest.builder()
                .date("2022-09-23")
                .limit(10)
                .cursor("")
                .build();
        ServiceResponse<FetchActiveCampaignsResponse> fetchCampaignsForDateServiceResponse =
                cpsClientService.getActiveCampaignsForDate(ServiceRequest.of(fetchActiveCampaignsRequest));
        FetchActiveCampaignsResponse fetchActiveCampaignsResponse = fetchCampaignsForDateServiceResponse.getResponse();
        System.out.println(fetchActiveCampaignsResponse);
    }

}
