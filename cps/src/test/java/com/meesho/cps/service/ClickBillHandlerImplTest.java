package com.meesho.cps.service;

import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author shubham.aggarwal
 * 17/08/21
 */
@RunWith(MockitoJUnitRunner.class)
public class ClickBillHandlerImplTest {

    @Test
    public void getTotalImpressions() {
        CampaignCatalogDateMetrics ccm1 = new CampaignCatalogDateMetrics();
        CampaignCatalogDateMetrics ccm2 = new CampaignCatalogDateMetrics();

        ccm1.setClickCount(2L);
        ccm1.setWishlistCount(3L);
        ccm1.setSharesCount(4L);

        ClickBillHandlerImpl clickBillHandler = new ClickBillHandlerImpl();
        Long value = clickBillHandler.getTotalInteractions(ccm1);

        Assert.assertEquals(Long.valueOf(2), value);

    }

}
