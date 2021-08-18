package com.meesho.cps.transformer;

import com.meesho.ads.lib.utils.Utils;
import com.meesho.commons.utils.DateUtils;
import com.meesho.cps.data.entity.kafka.AdInteractionEvent;
import com.meesho.cps.data.entity.kafka.AdInteractionPrismEvent;

import java.time.ZonedDateTime;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public class PrismEventTransformer {

    public static AdInteractionPrismEvent getAdInteractionPrismEvent(AdInteractionEvent adInteractionEvent,
                                                                     String userId, Long catalogId) {
        return AdInteractionPrismEvent.builder()
                .eventId(adInteractionEvent.getEventId())
                .eventName(adInteractionEvent.getEventName())
                .catalogId(catalogId)
                .userId(userId)
                .interactionType(adInteractionEvent.getProperties().getType())
                .eventTimestamp(adInteractionEvent.getEventTimestamp())
                .eventTimeIso(adInteractionEvent.getEventTimeIso())
                .appVersionCode(adInteractionEvent.getProperties().getAppVersionCode())
                .origin(adInteractionEvent.getProperties().getOrigin())
                .screen(adInteractionEvent.getProperties().getScreen())
                .currentTimestamp(DateUtils.toIsoString(ZonedDateTime.now(), Utils.getCountry()))
                .build();
    }

}
