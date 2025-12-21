package com.va.v.v_app.config.connector.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

import com.va.v.v_app.config.utils.Constants;

@Component
public class DynamicKeycloakDataSource extends AbstractRoutingDataSource {

    @Autowired
    @Qualifier("keycloakdataSourceMap")
    Map<String, HikariDataSource> keycloakdataSourceMap;

    @Override
    protected Object determineCurrentLookupKey() {
        return MDC.get(Constants.keycloak_db_instance);
    }

    @Override
    public void afterPropertiesSet() {
        Map<Object, Object> targetDataSources = new HashMap<>(keycloakdataSourceMap);
        setTargetDataSources(targetDataSources);
        setDefaultTargetDataSource(
                Objects.requireNonNull(keycloakdataSourceMap.get("initial"),
                        "initial Keycloak database is not configured"));
        super.afterPropertiesSet();
    }

}
