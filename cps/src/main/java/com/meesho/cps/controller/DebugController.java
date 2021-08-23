package com.meesho.cps.controller;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import com.meesho.cps.data.entity.hbase.CampaignDatewiseMetrics;
import com.meesho.cps.data.entity.hbase.CampaignMetrics;
import com.meesho.cps.data.request.CampaignCatalogMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignDatewiseMetricsSaveRequest;
import com.meesho.cps.data.request.CampaignMetricsSaveRequest;
import com.meesho.cps.service.DebugService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import io.swagger.annotations.ApiOperation;

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
    public CampaignCatalogMetrics saveCampaignCatalogMetrics(
            @Valid @RequestBody CampaignCatalogMetricsSaveRequest request) throws Exception {
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
    public CampaignCatalogMetrics getCampaignCatalogMetrics(@RequestParam("campaignId") Long campaignId,
                                                            @RequestParam("catalogId") Long catalogId)
            throws Exception {
        return debugService.getCampaignCatalogMetrics(campaignId, catalogId);
    }

}
