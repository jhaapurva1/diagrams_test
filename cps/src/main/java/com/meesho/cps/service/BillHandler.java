package com.meesho.cps.service;

import com.meesho.cps.constants.BillVersion;
import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;

import java.math.BigDecimal;
import java.util.Set;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public interface BillHandler {

    Set<String> getValidEvents();

    BigDecimal getTotalInteractions(CampaignCatalogMetrics baseCampaignMetrics);

    boolean performWindowDeDuplication();

    BillVersion getBillVersion();

}
