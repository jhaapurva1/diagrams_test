package com.meesho.cpsclient.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HbaseCampaignDataBackfillRequest {

    @NotNull
    @NotEmpty
    private String eventDate;

    @NotNull
    @NotEmpty
    private String dumpId;

    @NotNull
    private Integer batchSize;

    @NotNull
    private Boolean backfillCampaignCatalogDateMetrics;


}
