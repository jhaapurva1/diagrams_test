package com.meesho.cps.config.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Properties;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"com.meesho.cps.db.mysql.repository", "com.meesho.ads.lib.db.mysql.repository"})
public class DBConfig {

    @Autowired
    private Environment env;

    /**
     * Master Database connection config
     */

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource(masterDataSourceProperties());
        return dataSource;
    }

    private HikariConfig masterDataSourceProperties() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(env.getProperty("spring.datasource.driverClassName"));
        config.setJdbcUrl(env.getProperty("spring.datasource.url"));
        config.setUsername(env.getProperty("spring.datasource.username"));
        config.setPassword(env.getProperty("spring.datasource.password"));
        config.setInitializationFailTimeout(env.getProperty("spring.datasource.initializationFailTimeout", Long.class));
        config.setMaximumPoolSize(env.getProperty("spring.datasource.maximumPoolSize", Integer.class));
        config.setMinimumIdle(env.getProperty("spring.datasource.minIdle", Integer.class));
        config.setConnectionTestQuery(env.getProperty("spring.datasource.validationQuery"));
        config.setIdleTimeout(env.getProperty("spring.datasource.idleTimeout", Long.class));
        config.setMaxLifetime(env.getProperty("spring.datasource.maxLifetime", Long.class));
        config.setConnectionTimeout(env.getProperty("spring.datasource.connectionTimeout", Long.class));
        config.setPoolName("adserver");
        return config;
    }


    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource());
        entityManagerFactory.setPackagesToScan("com.meesho.cps.data.entity.mysql", "com.meesho.ads.lib.data.mysql");
        entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        // Hibernate properties
        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.dialect", env.getProperty("hibernate.dialect"));
        jpaProperties.put("hibernate.show_sql", env.getProperty("hibernate.show_sql"));
        entityManagerFactory.setJpaProperties(jpaProperties);
        return entityManagerFactory;
    }


    @Bean
    @Primary
    public PlatformTransactionManager transactionManager() {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory().getObject());
        //transactionManager.setNestedTransactionAllowed(true);
        return transactionManager;
    }

}
