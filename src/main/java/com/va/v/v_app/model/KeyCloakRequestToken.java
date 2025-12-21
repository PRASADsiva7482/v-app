package com.va.v.v_app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeyCloakRequestToken {
    private String client_secret;
    private String client_id;
    private String token;
}
