package com.meesho.cps.controller;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.request.BudgetExhaustedEventRequest;
import com.meesho.cps.data.request.CampaignCatalogDateMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignDatewiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;
import com.meesho.cps.service.DebugService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@RestController
@RequestMapping(Constants.API.DEBUG_API.BASE_PATH)
public class DebugController {

    @Autowired
    private DebugService debugService;

    @ApiOperation(value = Constants.API.DEBUG_API.SAVE_CAMPAIGN_CATALOG_METRICS, notes = "saves " +
            "campaignCatalogMetrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.SAVE_CAMPAIGN_CATALOG_METRICS, method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignCatalogDateMetrics saveCampaignCatalogMetrics(
            @Valid @RequestBody CampaignCatalogDateMetricsSaveRequest request) throws Exception {
        return debugService.saveCampaignCatalogMetrics(request);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.SAVE_CAMPAIGN_METRICS, notes = "saves campaignCatalogMetrics",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.SAVE_CAMPAIGN_METRICS, method = RequestMethod.POST, produces =
            MediaType.APPLICATION_JSON_VALUE)
    public CampaignMetrics saveCampaignMetrics(@Valid @RequestBody CampaignMetricsSaveRequest request)
            throws Exception {
        return debugService.saveCampaignMetrics(request);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.SAVE_CAMPAIGN_DATEWISE_METRICS, notes = "saves " +
            "campaignCatalogMetrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.SAVE_CAMPAIGN_DATEWISE_METRICS, method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignDatewiseMetrics saveCampaignDatewiseMetrics(
            @Valid @RequestBody CampaignDatewiseMetricsSaveRequest request) throws Exception {
        return debugService.saveCampaignDatewiseMetrics(request);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.GET_CAMPAIGN_DATEWISE_METRICS, notes = "returns " +
            "campaignDatewiseMetrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.GET_CAMPAIGN_DATEWISE_METRICS, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignDatewiseMetrics getCampaignDatewiseMetrics(@RequestParam("campaignId") Long campaignId,
                                                              @RequestParam("date") String date) throws Exception {
        return debugService.getCampaignDatewiseMetrics(campaignId, date);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.GET_CAMPAIGN_METRICS, notes = "returns campaignMetrics", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.GET_CAMPAIGN_METRICS, method = RequestMethod.GET, produces =
            MediaType.APPLICATION_JSON_VALUE)
    public CampaignMetrics getCampaignMetrics(@RequestParam("campaignId") Long campaignId) throws Exception {
        return debugService.getCampaignMetrics(campaignId);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.GET_CAMPAIGN_CATALOG_METRICS, notes = "returns " +
            "campaignCatalogMetrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.GET_CAMPAIGN_CATALOG_METRICS, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignCatalogDateMetrics getCampaignCatalogMetrics(@RequestParam("campaignId") Long campaignId,
                                                                @RequestParam("catalogId") Long catalogId,
                                                                @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date)
            throws Exception {
        return debugService.getCampaignCatalogMetrics(campaignId, catalogId, date);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.CAMPAIGN_PERFORMANCE_MIGRATE, notes = "returns " +
            "campaignCatalogMetrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.CAMPAIGN_PERFORMANCE_MIGRATE, method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void performMigrationOfCampaignPerformance() throws Exception {
        debugService.performMigrationOfCampaignPerformance();
    }

    @ApiOperation(value = Constants.API.DEBUG_API.BACKFILL_CAMPAIGN_CATALOG_DAY_PERFORMANCE_EVENT,
            notes = "Backfill the missing data from hbase, by calling prism api",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.DEBUG_API.BACKFILL_CAMPAIGN_CATALOG_DAY_PERFORMANCE_EVENT,
            method = RequestMethod.GET)
    public void postBackfillCampaignCatalogDayPerformanceEventsToPrism(@RequestParam("path") String path) {
        debugService.BackillCampaignCatalogDayPerformanceEventsToPrism(path);
    }

    @ApiOperation(value =  Constants.API.DEBUG_API.SEND_BUDGET_EXHAUSTED_EVENT,
            notes = "Sends budget exhausted event",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.DEBUG_API.SEND_BUDGET_EXHAUSTED_EVENT,
            method = RequestMethod.POST)
    public void sendBudgetExhaustedEvent(@Valid @RequestBody BudgetExhaustedEventRequest request) {
        debugService.sendBudgetExhaustedEvent(request);
    }

}
