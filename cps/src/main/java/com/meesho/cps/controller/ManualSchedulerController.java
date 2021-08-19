package com.meesho.cps.controller;

import com.meesho.ads.lib.data.internal.SchedulerProperty;
import com.meesho.ads.lib.factory.SchedulerFactory;
import com.meesho.ads.lib.scheduler.AbstractScheduler;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.Constants;
import com.meesho.cps.data.request.ManualSchedulerStartRequest;
import com.meesho.cps.data.response.ServiceResponse;
import com.meesho.cps.service.MigrationService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * @author shubham.aggarwal
 * 16/08/21
 */
@RestController
public class ManualSchedulerController {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private MigrationService migrationService;

    @ApiOperation(value = Constants.API.MANUAL_SCHEDULER_START, notes = "API to manually start cron", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.MANUAL_SCHEDULER_START, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    ServiceResponse<String> initScheduler(@RequestBody @Valid ManualSchedulerStartRequest schedulerStartRequest) throws Exception {
        String countryCode = MDC.get(com.meesho.ads.lib.constants.Constants.COUNTRY_CODE);
        AbstractScheduler scheduler = SchedulerFactory.getByType(schedulerStartRequest.getSchedulerType().name());
        Map<String, SchedulerProperty> configMap =
                applicationProperties.getSchedulerTypeCountryAndPropertyMap().get(schedulerStartRequest.getSchedulerType());
        SchedulerProperty schedulerProperty = configMap.get(countryCode);

        Long processedRows = scheduler.process(
                countryCode,
                schedulerProperty.getBatchSize(),
                ZonedDateTime.now(),
                schedulerProperty.getProcessBatchSize()
        );
        return new ServiceResponse<>("Completed, processed rows : " + processedRows, null, null);
    }

    @ApiOperation(value = Constants.API.MIGRATE_CAMPAIGN_PERFORMANCE, notes = "API to manually start CAMPAIGN PERFORMANCE migration", produces = MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.MIGRATE_CAMPAIGN_PERFORMANCE, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ServiceResponse<String> campaignPerformanceMigrationApi() throws Exception {
        Long updatedRows = migrationService.migrateCampaignPerformance();
        return new ServiceResponse<>("Completed, Updated rows : " + updatedRows, null, null);
    }

}
