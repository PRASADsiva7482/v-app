package com.va.v.v_app.config.audit.bean;

import java.io.Serializable;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class AuditConfig implements Serializable {

    private static final long serialVersionUID = -8945678901234567890L;

    private Boolean auditReq;
    private String auditFeatureId;
    private String auditOperationId;
    private String entityId;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AuditConfig [");
        if (auditReq != null) {
            builder.append("auditReq=");
            builder.append(auditReq);
            builder.append(", ");
        }
        if (auditFeatureId != null) {
            builder.append("auditFeatureId=");
            builder.append(auditFeatureId);
            builder.append(", ");
        }
        if (auditOperationId != null) {
            builder.append("auditOperationId=");
            builder.append(auditOperationId);
            builder.append(", ");
        }
        if (entityId != null) {
            builder.append("entityId=");
            builder.append(entityId);
        }
        builder.append("]");
        return builder.toString();
    }
}
