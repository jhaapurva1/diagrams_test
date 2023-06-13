package com.meesho.cps.service;

import com.meesho.cps.data.entity.mongodb.collection.CampaignCatalogDateMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author shubham.aggarwal
 * 17/08/21
 */
@RunWith(MockitoJUnitRunner.class)
public class InteractionBillHandlerImplTest {

    @Test
    public void getTotalImpressions() {
        CampaignCatalogDateMetrics document = new CampaignCatalogDateMetrics();

        document.setClicks(2L);
        document.setWishlists(3L);
        document.setShares(4L);

        InteractionBillHandlerImpl interactionBillHandler = new InteractionBillHandlerImpl();
        Long value = interactionBillHandler.getTotalInteractions(document);

        Assert.assertEquals(Long.valueOf(9), value);

    }

}
