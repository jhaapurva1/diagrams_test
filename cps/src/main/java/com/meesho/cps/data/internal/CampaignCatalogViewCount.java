package com.meesho.cps.data.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;


@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignCatalogViewCount {

    private Long supplierId;
    private Long campaignId;
    private Long catalogId;
    private LocalDate date;
    private Integer count;

}
