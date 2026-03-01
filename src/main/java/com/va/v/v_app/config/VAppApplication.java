package com.va.v.v_app.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

import lombok.extern.slf4j.Slf4j;

@EnableDiscoveryClient
@EnableAsync
@EnableCaching
@Configuration
@Slf4j
@EnableEncryptableProperties
@EnableFeignClients(basePackages = { "com.va.v.v_app" })
@ComponentScan(basePackages = { "com.va.v.v_app" })
@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling // Enable background jobs for For You feed
public class VAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(VAppApplication.class, args);
	}

}
