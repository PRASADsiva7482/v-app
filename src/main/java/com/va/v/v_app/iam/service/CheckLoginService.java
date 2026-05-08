package com.va.v.v_app.iam.service;

import org.keycloak.common.VerificationException;

import java.net.URISyntaxException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.va.v.v_app.iam.model.UserDetailsBean;

public interface CheckLoginService {

    /**
     * Validates the login by checking Keycloak token and building user details with
     * menus
     * 
     * @return UserDetailsBean with user info and role-based menus
     * @throws URISyntaxException
     * @throws VerificationException
     * @throws JsonProcessingException
     */
    UserDetailsBean validateLogin() throws URISyntaxException, VerificationException, JsonProcessingException;

    /**
     * Load user details by username
     * 
     * @param userName
     * @return UserDetailsBean
     */
    UserDetailsBean loadByUserName(String userName);
}
