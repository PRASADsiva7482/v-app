package com.va.v.v_app.core.connector.db;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.zaxxer.hikari.HikariDataSource;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
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

	@Bean(name = "keycloakdataSourceMap")
	public Map<String, HikariDataSource> dataKeycloakSourceMap() {
		Map<String, HikariDataSource> dataSourceMap = new HashMap<>();
		dataSourceConfiguration.getKeycloakdatasource()
				.forEach(dbProp -> {
					dataSourceMap.put(dbProp.getName(), createDataSource(dbProp));
					log.info("Created Keycloak datasource: {} with pool: {}", dbProp.getName(), dbProp.getPoolName());
				});
		log.info("Total Keycloak datasources initialized: {}", dataSourceMap.size());
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
