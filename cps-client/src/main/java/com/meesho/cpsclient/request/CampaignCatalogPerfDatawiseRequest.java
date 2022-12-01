package com.meesho.cpsclient.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignCatalogPerfDatawiseRequest {

    @NotNull(message = "campaign id is required")
    @JsonProperty("campaign_id")
    private Long campaignId;

    @NotNull(message = "start date can't be null")
    @JsonProperty("start_date")
    private LocalDate startDate;

    @NotNull(message = "end date can't be null")
    @JsonProperty("end_date")
    private LocalDate endDate;

}
