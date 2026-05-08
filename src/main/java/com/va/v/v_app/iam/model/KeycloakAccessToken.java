package com.va.v.v_app.iam.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeycloakAccessToken {
    private String access_token;
    private String expires_in;
    private String refresh_expires_in;
    private String refresh_token;
    private String token_type;
    private String session_state;
    private String scope;
    private String active;

    // Token introspection response fields
    private String sub; // User ID
    private String username;
    private String email;
    private String preferred_username;
    private String name;
    private String given_name;
    private String family_name;
}
