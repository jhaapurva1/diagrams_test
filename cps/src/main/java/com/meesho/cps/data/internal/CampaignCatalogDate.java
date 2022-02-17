package com.meesho.cps.data.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author shubham.aggarwal
 * 13/12/21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CampaignCatalogDate {
    private Long campaignId;
    private Long catalogId;
    private String date;
}
