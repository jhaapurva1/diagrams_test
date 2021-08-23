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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@Entity
@Table(name = DBConstants.Tables.REAL_ESTATE_METADATA)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RealEstateMetadata extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "click_multiplier")
    private BigDecimal clickMultiplier;

}
