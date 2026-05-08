package com.va.v.v_app.iam.service;

import com.va.v.v_app.core.config.CacheConfig;
import com.va.v.v_app.core.utils.Constants;
import com.va.v.v_app.iam.model.UserDetailsBean;
import com.va.v.v_app.iam.model.KeycloakUser;
import com.va.v.v_app.iam.repository.keycloak.KeycloakUserRepository;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementation for cached Keycloak user operations with pagination
 */
@Service
@Slf4j
public class CachedKeycloakUserServiceImpl implements CachedKeycloakUserService {

    @Autowired
    private KeycloakUserRepository keycloakUserRepository;

    private static final int PAGE_SIZE = 100;

    @Override
    @Cacheable(value = CacheConfig.KEYCLOAK_USERS_CACHE, key = "'allUsers'")
    public List<UserDetailsBean> getAllUsersFromCache() {
        log.info("Cache miss - fetching all users from Keycloak database");
        return fetchAllUsersWithPagination();
    }

    @Override
    @CachePut(value = CacheConfig.KEYCLOAK_USERS_CACHE, key = "'allUsers'")
    public List<UserDetailsBean> refreshAllUsersCache() {
        log.info("Refreshing Keycloak users cache with @CachePut (zero-downtime)");
        return fetchAllUsersWithPagination();
    }

    @Override
    public void clearAndReloadCache() {
        log.info("Clearing and reloading Keycloak users cache");
        refreshAllUsersCache();
    }

    /**
     * Scheduled task to refresh cache daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledCacheRefresh() {
        log.info("Starting scheduled Keycloak users cache refresh");
        long startTime = System.currentTimeMillis();

        try {
            List<UserDetailsBean> users = refreshAllUsersCache();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Scheduled cache refresh completed successfully. Loaded {} users in {} ms",
                    users.size(), duration);
        } catch (Exception e) {
            log.error("Error during scheduled cache refresh", e);
        }
    }

    /**
     * Fetch all users with pagination (100 users per page) and remove duplicates
     * 
     * @return List of unique UserDetailsBean
     */
    private List<UserDetailsBean> fetchAllUsersWithPagination() {
        // Set MDC context for Keycloak datasource
        MDC.put(Constants.keycloak_db_instance, Constants.initial_db_name);

        try {
            log.info("Starting paginated fetch of Keycloak users");

            // Get total count
            long totalUsers = keycloakUserRepository.countAllUsers();
            log.info("Total users in Keycloak database: {}", totalUsers);

            // Calculate total pages
            int totalPages = (int) Math.ceil((double) totalUsers / PAGE_SIZE);
            log.info("Will fetch {} pages with page size of {}", totalPages, PAGE_SIZE);

            // Use LinkedHashMap to maintain order and remove duplicates by userId
            Map<String, UserDetailsBean> uniqueUsersMap = new LinkedHashMap<>();

            // Fetch all pages
            for (int pageNumber = 0; pageNumber < totalPages; pageNumber++) {
                Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
                Page<KeycloakUser> page = keycloakUserRepository.findAll(pageable);

                log.info("Fetched page {} of {} with {} users",
                        pageNumber + 1, totalPages, page.getNumberOfElements());

                // Convert to UserDetailsBean and add to map (automatically removes duplicates)
                page.getContent().forEach(keycloakUser -> {
                    UserDetailsBean bean = mapToUserDetailsBean(keycloakUser);
                    // Only add if not already present (keeps first occurrence)
                    uniqueUsersMap.putIfAbsent(bean.getUserId(), bean);
                });
            }

            List<UserDetailsBean> result = new ArrayList<>(uniqueUsersMap.values());
            log.info("Completed fetching all users. Total unique users: {}, Duplicates removed: {}",
                    result.size(), (totalUsers - result.size()));

            return result;

        } catch (Exception e) {
            log.error("Error fetching users with pagination", e);
            throw new RuntimeException("Failed to fetch users from Keycloak database", e);
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
