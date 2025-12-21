package com.va.v.v_app.service;

import com.va.v.v_app.model.UserDetailsBean;

import java.util.List;

/**
 * Service interface for cached Keycloak user operations
 */
public interface CachedKeycloakUserService {

    /**
     * Get all users from cache. If cache is empty, fetches from DB and populates
     * cache
     * 
     * @return List of all users
     */
    List<UserDetailsBean> getAllUsersFromCache();

    /**
     * Refresh all users in cache with pagination (100 per page)
     * Uses @CachePut for zero-downtime updates
     * 
     * @return List of refreshed users
     */
    List<UserDetailsBean> refreshAllUsersCache();

    /**
     * Clear and reload the cache
     */
    void clearAndReloadCache();
}
