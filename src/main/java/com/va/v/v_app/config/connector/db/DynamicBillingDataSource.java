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
public class DynamicBillingDataSource extends AbstractRoutingDataSource {

    @Autowired
    @Qualifier("billingdataSourceMap")
    Map<String, HikariDataSource> billingdataSourceMap;

    @Override
    protected Object determineCurrentLookupKey() {
        return MDC.get(Constants.bill_db_instance);
    }

    @Override
    public void afterPropertiesSet() {
        Map<Object, Object> targetDataSources = new HashMap<>(billingdataSourceMap);
        setTargetDataSources(targetDataSources);
        setDefaultTargetDataSource(
                Objects.requireNonNull(billingdataSourceMap.get("initial"),
                        "initial Billing database is not configured"));
        super.afterPropertiesSet();
    }

}
