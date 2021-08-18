package com.meesho.cps.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meesho.cps.constants.SchedulerType;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author shubham.aggarwal
 * 16/08/21
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManualSchedulerStartRequest {

    @NotNull(message = "scheduler_type is required")
    @JsonProperty("scheduler_type")
    private SchedulerType schedulerType;

    @NotNull(message = "start time is required")
    @JsonProperty("start_time")
    private String startTime;

}
