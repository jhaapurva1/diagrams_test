package com.meesho.cps.enums;

public enum FeedType {
    FY("for_you"),
    CLP("clp"),
    TEXT_SEARCH("text_search"),
    COLLECTION("collection"),
    PRODUCT_RECO("product_reco");

    FeedType(String value){
        this.value = value;
    }

    private String value;

    public String getValue(){
        return value;
    }

}
