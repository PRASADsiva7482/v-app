package com.va.v.v_app.core.connector.db;

import java.util.HashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.va.v.v_app.iam.repository.keycloak", entityManagerFactoryRef = "keycloakEntityManagerFactory", transactionManagerRef = "keycloakTransactionManager")
public class KeycloakDataSourceConfig {

    @Autowired
    @Qualifier("dynamicKeycloakDataSource")
    private DataSource dynamicKeycloakDataSource;

    @Bean(name = "keycloakEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean keycloakEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dynamicKeycloakDataSource);
        em.setPackagesToScan("com.va.v.v_app.iam.model");
        em.setPersistenceUnitName("keycloak");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.put("hibernate.show_sql", false);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean(name = "keycloakTransactionManager")
    public PlatformTransactionManager keycloakTransactionManager(
            @Qualifier("keycloakEntityManagerFactory") LocalContainerEntityManagerFactoryBean keycloakEntityManagerFactory) {
        return new JpaTransactionManager(keycloakEntityManagerFactory.getObject());
    }
}
