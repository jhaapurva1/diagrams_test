package com.meesho.cps.config.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Configuration
@EnableConfigurationProperties
@PropertySource("classpath:application.properties")
@ConfigurationProperties(prefix = "rest-client.prism-service")
public class PrismServiceClientConfig extends ClientConfig {

    private String username;

    @Override
    public String toString() {
        return "PrismServiceClientConfig{" + "username='" + username + '\'' + ", clientConfig=" + super.toString() +
                '}';
    }

    @PostConstruct
    public void init() {
        log.info(this.toString());
    }

}
