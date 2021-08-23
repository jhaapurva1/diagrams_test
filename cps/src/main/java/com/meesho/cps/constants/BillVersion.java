package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public enum BillVersion {

    CHARGE_PER_CLICK(1), CHARGE_PER_INTERACTION(2);

    private final int value;

    BillVersion(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return value;
    }
}
