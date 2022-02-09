package com.meesho.cps.data.redshift;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdsDeductionCampaignSupplier {

    private String startDate;
    private Long supplierId;
    private Long campaignId;
    private BigDecimal adsCost;
    private BigDecimal gst;
    private BigDecimal netDeduction;
    private Long credits;
    private String deductionDuration;

}
