package com.va.v.v_app.core.connector.db;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Component
public class DatasourceProperties {

	private String name;
	private String jdbcUrl;
	private String driverClassName;
	private String username;
	private String password;
	private String poolName;
	private Integer maximumPoolSize;
	private Integer minimumIdle;
}
