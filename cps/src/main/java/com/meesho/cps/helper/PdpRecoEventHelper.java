package com.meesho.cps.helper;

import com.meesho.cps.constants.Constants.AdWidgets;
import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.enums.FeedType;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PdpRecoEventHelper implements WidgetEventHelper {

    @Value(AdWidgets.PDP_RECO_CPC_MULTIPLIER)
    private BigDecimal pdpRecoCpcMultiplier;

    @Override
    public String getFeedType() {
        return FeedType.PRODUCT_RECO.getValue();
    }

    @Override
    public String getScreen(AdWidgetClickEvent adWidgetClickEvent) {
        return AdWidgets.SCREEN_PDP_RECO;
    }

    @Override
    public String getOrigin(AdWidgetClickEvent adWidgetClickEvent) {
        return AdWidgets.ORIGIN_PDP_RECO;
    }

    @Override
    public BigDecimal getCpcMultiplier() {
        return pdpRecoCpcMultiplier;
    }
}
