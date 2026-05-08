package com.va.v.v_app.core.connector.db;

import java.io.Serializable;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "datasource")
@Getter
@Setter
public class DataSourceConfiguration implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<DatasourceProperties> crmdatasource;
	private List<DatasourceProperties> keycloakdatasource;
}