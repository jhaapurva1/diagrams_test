package com.meesho.cps.controller;

import com.meesho.cps.constants.Constants;
import com.meesho.cps.service.BackFillMissedEventsService;
import com.meesho.cpsclient.request.MissedEventsBackfillRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping(path = Constants.API.BASE_PATH)
public class BackfillMissedEventsController {

    @Autowired
    private BackFillMissedEventsService backFillMissedEventsService;

    @RequestMapping(path = "events/backfill/", method = RequestMethod.POST)
    public Map<String, Object> backFill(@Valid @RequestBody MissedEventsBackfillRequest missedEventsBackfillRequest) {
        Map<String, Object> lastProcessedId = backFillMissedEventsService.backFill(missedEventsBackfillRequest);
        return lastProcessedId;
    }

    @RequestMapping(path = "events/backfill/", method = RequestMethod.GET)
    public Map<String, Object> backFillStatus() {
        return BackFillMissedEventsService.processDetails;
    }
}
