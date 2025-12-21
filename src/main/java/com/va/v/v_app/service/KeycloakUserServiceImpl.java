package com.va.v.v_app.service;

import com.va.v.v_app.config.utils.Constants;
import com.va.v.v_app.model.UserDetailsBean;
import com.va.v.v_app.persistence.keycloak.KeycloakUser;
import com.va.v.v_app.repository.keycloak.KeycloakUserRepository;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation for fetching user details from Keycloak database
 */
@Service
@Slf4j
public class KeycloakUserServiceImpl implements KeycloakUserService {

    @Autowired
    private KeycloakUserRepository keycloakUserRepository;

    @Override
    public List<UserDetailsBean> getAllUsers() {
        log.info("Fetching all users from Keycloak database");

        // Set the MDC context to use the initial keycloak datasource
        MDC.put(Constants.keycloak_db_instance, Constants.initial_db_name);

        try {
            List<KeycloakUser> keycloakUsers = keycloakUserRepository.findAll();
            log.info("Found {} users in Keycloak database", keycloakUsers.size());

            return keycloakUsers.stream()
                    .map(this::mapToUserDetailsBean)
                    .collect(Collectors.toList());
        } finally {
            // Clean up MDC
            MDC.remove(Constants.keycloak_db_instance);
        }
    }

    @Override
    public UserDetailsBean getUserByUsername(String username) {
        log.info("Fetching user details for username: {}", username);

        // Set the MDC context to use the initial keycloak datasource
        MDC.put(Constants.keycloak_db_instance, Constants.initial_db_name);

        try {
            Optional<KeycloakUser> keycloakUser = keycloakUserRepository.findByUsername(username);

            if (keycloakUser.isPresent()) {
                log.info("Found user in Keycloak database: {}", username);
                return mapToUserDetailsBean(keycloakUser.get());
            } else {
                log.warn("User not found in Keycloak database: {}", username);
                return null;
            }
        } finally {
            // Clean up MDC
            MDC.remove(Constants.keycloak_db_instance);
        }
    }

    /**
     * Map KeycloakUser entity to UserDetailsBean
     * 
     * @param keycloakUser the Keycloak user entity
     * @return UserDetailsBean
     */
    private UserDetailsBean mapToUserDetailsBean(KeycloakUser keycloakUser) {
        UserDetailsBean bean = new UserDetailsBean();
        bean.setUserId(keycloakUser.getId());
        bean.setUserName(keycloakUser.getUsername());
        bean.setEmailId(keycloakUser.getEmail());

        // Combine first name and last name for full name
        StringBuilder fullName = new StringBuilder();
        if (keycloakUser.getFirstName() != null) {
            fullName.append(keycloakUser.getFirstName());
        }
        if (keycloakUser.getLastName() != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(keycloakUser.getLastName());
        }
        bean.setFullName(fullName.toString());

        return bean;
    }
}
