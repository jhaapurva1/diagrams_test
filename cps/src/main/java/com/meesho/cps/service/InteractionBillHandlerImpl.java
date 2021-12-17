package com.meesho.cps.service;

import com.meesho.cps.constants.BillVersion;
import com.meesho.cps.constants.ConsumerConstants;
import com.meesho.cps.data.entity.hbase.CampaignCatalogDateMetrics;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Service
public class InteractionBillHandlerImpl implements BillHandler {

    private final Set<String> VALID_EVENTS = new HashSet<>(
            Arrays.asList(ConsumerConstants.IngestionInteractionEvents.AD_CLICK_TOPIC,
                    ConsumerConstants.IngestionInteractionEvents.AD_SHARED_TOPIC,
                    ConsumerConstants.IngestionInteractionEvents.AD_WISHLISTED_TOPIC,
                    ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_CLICK_TOPIC,
                    ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_SHARED_TOPIC,
                    ConsumerConstants.IngestionInteractionEvents.ANONYMOUS_AD_WISHLISTED_TOPIC));

    @Override
    public Set<String> getValidEvents() {
        return VALID_EVENTS;
    }

    @Override
    public Long getTotalInteractions(CampaignCatalogDateMetrics baseCampaignMetrics) {
        return baseCampaignMetrics.getClickCount() + baseCampaignMetrics.getSharesCount() +
                baseCampaignMetrics.getWishlistCount();
    }

    @Override
    public boolean performWindowDeDuplication() {
        return true;
    }

    @Override
    public BillVersion getBillVersion() {
        return BillVersion.CHARGE_PER_INTERACTION;
    }

}
