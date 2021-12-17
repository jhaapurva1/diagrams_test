package com.meesho.cps.data.redshift;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author shubham.aggarwal
 * 23/11/21
 */
@Data
@Builder
public class CampaignPerformanceRedshift {

    private Long campaignId;
    private Long catalogId;
    private String date;
    private Integer orderCount;
    private BigDecimal revenue;

}
