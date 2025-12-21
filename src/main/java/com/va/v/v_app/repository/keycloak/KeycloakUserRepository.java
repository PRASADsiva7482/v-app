package com.va.v.v_app.repository.keycloak;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.va.v.v_app.persistence.keycloak.KeycloakUser;

import java.util.List;
import java.util.Optional;

/**
 * Repository for querying Keycloak user_entity table
 */
@Repository
public interface KeycloakUserRepository extends JpaRepository<KeycloakUser, String> {

    /**
     * Find user by username
     * 
     * @param username the username
     * @return Optional<KeycloakUser>
     */
    Optional<KeycloakUser> findByUsername(String username);

    /**
     * Find all enabled users
     * 
     * @param enabled whether user is enabled
     * @return List of enabled users
     */
    List<KeycloakUser> findByEnabled(Boolean enabled);

    /**
     * Find all users by realm ID
     * 
     * @param realmId the realm ID
     * @return List of users in the realm
     */
    List<KeycloakUser> findByRealmId(String realmId);

    /**
     * Find all users with pagination support
     * 
     * @param pageable pagination information
     * @return Page of users
     */
    Page<KeycloakUser> findAll(Pageable pageable);

    /**
     * Count total number of users
     * 
     * @return total count
     */
    @Query("SELECT COUNT(u) FROM KeycloakUser u")
    long countAllUsers();
}
