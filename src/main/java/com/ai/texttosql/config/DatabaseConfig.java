package com.ai.texttosql.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableJpaAuditing
public class DatabaseConfig {

    @Bean
    public DataSource dataSource(@Value("${spring.datasource.driver-class-name}") String driverClassName,
                                 @Value("${spring.datasource.url}") String url,
                                 @Value("${spring.datasource.username}") String username,
                                 @Value("${spring.datasource.password}") String password) {
        return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(url)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                       @Value("${spring.jpa.hibernate.ddl-auto}") String ddlAuto,
                                                                       @Value("${spring.jpa.properties.hibernate.dialect}") String dialect,
                                                                       @Value("${spring.jpa.show-sql}") String showSql,
                                                                       @Value("${spring.jpa.properties.hibernate.format_sql}") String formatSql,
                                                                       @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}") String batchSize,
                                                                       @Value("${spring.jpa.properties.hibernate.jdbc.order_inserts}") String orderInserts,
                                                                       @Value("${spring.jpa.properties.hibernate.jdbc.order_updates}") String orderUpdates,
                                                                       @Value("${spring.jpa.hibernate.physical_naming_strategy}") String namingStrategy) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.ai.texttosql");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setJpaProperties(hibernateProperties(ddlAuto, dialect, showSql, formatSql, batchSize, orderInserts, orderUpdates, namingStrategy));
        return em;
    }

    private Properties hibernateProperties(String ddlAuto, String dialect, String showSql,
                                           String formatSql, String batchSize, String orderInserts,
                                           String orderUpdates, String namingStrategy) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", ddlAuto);
        properties.setProperty("hibernate.dialect", dialect);
        properties.setProperty("hibernate.show_sql", showSql);
        properties.setProperty("hibernate.format_sql", formatSql);
        properties.setProperty("hibernate.jdbc.batch_size", batchSize);
        properties.setProperty("hibernate.order_inserts", orderInserts);
        properties.setProperty("hibernate.order_updates", orderUpdates);
        properties.setProperty("hibernate.physical_naming_strategy", namingStrategy);
        return properties;
    }

    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
}