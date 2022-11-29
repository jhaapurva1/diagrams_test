package com.meesho.cps.controller;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.service.CampaignPerformanceService;
import com.meesho.cpsclient.request.*;
import com.meesho.cpsclient.response.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.IOException;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@RestController
@RequestMapping(path = Constants.API.BASE_PATH)
public class CampaignPerformanceController {

    @Autowired
    private CampaignPerformanceService performanceService;

    @ApiOperation(value = Constants.API.SUPPLIER_PERFORMANCE, notes = "API to get overall campaign performance " +
            "metrics for a supplier", consumes = MediaType.APPLICATION_JSON_VALUE, produces =
            MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.SUPPLIER_PERFORMANCE, method = RequestMethod.POST, consumes =
            MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public SupplierPerformanceResponse getPerformanceMetricsForSupplier(
            @Valid @RequestBody SupplierPerformanceRequest supplierPerformanceRequest) throws Exception {
        return performanceService.getSupplierPerformanceMetrics(supplierPerformanceRequest);
    }

    @ApiOperation(value = Constants.API.CAMPAIGN_PERFORMANCE, notes = "API to get performance metrics on campaign " +
            "level", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.CAMPAIGN_PERFORMANCE, method = RequestMethod.POST, consumes =
            MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignPerformanceResponse getCampaignPerformanceMetrics(
            @Valid @RequestBody CampaignPerformanceRequest campaignPerformanceRequest) throws Exception {
        return performanceService.getCampaignPerformanceMetrics(campaignPerformanceRequest);
    }

    @ApiOperation(value = Constants.API.CAMPAIGN_CATALOG_PERFORMANCE, notes = "API to get performance metrics on " +
            "campaign catalog level", consumes = MediaType.APPLICATION_JSON_VALUE, produces =
            MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.CAMPAIGN_CATALOG_PERFORMANCE, method = RequestMethod.POST, consumes =
            MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignCatalogPerformanceResponse getCampaignCatalogPerformanceMetrics(
            @Valid @RequestBody CampaignCatalogPerformanceRequest campaignCatalogPerformanceRequest) throws Exception {
        return performanceService.getCampaignCatalogPerformanceMetrics(campaignCatalogPerformanceRequest);
    }

    @ApiOperation(value = Constants.API.CAMPAIGN_BUDGET_UTILISED, notes = "API to get budget utilised of campaign",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.CAMPAIGN_BUDGET_UTILISED, method = RequestMethod.POST, consumes =
            MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public BudgetUtilisedResponse getCampaignCatalogBudgetUtilised(
            @Valid @RequestBody BudgetUtilisedRequest budgetUtilisedRequest) throws Exception {
        return performanceService.getBudgetUtilised(budgetUtilisedRequest);
    }

    @ApiOperation(value = Constants.API.CAMPAIGN_CATALOG_DATE_LEVEL_BUDGET_UTILISED, notes = "API to get " +
            "campaign-catalog-date level budget utilised", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE )
    @RequestMapping(path = Constants.API.CAMPAIGN_CATALOG_DATE_LEVEL_BUDGET_UTILISED, method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignCatalogDateLevelBudgetUtilisedResponse getCampaignCatalogDateLevelBudgetUtilised(
            @Valid @RequestBody CampaignCatalogDateLevelBudgetUtilisedRequest request) {
        return performanceService.getDateLevelBudgetUtilised(request);
    }

    @ApiOperation(value = Constants.API.ACTIVE_CAMPAIGNS, notes = "API to get active campaigns for a date",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.ACTIVE_CAMPAIGNS, method = RequestMethod.POST, consumes =
            MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public FetchActiveCampaignsResponse getActiveCampaignsForDate(
            @Valid @RequestBody FetchActiveCampaignsRequest request) throws Exception {
        return performanceService.getActiveCampaignsForDate(request);
    }

    @ApiOperation(value = Constants.API.CAMPAIGN_CATALOG_PERFORMANCE_DATE_WISE,
            notes = "API to get campaign_catalog perf datewise",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.CAMPAIGN_CATALOG_PERFORMANCE_DATE_WISE, method = RequestMethod.POST, consumes =
            MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignPerformanceDatewiseResponse getCampaignCatalogPerfDateWise(
            @Valid @RequestBody CampaignCatalogPerfDatawiseRequest request) throws IOException {
        return performanceService.getCampaignCatalogPerfDatewise(request);
    }

}
