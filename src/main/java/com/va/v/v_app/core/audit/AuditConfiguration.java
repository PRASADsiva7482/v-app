package com.va.v.v_app.core.audit;

import java.io.Serializable;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import com.va.v.v_app.core.audit.bean.AuditConfig;
import lombok.Getter;
import lombok.Setter;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "audit")
@Getter
@Setter
public class AuditConfiguration implements Serializable {

    private static final long serialVersionUID = 1647047578784559889L;

    private Map<String, AuditConfig> mappings;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AuditConfiguration [");
        if (mappings != null) {
            builder.append("mappings=");
            builder.append(mappings);
        }
        builder.append("]");
        return builder.toString();
    }
}
