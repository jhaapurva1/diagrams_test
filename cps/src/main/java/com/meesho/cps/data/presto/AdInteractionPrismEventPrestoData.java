package com.meesho.cps.data.presto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.meesho.prism.proxy.annotations.DataLakeColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdInteractionPrismEventPrestoData {

    @DataLakeColumn(name ="event_id")
    private String eventId;

    @DataLakeColumn(name ="event_name")
    private String eventName;

    @DataLakeColumn(name ="dump_id")
    private String dumpId;

    @DataLakeColumn(name ="campaign_id")
    private Long campaignId;

    @DataLakeColumn(name ="catalog_id")
    private Long catalogId;

    @DataLakeColumn(name ="user_id")
    private String userId;

    @DataLakeColumn(name ="origin")
    private String origin;

    @DataLakeColumn(name ="screen")
    private String screen;

    @DataLakeColumn(name ="status")
    private String status;

    @DataLakeColumn(name ="reason")
    private String reason;

    @DataLakeColumn(name ="cpc")
    private Float cpc;

    @DataLakeColumn(name ="click_multiplier")
    private Float clickMultiplier;

    @DataLakeColumn(name ="interaction_type")
    private String interactionType;

    @DataLakeColumn(name ="event_time")
    private Long eventTimestamp;

    @DataLakeColumn(name ="event_time_iso")
    private String eventTimeIso;

    @DataLakeColumn(name ="processed_at")
    private String currentTimestamp;

}
