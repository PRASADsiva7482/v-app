package com.va.v.v_app.config.webConfig;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "feature")
@Getter
@Setter
public class ApplicationCacheConfiguration {

    private Map<String, FeatureMapping> mappings = new HashMap<>();

    @Getter
    @Setter
    public static class FeatureMapping {
        private Map<String, String> dynamicDetails = new HashMap<>();
    }
}
