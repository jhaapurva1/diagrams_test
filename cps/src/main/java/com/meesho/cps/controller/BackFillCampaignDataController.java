package com.meesho.cps.controller;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.service.BackFillCampaignDataService;
import com.meesho.cpsclient.request.CampaignDataBackFillRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping(path = Constants.API.BASE_PATH)
public class BackFillCampaignDataController {

    @Autowired
    private BackFillCampaignDataService backFillCampaignDataService;

    @RequestMapping(path = "/campaign-details/hbase/backfill", method = RequestMethod.POST)
    public Map<String, Object> backFill(@Valid @RequestBody CampaignDataBackFillRequest campaignDataBackfillRequest) {
        Map<String, Object> processDetails = backFillCampaignDataService.backFill(campaignDataBackfillRequest);
        return processDetails;
    }

    @RequestMapping(path = "/campaign-details/hbase/backfill/status", method = RequestMethod.GET)
    public Map<String, Object> backFillStatus() {
        System.gc();
        return BackFillCampaignDataService.processDetails;
    }

}