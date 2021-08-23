package com.meesho.cps.data.request;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class PrismEventRequest<T> {
    private String event;
    private T properties;

    public static <T> List<PrismEventRequest<T>> of(String eventName, List<T> eventProperties) {
        return eventProperties.stream()
                .map(eventProperty -> PrismEventRequest.<T>builder().event(eventName).properties(eventProperty).build())
                .collect(Collectors.toList());
    }

}
