package com.va.v.v_app.config.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Custom Cache Key Generator for v-app
 * 
 * Generates meaningful cache keys in the format:
 * {cacheName}:{className}:{methodName}:{param1}:{param2}:...
 * 
 * Example keys:
 * - crm:customer:UserProfileService:getProfileByUserId:12345
 * - billing:invoice:InvoiceService:getInvoiceById:INV-2026-001
 * - keycloak:user:KeycloakUserService:getUserById:uuid-123-456
 * 
 * @author v-app team
 * @since 2026-01-11
 */
@Component("customKeyGenerator")
public class CustomCacheKeyGenerator implements KeyGenerator {

    private static final String DELIMITER = ":";

    @Override
    public Object generate(Object target, Method method, Object... params) {
        StringBuilder key = new StringBuilder();

        // Add class name (simple name, not fully qualified)
        key.append(target.getClass().getSimpleName());
        key.append(DELIMITER);

        // Add method name
        key.append(method.getName());

        // Add parameters if present
        if (params != null && params.length > 0) {
            key.append(DELIMITER);
            String paramsString = Arrays.stream(params)
                    .map(param -> param != null ? param.toString() : "null")
                    .collect(Collectors.joining(DELIMITER));
            key.append(paramsString);
        }

        return key.toString();
    }
}
