package com.meesho.cps.constants;

public enum PdpWidgetPosition {
    PRODUCT_DETAILS("product_details", 5), AFTER_PRODUCT_DETAILS("after_product_details",
        6), PRODUCT_RATING_REVIEW("product_rating_review", 7), AFTER_PRODUCT_RATING_REVIEW(
        "after_product_rating_review", 8), DUPLICATE_PRODUCT("duplicate_product",
        10), AFTER_DUPLICATE_PRODUCT("after_duplicate_product", 11);

    private final String positionName;
    private final Integer positionNumber;


    PdpWidgetPosition(String positionName, int positionNumber) {
        this.positionName = positionName;
        this.positionNumber = positionNumber;
    }

    public String positionName() {
        return positionName;
    }

    public Integer positionNumber() {
        return positionNumber;
    }

    public static PdpWidgetPosition fromPositionNumber(Integer positionNumber) {
        for (PdpWidgetPosition pdpWidgetPositions : PdpWidgetPosition.values()) {
            if (pdpWidgetPositions.positionNumber().equals(positionNumber)) {
                return pdpWidgetPositions;
            }
        }
        return null;
    }
}
