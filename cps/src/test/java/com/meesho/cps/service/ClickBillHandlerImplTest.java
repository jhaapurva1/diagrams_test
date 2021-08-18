package com.meesho.cps.service;

import com.meesho.cps.data.entity.hbase.CampaignCatalogMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;

/**
 * @author shubham.aggarwal
 * 17/08/21
 */
@RunWith(MockitoJUnitRunner.class)
public class ClickBillHandlerImplTest {

    @Test
    public void getTotalImpressions() {
        CampaignCatalogMetrics ccm1 = new CampaignCatalogMetrics();
        CampaignCatalogMetrics ccm2 = new CampaignCatalogMetrics();

        ccm1.setWeightedClickCount(new BigDecimal(2));
        ccm1.setWeightedWishlistCount(new BigDecimal(3));
        ccm1.setWeightedSharesCount(new BigDecimal(4));

        ClickBillHandlerImpl clickBillHandler = new ClickBillHandlerImpl();
        BigDecimal value = clickBillHandler.getTotalInteractions(ccm1);

        Assert.assertEquals(new BigDecimal(2).stripTrailingZeros(), value.stripTrailingZeros());

    }

}
