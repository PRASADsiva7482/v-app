package com.va.v.v_app.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads environment variables from .env file into Spring context
 * This allows Spring Boot to read ${...} placeholders from .env
 */
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // Load .env file from project root
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // Don't fail if .env is missing
                    .load();

            // Convert .env entries to a property source
            Map<String, Object> envMap = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                envMap.put(entry.getKey(), entry.getValue());
            });

            // Add to Spring's environment
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", envMap));

            System.out.println("✅ Loaded .env file with " + envMap.size() + " environment variables");
        } catch (Exception e) {
            System.out.println("⚠️  No .env file found or error loading it. Using system environment variables.");
        }
    }
}
