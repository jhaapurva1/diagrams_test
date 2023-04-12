package com.meesho.cps.helper;

import com.meesho.cps.data.entity.kafka.AdWidgetClickEvent;
import com.meesho.cps.data.entity.kafka.AdWidgetViewEvent;
import java.util.Objects;
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
        if (Objects.isNull(adWidgetClickEvent.getProperties())) {
            return widgetEventHelperDummy;
        } else if (Boolean.TRUE.equals(AdWidgetValidationHelper.isTopOfSearchRealEstate(
            adWidgetClickEvent.getProperties().getPrimaryRealEstate()))) {
            return topOfSearchEventHelper;
        } else if (Boolean.TRUE.equals(AdWidgetValidationHelper.isPdpRecoRealEstate(
            adWidgetClickEvent.getProperties().getPrimaryRealEstate()))) {
            return pdpRecoEventHelper;
        }
        return widgetEventHelperDummy;
    }

    public WidgetEventHelper getWidgetEventHelperInstance(AdWidgetViewEvent adWidgetViewEvent) {
        if (Objects.isNull(adWidgetViewEvent.getProperties()) || Objects.isNull(
            adWidgetViewEvent.getProperties().getPrimaryRealEstates())
            || adWidgetViewEvent.getProperties().getPrimaryRealEstates().isEmpty()) {
            return widgetEventHelperDummy;
        } else if (Boolean.TRUE.equals(AdWidgetValidationHelper.isTopOfSearchRealEstate(
            adWidgetViewEvent.getProperties().getPrimaryRealEstates().get(0)))) {
            return topOfSearchEventHelper;
        } else if (Boolean.TRUE.equals(AdWidgetValidationHelper.isPdpRecoRealEstate(
            adWidgetViewEvent.getProperties().getPrimaryRealEstates().get(0)))) {
            return pdpRecoEventHelper;
        }
        return widgetEventHelperDummy;
    }
}
