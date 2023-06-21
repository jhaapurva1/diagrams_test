package com.meesho.cps;

import com.meesho.mq.client.MqClientConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;

@Import(MqClientConfiguration.class)
@SpringBootApplication(exclude = {KafkaAutoConfiguration.class, DataSourceAutoConfiguration.class}, scanBasePackages = {
        "com.meesho"})
@Slf4j
public class CPSApplication {

    public static void main(String[] args) {
        SpringApplication.run(CPSApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext applicationContext) {
        return args -> {
            System.out.println("Inspect beans:");
            String[] beanNames = applicationContext.getBeanDefinitionNames();
            Arrays.sort(beanNames);
//            Arrays.stream(beanNames).forEach(System.out::println);
        };
    }

}
