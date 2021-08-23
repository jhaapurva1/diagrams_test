package com.meesho.cps.config;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
@Slf4j
public class OutBoundInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String url = request.getURI().toString();
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("[HTTP Success] URL: {} Status:{}", url, response.getRawStatusCode());
        } else {
            log.error("[HTTP Error] URL: {} Status: {} Body: {}", url, response.getRawStatusCode(), new String(body));
        }
        return response;
    }

}
