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

    @ApiOperation(value = Constants.API.DEBUG_API.BACKFILL_CAMPAIGN_CATALOG_DAY_PERFORMANCE_EVENT,
            notes = "Backfill the missing data from hbase, by calling prism api",
    consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.DEBUG_API.BACKFILL_CAMPAIGN_CATALOG_DAY_PERFORMANCE_EVENT,
    method = RequestMethod.POST)
    public void postBackfillCampaignCatalogDayPerformanceEventsToPrism(){
        performanceService.BackillCampaignCatalogDayPerformanceEventsToPrism();
    }
}
