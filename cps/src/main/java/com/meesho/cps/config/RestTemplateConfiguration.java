package com.meesho.cps.config;

import com.meesho.cps.config.external.AdServiceClientConfig;
import com.meesho.cps.config.external.ClientConfig;
import com.meesho.cps.config.external.PrismServiceClientConfig;
import com.meesho.cps.constants.BeanNames;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
@Configuration
public class RestTemplateConfiguration {

    @Autowired
    AdServiceClientConfig adServiceClientConfig;

    @Autowired
    PrismServiceClientConfig prismClientConfig;

    private static RestTemplate getRestTemplate(ClientConfig clientConfig) {
        HttpClient httpClient = getHttpClientFactory(clientConfig.getHttpConfig());
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(httpClient);
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory);
        restTemplate.setInterceptors(Collections.singletonList(new OutBoundInterceptor()));
        return restTemplate;
    }

    public static HttpClient getHttpClientFactory(ClientConfig.HttpConfig httpPoolConfig) {
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager pooledConnectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        pooledConnectionManager.setDefaultMaxPerRoute(httpPoolConfig.getMaxPerRoute());
        pooledConnectionManager.setMaxTotal(httpPoolConfig.getMaxTotal());

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(httpPoolConfig.getConnectTimeout())
                .setConnectionRequestTimeout(httpPoolConfig.getConnectionRequestTimeout())
                .setSocketTimeout(httpPoolConfig.getSocketTimeout())
                .build();

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(pooledConnectionManager)
                .build();
    }

    @Bean(BeanNames.RestClients.AD_SERVICE)
    public RestTemplate getRestClientForABService() {
        return getRestTemplate(adServiceClientConfig);
    }

    @Bean(BeanNames.RestClients.PRISM_SERVICE)
    public RestTemplate getRestClientForPrismService() {
        return getRestTemplate(prismClientConfig);
    }

}
