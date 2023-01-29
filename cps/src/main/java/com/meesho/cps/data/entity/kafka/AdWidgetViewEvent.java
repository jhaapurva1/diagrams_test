package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdWidgetViewEvent {

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

        @JsonProperty("widget_ids")
        private List<Long> widgetIds;

        @JsonProperty("catalog_id")
        private List<Long> catalogIds;

        @JsonProperty("campaign_id")
        private List<Long> campaignIds;

        @JsonProperty("widget_group_ids")
        private List<Long> widgetGroupIds;

        @JsonProperty("widget_group_position")
        private List<Integer> widgetGroupPosition;

        @JsonProperty("widget_group_titles")
        private List<String> widgetGroupTitles;

        @JsonProperty("screens")
        private List<String> screens;

        @JsonProperty("widget_screen_ids")
        private List<String> widgetScreenIds;

        @JsonProperty("origins")
        private List<String> origins;

        @JsonProperty("source_screens")
        private List<String> sourceScreens;

        @JsonProperty("primary_real_estates")
        private List<String> primaryRealEstates;

        @JsonProperty("app_version_code")
        private Integer appVersionCode;
    }
}
