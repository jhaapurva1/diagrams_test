package com.meesho.cps.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meesho.ads.lib.constants.Constants;
import com.meesho.ads.lib.data.internal.SchedulerProperty;
import com.meesho.ads.lib.factory.SchedulerFactory;
import com.meesho.ads.lib.scheduler.AbstractScheduler;
import com.meesho.commons.enums.CommonConstants;
import com.meesho.commons.enums.Country;
import com.meesho.cps.constants.SchedulerType;
import com.meesho.cps.decorator.AsyncTaskDecorator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

import static com.meesho.cps.constants.SchedulerType.IN_MEMORY_SCHEDULERS;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Configuration
@Slf4j
public class AppConfig {

    @Autowired
    private ApplicationProperties applicationProperties;

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return objectMapper;
    }

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(customRequestFactory());
        // add multi-tenant headers
        restTemplate.getInterceptors().add(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes,
                                                ClientHttpRequestExecution clientHttpRequestExecution)
                    throws IOException {
                httpRequest.getHeaders().add(CommonConstants.COUNTRY_HEADER, MDC.get(Constants.COUNTRY_CODE));
                return clientHttpRequestExecution.execute(httpRequest, bytes);
            }
        });
        return restTemplate;
    }

    private ClientHttpRequestFactory customRequestFactory() {
        HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory =
                new HttpComponentsClientHttpRequestFactory();
        httpComponentsClientHttpRequestFactory.setConnectTimeout(applicationProperties.getApiDefaultConnectTimeout());
        httpComponentsClientHttpRequestFactory.setReadTimeout(applicationProperties.getApiDefaultReadTimeout());
        return httpComponentsClientHttpRequestFactory;
    }

    @Primary
    @Bean(name = "commonAsyncTaskExecutor")
    public Executor asyncExecutor(final MeterRegistry registry) {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(applicationProperties.getCommonAsyncExecutorCorePoolSize());
        threadPoolTaskExecutor.setMaxPoolSize(applicationProperties.getCommonAsyncExecutorMaxPoolSize());
        threadPoolTaskExecutor.setThreadNamePrefix("common-async-pool-");
        threadPoolTaskExecutor.setTaskDecorator(new AsyncTaskDecorator());
        threadPoolTaskExecutor.initialize();
        return ExecutorServiceMetrics.monitor(registry, threadPoolTaskExecutor, "commonAsyncTaskExecutor");
    }

    public ThreadPoolTaskScheduler getThreadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(IN_MEMORY_SCHEDULERS.size() * Country.values().length);
        threadPoolTaskScheduler.setThreadNamePrefix("async-scheduler-");
        threadPoolTaskScheduler.initialize();
        return threadPoolTaskScheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initSchedulers() {
        MDC.put(Constants.GUID, UUID.randomUUID().toString());
        TaskScheduler taskScheduler = getThreadPoolTaskScheduler();

        for (SchedulerType schedulerType : IN_MEMORY_SCHEDULERS) {
            log.info("Initializing scheduler {}", schedulerType);
            AbstractScheduler scheduler = SchedulerFactory.getByType(schedulerType.name());
            if (Objects.nonNull(scheduler)) {
                for (Country country : Country.values()) {
                    SchedulerProperty schedulerProperty = applicationProperties.getSchedulerTypeCountryAndPropertyMap()
                            .get(schedulerType.name())
                            .get(country.getCountryCode());

                    if (Objects.nonNull(schedulerProperty) && schedulerProperty.getEnableCron()) {
                        taskScheduler.schedule(
                                () -> {
                                    try {
                                        scheduler.run(country.getCountryCode(), schedulerProperty.getCronitorCode(),
                                                schedulerProperty.getBatchSize(), schedulerProperty.getProcessBatchSize());
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                },
                                new CronTrigger(schedulerProperty.getCronExpression())
                        );
                    }
                }
            }
        }
    }

}
