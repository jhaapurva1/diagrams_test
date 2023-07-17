package com.meesho.cps.controller;

import com.meesho.ad.client.constants.FeedType;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.internal.CampaignBudgetUtilisedData;
import com.meesho.cps.data.entity.kafka.CatalogBudgetExhaustEvent;
import com.meesho.cps.data.entity.kafka.SupplierWeeklyBudgetExhaustedEvent;
import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignDateWiseMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CampaignMetrics;
import com.meesho.cps.data.entity.mongodb.collection.CatalogCPCDiscount;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.request.BudgetExhaustedEventRequest;
import com.meesho.cps.data.request.CampaignCatalogDateMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignDateWiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;
import com.meesho.cps.data.request.CatalogCPCDiscountSaveRequest;
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
    public CampaignDateWiseMetrics saveCampaignDateWiseMetrics(
            @Valid @RequestBody CampaignDateWiseMetricsSaveRequest request) throws Exception {
        return debugService.saveCampaignDateWiseMetrics(request);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.GET_CAMPAIGN_DATEWISE_METRICS, notes = "returns " +
            "campaignDateWiseMetrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.GET_CAMPAIGN_DATEWISE_METRICS, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CampaignDateWiseMetrics getCampaignDateWiseMetrics(@RequestParam("campaignId") Long campaignId,
                                                              @RequestParam("date") String date) throws Exception {
        return debugService.getCampaignDateWiseMetrics(campaignId, date);
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

    @ApiOperation(value = Constants.API.DEBUG_API.BACKFILL_CAMPAIGN_CATALOG_DAY_PERFORMANCE_EVENT,
            notes = "BackFill the missing data from mongo, by calling prism api",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.DEBUG_API.BACKFILL_CAMPAIGN_CATALOG_DAY_PERFORMANCE_EVENT,
            method = RequestMethod.GET)
    public void postBackFillCampaignCatalogDayPerformanceEventsToPrism(@RequestParam("path") String path) {
        debugService.BackillCampaignCatalogDayPerformanceEventsToPrism(path);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.GET_CATALOG_CPC_DISCOUNT, notes = "debug api to get catalog cpc discount",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.GET_CATALOG_CPC_DISCOUNT, method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CatalogCPCDiscount getCatalogCPCDiscount(@RequestParam("catalogId") Long catalogId) throws Exception {
        return debugService.getCatalogCPCDiscount(catalogId);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.SAVE_CATALOG_CPC_DISCOUNT, notes = "debug api to save catalog cpc discount",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.SAVE_CATALOG_CPC_DISCOUNT, method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CatalogCPCDiscount saveCatalogCPCDiscount(@Valid @RequestBody CatalogCPCDiscountSaveRequest request)
            throws Exception {
        return debugService.saveCatalogCPCDiscount(request);
    }

    @ApiOperation(value =  Constants.API.DEBUG_API.SEND_BUDGET_EXHAUSTED_EVENT,
            notes = "Sends budget exhausted event",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.DEBUG_API.SEND_BUDGET_EXHAUSTED_EVENT,
            method = RequestMethod.POST)
    public void sendBudgetExhaustedEvent(@Valid @RequestBody BudgetExhaustedEventRequest request) {
        debugService.sendBudgetExhaustedEvent(request);
    }

    @ApiOperation(value =  Constants.API.DEBUG_API.SEND_CATALOG_BUDGET_EXHAUSTED_EVENT,
            notes = "Sends catalog budget exhausted event",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.DEBUG_API.SEND_CATALOG_BUDGET_EXHAUSTED_EVENT,
            method = RequestMethod.POST)
    public void sendCatalogBudgetExhaustedEvent(@Valid @RequestBody CatalogBudgetExhaustEvent request) {
        debugService.sendCatalogBudgetExhaustEvent(request);
    }

    @ApiOperation(value =  Constants.API.DEBUG_API.SEND_SUPPLIER_BUDGET_EXHAUSTED_EVENT,
            notes = "Sends supplier budget exhausted event",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(path = Constants.API.DEBUG_API.SEND_SUPPLIER_BUDGET_EXHAUSTED_EVENT,
            method = RequestMethod.POST)
    public void sendSupplierBudgetExhaustedEvent(@Valid @RequestBody SupplierWeeklyBudgetExhaustedEvent request) {
        debugService.sendSupplierBudgetExhaustedEvent(request);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.PRODUCE_KAFKA, notes = "debug api to kafka",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.PRODUCE_KAFKA, method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void publishKafkaInteractionEvent( @RequestBody AdInteractionEvent adInteractionEvent)
            throws Exception {
        debugService.publishKafkaInteractionEvent(adInteractionEvent);
    }

    @ApiOperation(value = Constants.API.DEBUG_API.INCREMENT_BUDGET_UTILISED, notes = "returns incremented budget utilised info", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.DEBUG_API.INCREMENT_BUDGET_UTILISED, method = RequestMethod.POST, produces =
            MediaType.APPLICATION_JSON_VALUE)
    public CampaignBudgetUtilisedData incrementBudgetUtilised(@RequestParam("campaignId") Long campaignId,
                                                              @RequestParam("realEstate") FeedType realEstate,
                                                              @RequestParam("budget") Double budget) throws Exception {
        return debugService.incrementBudgetUtilised(campaignId, budget, realEstate);
    }
}
