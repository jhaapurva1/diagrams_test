package com.meesho.cpsclient.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FetchActiveCampaignsRequest {

    @JsonProperty("date")
    @NotNull(message = "date is required")
    private String date;

    @JsonProperty("cursor")
    private String cursor;

    @JsonProperty("limit")
    @Max(1000)
    private Integer limit;

}
