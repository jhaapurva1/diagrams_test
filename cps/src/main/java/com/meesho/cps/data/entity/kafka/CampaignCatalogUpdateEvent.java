package com.meesho.cps.data.entity.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignCatalogUpdateEvent {

    @JsonProperty("catalogs")
    private List<CatalogData> catalogs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CatalogData {

        @JsonProperty("id")
        private Long id;

        @JsonProperty("campaign_id")
        private Long campaignId;

        @JsonProperty("ad_active")
        private Boolean isAdActive;
    }

}
