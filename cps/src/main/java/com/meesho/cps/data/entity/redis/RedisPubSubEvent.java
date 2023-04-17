package com.meesho.cps.data.entity.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.meesho.cps.constants.MessageIntent;
import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class RedisPubSubEvent implements Serializable {

    private MessageIntent messageIntent;

    private String recordsAsString;

}
