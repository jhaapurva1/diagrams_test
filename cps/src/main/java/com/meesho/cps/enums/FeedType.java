package com.meesho.cps.enums;

public enum FeedType {
    FY("for_you"),
    CLP("clp"),
    TEXT_SEARCH("text_search"),
    COLLECTION("collection"),
    PRODUCT_RECO("product_reco"),
    TOP_OF_SEARCH("top_of_search"),
    ADS_ON_PDP("ads_on_pdp"),
    UNKNOWN("unknown");

    FeedType(String value){
        this.value = value;
    }

    private String value;

    public String getValue(){
        return value;
    }

    public static FeedType fromValue(String value) {
        for(FeedType feedType : FeedType.values()){
            if(feedType.value.equals(value)){
                return feedType;
            }
        }
        return null;
    }

}
