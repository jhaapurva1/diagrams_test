package com.meesho.cps.helper;

import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.AdViewEvent;

import java.util.Objects;

/**
 * @author shubham.aggarwal
 * 02/08/21
 */
public class ValidationHelper {

    public static Boolean isValidAdInteractionEvent(AdInteractionEvent adInteractionEvent) {
        return Objects.nonNull(adInteractionEvent.getUserId()) && Objects.nonNull(adInteractionEvent.getProperties()) &&
                Objects.nonNull(adInteractionEvent.getProperties().getId()) &&
                Objects.nonNull(adInteractionEvent.getEventTimestamp()) &&
                Objects.nonNull(adInteractionEvent.getProperties().getType());
    }

    public static Boolean isValidAdViewEvent(AdViewEvent adViewEvent) {
        return Objects.nonNull(adViewEvent.getUserId()) && Objects.nonNull(adViewEvent.getProperties()) &&
                Objects.nonNull(adViewEvent.getProperties().getId());
    }

}
