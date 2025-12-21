package com.va.v.v_app.persistence.keycloak;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.persistence.*;

/**
 * JPA Entity mapping to Keycloak's user_entity table
 */
@Entity
@Table(name = "user_entity")
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class KeycloakUser {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "email_constraint", length = 255)
    private String emailConstraint;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "federation_link", length = 255)
    private String federationLink;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "realm_id", length = 255)
    private String realmId;

    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "created_timestamp")
    private Long createdTimestamp;

    @Column(name = "service_account_client_link", length = 255)
    private String serviceAccountClientLink;

    @Column(name = "not_before")
    private Integer notBefore;
}
