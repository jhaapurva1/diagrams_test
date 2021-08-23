package com.meesho.cps.service.external;

import com.meesho.ads.lib.constants.Constants;
import com.meesho.commons.enums.CommonConstants;
import com.meesho.cps.config.external.PrismServiceClientConfig;
import com.meesho.cps.data.request.PrismEventRequest;
import com.meesho.instrumentation.annotation.DigestLogger;
import com.meesho.instrumentation.enums.MetricType;

import org.apache.hadoop.hbase.shaded.org.apache.commons.codec.binary.Base64;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Service
@Slf4j
@DigestLogger(metricType = MetricType.METHOD, tagSet = "PrismService")
public class PrismService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    PrismServiceClientConfig prismServiceClientConfig;

    public <T> void publishEvent(String eventName, List<T> eventProperties) {
        try {
            List<PrismEventRequest<T>> requestBody = PrismEventRequest.of(eventName, eventProperties);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(CommonConstants.COUNTRY_HEADER, MDC.get(Constants.COUNTRY_CODE));
            String authValue = "Basic " + Base64.encodeBase64String(
                    (prismServiceClientConfig.getUsername() + ":" + prismServiceClientConfig.getSecret()).getBytes());
            headers.set(HttpHeaders.AUTHORIZATION, authValue);
            HttpEntity<List<PrismEventRequest<T>>> request = new HttpEntity<>(requestBody, headers);
            restTemplate.postForLocation(prismServiceClientConfig.getHost() +
                    com.meesho.cps.constants.Constants.API.PrismService.EVENT_PUBLISH + eventName, request);
        } catch (Exception e) {
            log.error("Exception in publishEvent", e);
        }
    }

}
