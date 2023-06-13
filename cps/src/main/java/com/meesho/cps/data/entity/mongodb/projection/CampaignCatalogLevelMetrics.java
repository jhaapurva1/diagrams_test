package com.meesho.cps.data.entity.mongodb.projection;

import com.meesho.cps.data.internal.BasePerformanceMetrics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class CampaignCatalogLevelMetrics extends BasePerformanceMetrics {

    @Id
    private CampaignIdCatalogId campaignIdCatalogId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignIdCatalogId {

        private Long campaignId;

        private Long catalogId;

    }

}
