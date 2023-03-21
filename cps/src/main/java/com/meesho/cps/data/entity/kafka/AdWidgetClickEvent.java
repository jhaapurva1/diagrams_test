package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdWidgetClickEvent {

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

        @JsonProperty("widget_id")
        private Long widgetId;

        @JsonProperty("catalog_id")
        private Long catalogId;

        @JsonProperty("campaign_id")
        private Long campaignId;

        @JsonProperty("widget_group_id")
        private Long widgetGroupId;

        @JsonProperty("widget_group_position")
        private Integer widgetGroupPosition;

        @JsonProperty("widget_group_title")
        private String widgetGroupTitle;

        @JsonProperty("screen")
        private String screen;

        @JsonProperty("screen_id")
        private String screenId;

        @JsonProperty("origin")
        private String origin;

        @JsonProperty("source_screen")
        private String sourceScreen;

        @JsonProperty("primary_real_estate")
        private String primaryRealEstate;

        @JsonProperty("app_version_code")
        private Integer appVersionCode;

        @JsonProperty("is_ad_widget")
        private Boolean isAdWidget;

        @JsonProperty("ads_metadata")
        private String adsMetadata;
    }
}
