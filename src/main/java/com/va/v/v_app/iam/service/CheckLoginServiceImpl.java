package com.va.v.v_app.iam.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.va.v.v_app.iam.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.authorization.Permission;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CheckLoginServiceImpl implements CheckLoginService {

    @Autowired
    private KeycloakRequestMaker ssoReqMaker;

    @Autowired
    private MenuAndRoleIteratorService menuFunctionIterator;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_ID_HEADER = "userId";

    @Override
    @SuppressWarnings("unchecked")
    public UserDetailsBean validateLogin() throws URISyntaxException, VerificationException, JsonProcessingException {

        if (StringUtils.isEmpty(MDC.get(AUTHORIZATION_HEADER))) {
            throw new RuntimeException("Token is not available to validate");
        }

        UserDetailsBean bean = new UserDetailsBean();
        Long pst = System.currentTimeMillis();

        log.info("Check Login Method initiated");

        /**
         * Trigger SSO with header to get the user menus and function mapping
         */
        KeycloakAccessToken token = ssoReqMaker.initiateSSOReq();

        // Parse the access token to extract user details and permissions
        AccessToken acctoken = TokenVerifier.create(token.getAccess_token(), AccessToken.class).getToken();

        // Extract user details from token
        bean.setUserName(acctoken.getPreferredUsername());
        bean.setFullName(acctoken.getGivenName());
        bean.setEmailId(acctoken.getEmail());
        bean.setUserId(acctoken.getSubject());

        // Extract group names from token (if available)
        Map<String, Object> otherClaims = acctoken.getOtherClaims();
        if (otherClaims != null && otherClaims.containsKey("groups")) {
            List<String> groupNames = (List<String>) otherClaims.get("groups");
            if (groupNames != null) {
                String formattedGroupNames = groupNames.stream()
                        .map(group -> group.startsWith("/") ? group.substring(1) : group) // Remove leading "/"
                        .collect(Collectors.joining(",")); // Join with a comma
                bean.setGroupNames(formattedGroupNames);
            } else {
                log.info("Group names not found in token");
            }
        }

        log.info("Setting the userId to MDC: {}", bean.getUserId());

        // Set userId to MDC for audit and logging
        if (StringUtils.isEmpty(MDC.get(USER_ID_HEADER)) || "1".equals(MDC.get(USER_ID_HEADER))) {
            MDC.put(USER_ID_HEADER, bean.getUserId());
        }

        /**
         * Extract resources and scopes from Keycloak permissions
         */
        List<String> resourceNames = new ArrayList<>();
        List<String> scopes = new ArrayList<>();

        // Get authorization permissions from token
        if (acctoken.getAuthorization() != null && acctoken.getAuthorization().getPermissions() != null) {
            Collection<Permission> permissions = acctoken.getAuthorization().getPermissions();
            permissions.forEach(permission -> {
                resourceNames.add(permission.getResourceName());
                if (permission.getScopes() != null) {
                    scopes.addAll(permission.getScopes().stream().collect(Collectors.toList()));
                }
            });
        }

        log.info("Extracted {} resources and {} scopes from token", resourceNames.size(), scopes.size());

        /**
         * Build role-based menu structure
         */
        RoleDetailsBean roleDetailsBean = new RoleDetailsBean();
        List<RoleMenuBean> menuList = menuFunctionIterator.iterateMenuAndRoleDetails(resourceNames, 0, scopes);

        /**
         * Group the menus to a map based on parent menu id
         */
        Map<Integer, List<RoleMenuBean>> menuRelationMap = menuList.stream()
                .filter(menu -> menu.getParentId() != 0)
                .collect(Collectors.groupingBy(RoleMenuBean::getParentId));

        /**
         * Collect the menu with parent and child relation
         */
        List<RoleMenuBean> finalMenuList = menuList.stream()
                .filter(menu -> menu.getParentId() == 0)
                .map(v -> {
                    v.setChilds(menuRelationMap.get(v.getMenuId()));
                    return v;
                })
                .collect(Collectors.toList());

        roleDetailsBean.setMenuIds(finalMenuList);
        bean.setRoleDetails(roleDetailsBean);

        log.info("Check Login completed successfully in {} ms", (System.currentTimeMillis() - pst));

        return bean;
    }

    @Override
    public UserDetailsBean loadByUserName(String userName) {
        log.info("Get User Details Method For User Name: {}", userName);

        // This is a placeholder - implement based on your user repository
        // For now, returning a basic bean
        UserDetailsBean bean = new UserDetailsBean();
        bean.setUserName(userName);

        // TODO: Implement user lookup from database if needed
        // Optional<UserDetailsDomain> existing =
        // userDetailsRepo.findByUserName(userName);
        // if (!existing.isPresent()) {
        // throw new RuntimeException("User Not Found");
        // }
        // Map domain to bean

        return bean;
    }
}
