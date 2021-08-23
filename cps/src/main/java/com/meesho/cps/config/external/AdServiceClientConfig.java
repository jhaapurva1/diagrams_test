package com.meesho.cps.config.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 09/08/21
 */
@Data
@Configuration
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "rest-client.ad-service")
@Slf4j
public class AdServiceClientConfig extends ClientConfig {

    @PostConstruct
    public void init() {
        log.info(super.toString());
    }

}
