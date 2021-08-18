package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdInteractionEvent {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_name")
    private String eventName;

    @JsonProperty("event_time")
    private Long eventTimestamp;

    @JsonProperty("event_time_iso")
    private String eventTimeIso;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("properties")
    private Properties properties;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Properties {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("type")
        private String type;

        @JsonProperty("origin")
        private String origin;

        @JsonProperty("screen")
        private String screen;

        @JsonProperty("app_version_code")
        private Integer appVersionCode;

    }

}
