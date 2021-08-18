package com.meesho.cps.factory;

import com.meesho.cps.service.BillHandler;
import com.meesho.cps.service.ClickBillHandlerImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Component
public class AdBillFactory {

    private static Map<Integer, BillHandler> billHandlersMap;
    @Autowired
    private List<BillHandler> billHandlers;
    @Autowired
    private ClickBillHandlerImpl clickBillHandler;

    @PostConstruct
    public void init() {
        billHandlersMap = new HashMap<>();
        for (BillHandler billHandler : billHandlers) {
            billHandlersMap.put(billHandler.getBillVersion().getValue(), billHandler);
        }
    }

    public BillHandler getBillHandlerForBillVersion(Integer billVersion) {
        if (Objects.isNull(billVersion) || !billHandlersMap.containsKey(billVersion))
            return clickBillHandler;
        return billHandlersMap.get(billVersion);
    }

}
