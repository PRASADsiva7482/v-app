package com.va.v.v_app.config.connector.db;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.log4j.Log4j2;

@Configuration
@Log4j2
public class DatasourceMap {

	@Autowired
	DataSourceConfiguration dataSourceConfiguration;

	@Bean(name = "crmdataSourceMap")
	public Map<String, HikariDataSource> dataCRMSourceMap() {
		Map<String, HikariDataSource> dataSourceMap = new HashMap<>();
		dataSourceConfiguration.getCrmdatasource()
				.forEach(dbProp -> {
					dataSourceMap.put(dbProp.getName(), createDataSource(dbProp));
					log.info("Created CRM datasource: {} with pool: {}", dbProp.getName(), dbProp.getPoolName());
				});
		log.info("Total CRM datasources initialized: {}", dataSourceMap.size());
		return dataSourceMap;
	}

	@Bean(name = "billingdataSourceMap")
	public Map<String, HikariDataSource> dataBillingSourceMap() {
		Map<String, HikariDataSource> dataSourceMap = new HashMap<>();
		dataSourceConfiguration.getBillingdatasource()
				.forEach(dbProp -> {
					dataSourceMap.put(dbProp.getName(), createDataSource(dbProp));
					log.info("Created Billing datasource: {} with pool: {}", dbProp.getName(), dbProp.getPoolName());
				});
		log.info("Total Billing datasources initialized: {}", dataSourceMap.size());
		return dataSourceMap;
	}

	private HikariDataSource createDataSource(DatasourceProperties connectionProperties) {

		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setDriverClassName(connectionProperties.getDriverClassName());
		dataSource.setJdbcUrl(connectionProperties.getJdbcUrl());
		dataSource.setUsername(connectionProperties.getUsername());
		dataSource.setPassword(connectionProperties.getPassword());
		dataSource.setPoolName(connectionProperties.getPoolName());
		dataSource.setMaximumPoolSize(connectionProperties.getMaximumPoolSize());
		dataSource.setMinimumIdle(connectionProperties.getMinimumIdle());

		return dataSource;
	}
}
