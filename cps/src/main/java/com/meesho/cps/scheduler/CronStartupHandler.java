package com.meesho.cps.scheduler;
import com.meesho.ads.lib.data.internal.SchedulerProperty;
import com.meesho.ads.lib.factory.SchedulerFactory;
import com.meesho.ads.lib.scheduler.AbstractScheduler;
import com.meesho.commons.enums.Country;
import com.meesho.cps.config.ApplicationProperties;
import com.meesho.cps.constants.SchedulerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class CronStartupHandler implements CommandLineRunner,ApplicationContextAware {
    private final ApplicationProperties applicationProperties;

    private  ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext=applicationContext;
    }

    @Override
    public void run(String... args) {
        if (args == null || args.length == 0) {
            log.info("Scheduler cfg not defined");
            return;
        }

        String job = args[0];
        log.info(args[0]);
        log.info(SchedulerType.getInstance(job).name());
        Map<String, SchedulerProperty> countryConfigMap=applicationProperties.getSchedulerTypeCountryAndPropertyMap().get(SchedulerType.getInstance(job).name());
        if (Objects.isNull(countryConfigMap)) {
            log.info("Scheduler countryConfigMap is not valid ,{}, not initiating any schedulers", countryConfigMap);
        } else {
            for (Map.Entry<String, SchedulerProperty> countryConfig : countryConfigMap.entrySet()) {
                Instant start = Instant.now();
                SchedulerType schedulerType = null;

                try {
                    String countryCode = countryConfig.getKey();
                    Country country = Country.valueOf(countryCode.toUpperCase());
                    schedulerType = SchedulerType.getInstance(job);
                    log.info("Starting cron {} at time {}", schedulerType, start);
                    getCronCommand(schedulerType,country).run();
                } catch (Exception e) {
                    log.error("Encountered exception while scheduling cron for: {}, error : {}",schedulerType, ExceptionUtils.getStackTrace(e));
                }

                log.info("Shutting down scheduler");
                log.info("Time take by job {} is {} minutes", schedulerType, Duration.between(start, Instant.now()).toMinutes());
            }
        }
        try {
            Thread.sleep(applicationProperties.getCronAppTerminationDelayMilliseconds());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }

    private CronRunner getCronCommand(SchedulerType schedulerType, Country country) {

        if (SchedulerType.UNKNOWN.equals(schedulerType)) {
            log.error("Encountered unknown cron schedulerType : {}", schedulerType);
            throw new UnsupportedOperationException("Encountered unknown cron schedulerType : " + schedulerType);
        }
        return () -> {
                schedulerProcessMethod(schedulerType,country);
        };
    }

    private void schedulerProcessMethod(SchedulerType schedulerType,Country country) {
        log.info("Initializing scheduler {}", schedulerType);
        AbstractScheduler scheduler = SchedulerFactory.getByType(schedulerType.name());
        if (Objects.nonNull(scheduler)) {
                SchedulerProperty schedulerProperty =
                        applicationProperties.getSchedulerTypeCountryAndPropertyMap()
                                .get(schedulerType.name())
                                .get(country.getCountryCode());

                if (Objects.nonNull(schedulerProperty)&&schedulerProperty.getEnableCron()) {
                                try {
                                    scheduler.run(
                                            country.getCountryCode(),
                                            schedulerProperty.getCronitorCode(),
                                            schedulerProperty.getBatchSize(),
                                            schedulerProperty.getProcessBatchSize()
                                    );
                                } catch (Exception e) {
                                    log.error("Exception encountered while running the scheduler {}", schedulerType, e);
                                    throw new RuntimeException(e);
                                }
                }
        }
    }
}