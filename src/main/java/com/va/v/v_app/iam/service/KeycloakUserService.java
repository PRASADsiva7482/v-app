package com.va.v.v_app.iam.service;

import com.va.v.v_app.iam.model.UserDetailsBean;

import java.util.List;

/**
 * Service interface for fetching user details from Keycloak database
 */
public interface KeycloakUserService {

    /**
     * Get all users from Keycloak database
     * 
     * @return List of UserDetailsBean
     */
    List<UserDetailsBean> getAllUsers();

    /**
     * Get user details by username from Keycloak database
     * 
     * @param username the username
     * @return UserDetailsBean
     */
    UserDetailsBean getUserByUsername(String username);
}
