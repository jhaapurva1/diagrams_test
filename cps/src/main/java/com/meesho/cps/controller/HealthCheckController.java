package com.meesho.cps.controller;

import com.meesho.cps.constants.Constants;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.ApiOperation;

/**
 * @author shubham.aggarwal
 * 10/08/21
 */
@RestController
public class HealthCheckController {

    @ApiOperation(value = Constants.API.HEALTH_CHECK_ENDPOINT, notes = "Health check api", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @RequestMapping(value = Constants.API.HEALTH_CHECK_ENDPOINT, method = RequestMethod.GET, produces =
            MediaType.APPLICATION_JSON_VALUE)
    String healthCheck() throws Exception {
        return "pong";
    }

}
