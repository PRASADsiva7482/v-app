package com.va.v.v_app.web.rest.controller;

import com.va.v.v_app.model.UserDetailsBean;
import com.va.v.v_app.service.CachedKeycloakUserService;
import com.va.v.v_app.web.rest.annotation.UserDetailsApiResponses;
import com.va.v.v_app.web.rest.annotation.UserListApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller for cached Keycloak user operations
 */
@RestController
@RequestMapping("api/keycloak-users")
@Tag(name = "Keycloak User Cache", description = "Cached Keycloak user data with zero-downtime updates")
@Slf4j
public class KeycloakUserCacheController {

    @Autowired
    private CachedKeycloakUserService cachedKeycloakUserService;

    @GetMapping(value = "/cached", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get All Users from Cache", description = "Returns all Keycloak users from cache. If cache is empty, fetches from database with pagination (100 users/page) and caches the result. "
            +
            "Cache is automatically refreshed daily at 2 AM. Duplicates are automatically removed.")
    @UserListApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> getAllUsersFromCache() {
        log.info("API called: Get all users from cache");

        try {
            long startTime = System.currentTimeMillis();
            List<UserDetailsBean> users = cachedKeycloakUserService.getAllUsersFromCache();
            long duration = System.currentTimeMillis() - startTime;

            if (users == null || users.isEmpty()) {
                log.warn("No users found in cache or database");
                return new ResponseEntity<>("No Users Found", HttpStatus.NOT_FOUND);
            }

            log.info("Returned {} users from cache in {} ms", users.size(), duration);
            return new ResponseEntity<>(users, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error fetching users from cache", e);
            return new ResponseEntity<>("Error fetching users: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Manually Refresh Users Cache", description = "Manually triggers cache refresh with zero-downtime using @CachePut. "
            +
            "Fetches all users from database with pagination (100 users/page) and updates cache. " +
            "Old cache remains available during refresh. Use this when new users are registered in Keycloak.")
    @UserListApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> refreshCache() {
        log.info("API called: Manual cache refresh");

        try {
            long startTime = System.currentTimeMillis();
            List<UserDetailsBean> users = cachedKeycloakUserService.refreshAllUsersCache();
            long duration = System.currentTimeMillis() - startTime;

            if (users == null || users.isEmpty()) {
                log.warn("No users found during cache refresh");
                return new ResponseEntity<>("No Users Found", HttpStatus.NOT_FOUND);
            }

            log.info("Cache refreshed successfully with {} users in {} ms", users.size(), duration);

            return new ResponseEntity<>(Map.of(
                    "message", "Cache refreshed successfully",
                    "totalUsers", users.size(),
                    "durationMs", duration,
                    "users", users), HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error refreshing cache", e);
            return new ResponseEntity<>("Error refreshing cache: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/cache-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get Cache Statistics", description = "Returns statistics about the cached users including total count and cache status.")
    @UserDetailsApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> getCacheStats() {
        log.info("API called: Get cache statistics");

        try {
            List<UserDetailsBean> users = cachedKeycloakUserService.getAllUsersFromCache();

            return new ResponseEntity<>(Map.of(
                    "totalUsers", users != null ? users.size() : 0,
                    "cacheEnabled", true,
                    "scheduledRefresh", "Daily at 2:00 AM",
                    "pageSize", 100,
                    "duplicatesHandling", "Automatically removed"), HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error getting cache statistics", e);
            return new ResponseEntity<>("Error getting cache stats: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping(value = "/clear", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Clear and Reload Cache", description = "Clears the cache and immediately reloads it from the database. Use with caution.")
    @UserDetailsApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> clearAndReloadCache() {
        log.info("API called: Clear and reload cache");

        try {
            cachedKeycloakUserService.clearAndReloadCache();

            return new ResponseEntity<>(Map.of(
                    "message", "Cache cleared and reloaded successfully"), HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error clearing and reloading cache", e);
            return new ResponseEntity<>("Error clearing cache: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
