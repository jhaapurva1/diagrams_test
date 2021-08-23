package com.meesho.cps.config.external;

import com.meesho.baseclient.pojos.ServiceRestConfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class ClientConfig {

    private String host;
    private Integer port;
    private String secret;
    private HttpConfig httpConfig;

    @Override
    public String toString() {
        return "ClientConfig{" + "host='" + host + '\'' + ", port='" + port + '\'' + ", secret='" + secret + '\'' +
                ", http=" + httpConfig + '}';
    }

    public ServiceRestConfig getRestConfig() {
        return ServiceRestConfig.builder().host(getHost()).port(getPort()).authToken(getSecret()).build();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @SuperBuilder
    public static class HttpConfig {
        private int connectTimeout;
        private int socketTimeout;
        private int connectionRequestTimeout;
        private int maxTotal;
        private int maxPerRoute;

        @Override
        public String toString() {
            return "HttpConfig{" + "connectTimeout=" + connectTimeout + ", socketTimeout=" + socketTimeout +
                    ", connectionRequestTimeout=" + connectionRequestTimeout + ", maxTotal=" + maxTotal +
                    ", maxPerRoute=" + maxPerRoute + '}';
        }
    }

}
