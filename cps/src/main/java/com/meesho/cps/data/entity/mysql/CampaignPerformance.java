package com.meesho.cps.data.entity.mysql;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.meesho.ads.lib.data.mysql.BaseEntity;
import com.meesho.cps.constants.DBConstants;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
@Data
@Entity
@Table(name = DBConstants.Tables.CAMPAIGN_PERFORMANCE)
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignPerformance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "catalog_id")
    private Long catalogId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "budget_utilised")
    private BigDecimal budgetUtilised;

    @Column(name = "total_views")
    private Long totalViews;

    @Column(name = "total_clicks")
    private Long totalClicks;

    @Column(name = "revenue")
    private BigDecimal revenue;

    @Column(name = "order_count")
    private Integer orderCount;

}
