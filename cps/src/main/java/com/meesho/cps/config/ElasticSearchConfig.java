package com.meesho.cps.config;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

@Configuration
public class ElasticSearchConfig {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.port}")
    private Integer port;

    @Value("${elasticsearch.host.scheme}")
    private String httpScheme;

    @Value("${elasticsearch.connect.timeout.ms}")
    private Integer connectTimeoutMs;

    @Value("${elasticsearch.socket.timeout.ms}")
    private Integer socketTimeoutMs;

    @Value("${elasticsearch.ioreactor.connections.count}")
    private Integer connections;

    @Value("${elasticsearch.http-pool.max-per-route}")
    private Integer maxConnPerRoute;

    @Value("${elasticsearch.http-pool.max-total}")
    private Integer maxConnTotal;

    @Bean("mainCluster")
    public RestHighLevelClient client() {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, httpScheme)
                ).setRequestConfigCallback(
                        new RestClientBuilder.RequestConfigCallback() {
                            @Override
                            public RequestConfig.Builder customizeRequestConfig(RequestConfig.Builder builder) {
                                return builder
                                        .setConnectTimeout(connectTimeoutMs)
                                        .setConnectionRequestTimeout(connectTimeoutMs)
                                        .setSocketTimeout(socketTimeoutMs);
                            }
                        }).setHttpClientConfigCallback(
                        new RestClientBuilder.HttpClientConfigCallback() {
                            @Override
                            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                                return httpAsyncClientBuilder
                                        .setDefaultIOReactorConfig(
                                                IOReactorConfig.custom()
                                                        .setIoThreadCount(connections)
                                                        .build()
                                        )
                                        .setMaxConnTotal(maxConnTotal)
                                        .setMaxConnPerRoute(maxConnPerRoute);

                            }
                        })
        );
    }
}