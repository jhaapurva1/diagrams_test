package com.meesho.cps.data.presto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.prism.proxy.annotations.DataLakeColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdsDeductionCampaignSupplierPrestoData {

    @DataLakeColumn(name = "campaign_id")
    private Long campaignId;

    @DataLakeColumn(name = "supplier_id")
    private Long supplierId;

    @DataLakeColumn(name = "deduction_duration")
    private String deductionDuration;

    @DataLakeColumn(name = "start_date")
    private String startDate;

    @DataLakeColumn(name = "gst")
    private Double gst;

    @DataLakeColumn(name = "net_deduction")
    private Double netDeduction;

    @DataLakeColumn(name = "credits")
    private Double credits;

    @DataLakeColumn(name = "ads_cost")
    private Double adsCost;

    @DataLakeColumn(name = "transaction_id")
    private String transactionId;

    @DataLakeColumn(name = "created_at")
    private Timestamp created_at;
}
