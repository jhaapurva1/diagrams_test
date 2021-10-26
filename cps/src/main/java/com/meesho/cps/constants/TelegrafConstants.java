package com.meesho.cps.constants;

/**
 * @author shubham.aggarwal
 * 12/10/21
 */
public class TelegrafConstants {

    public static final String SERVICE_NAME = "campaignPerformance";

    public static final String INTERACTION_EVENT_KEY = SERVICE_NAME + "InteractionEvent";
    public static final String VIEW_EVENT_KEY = SERVICE_NAME + "ViewEvent";
    public static final String INTERACTION_EVENT_CPC_KEY = SERVICE_NAME + "InteractionEventCPC";

    // tags placeholder
    public static final String INTERACTION_EVENT_TAGS = "eventName=%s,screen=%s,status=%s,reason=%s";
    public static final String VIEW_EVENT_TAGS = "eventName=%s,screen=%s,status=%s,reason=%s";
    public static final String INTERACTION_EVENT_CPC_TAGS = "eventName=%s,screen=%s";

    // tag values
    public static final String NAN = "NAN";
    public static final String VALID = "valid";
    public static final String INVALID = "invalid";

}
