package com.meesho.cps.helper;

import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WidgetEventHelperInstanceSelector {

    @Autowired
    private TopOfSearchEventHelper topOfSearchEventHelper;
    @Autowired
    private PdpRecoEventHelper pdpRecoEventHelper;
    @Autowired
    private WidgetEventHelperDummy widgetEventHelperDummy;
    public WidgetEventHelper getWidgetEventHelperInstance(AdWidgetClickEvent adWidgetClickEvent) {
        if (Boolean.TRUE.equals(AdWidgetValidationHelper.isTopOfSearchRealEstate(
            adWidgetClickEvent.getProperties().getPrimaryRealEstate()))) {
            return topOfSearchEventHelper;
        } else if (Boolean.TRUE.equals(AdWidgetValidationHelper.isPdpRecoRealEstate(
            adWidgetClickEvent.getProperties().getPrimaryRealEstate()))) {
            return pdpRecoEventHelper;
        }
        return widgetEventHelperDummy;
    }
}
