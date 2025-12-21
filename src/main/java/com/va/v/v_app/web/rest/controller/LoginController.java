package com.va.v.v_app.web.rest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.va.v.v_app.model.UserDetailsBean;
import com.va.v.v_app.service.CheckLoginService;
import com.va.v.v_app.service.KeycloakUserService;
import com.va.v.v_app.web.rest.annotation.UserDetailsApiResponses;
import com.va.v.v_app.web.rest.annotation.UserListApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.common.VerificationException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("checkLogin")
@Tag(name = "Authentication", description = "Check User Login with Keycloak authentication")
@Slf4j
public class LoginController {

    @Autowired
    private CheckLoginService loginService;

    @Autowired
    private KeycloakUserService keycloakUserService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TXN_ID_HEADER = "txnId";
    private static final String USER_ID_HEADER = "userId";
    private static final String TEAM_ID_HEADER = "teamId";
    private static final String SESSION_ID_HEADER = "sessionId";
    private static final String USER_NAME_HEADER = "userName";
    private static final String ENTITY_ID_HEADER = "entityId";
    private static final String LANGUAGE_ID_HEADER = "languageId";

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Validate Keycloak Token and Get User Details", description = "Login User With Keycloak Token - Validates token and returns user details with role-based menus. "
            +
            "Requires a valid JWT token in the Authorization header.")
    @UserDetailsApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> checkLogin()
            throws URISyntaxException, VerificationException, JsonProcessingException {
        log.info("Check Login Method For token: {}", MDC.get(AUTHORIZATION_HEADER));

        UserDetailsBean userDetailsBean = loginService.validateLogin();

        if (userDetailsBean == null) {
            return new ResponseEntity<>("User Details Not Found", HttpStatus.NOT_FOUND);
        }

        final String token = MDC.get(AUTHORIZATION_HEADER);

        // Build response headers
        HttpHeaders headers = new HttpHeaders();
        List<String> headerlist = new ArrayList<>();
        List<String> exposeList = new ArrayList<>();

        headerlist.add("Content-Type");
        headerlist.add("Accept");
        headerlist.add("X-Requested-With");
        headerlist.add(AUTHORIZATION_HEADER);
        headerlist.add(TXN_ID_HEADER);
        headerlist.add(USER_ID_HEADER);
        headerlist.add(TEAM_ID_HEADER);
        headerlist.add(SESSION_ID_HEADER);
        headerlist.add(USER_NAME_HEADER);
        headerlist.add(ENTITY_ID_HEADER);
        headerlist.add(LANGUAGE_ID_HEADER);

        headers.setAccessControlAllowHeaders(headerlist);

        exposeList.add(AUTHORIZATION_HEADER);
        exposeList.add(TXN_ID_HEADER);
        exposeList.add(USER_ID_HEADER);

        headers.setAccessControlExposeHeaders(exposeList);
        headers.set(AUTHORIZATION_HEADER, token);

        return new ResponseEntity<>(userDetailsBean, headers, HttpStatus.OK);
    }

    @GetMapping(value = "getUserDetailsByUserName", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get User Details by Username from Keycloak DB", description = "Retrieves user details from Keycloak database using the provided username. "
            +
            "If username is not provided, returns all users. Requires authentication.")
    @UserListApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> getUserDetailsByUserName(
            @RequestParam(value = "userName", required = false) String userName) {

        if (userName != null && !userName.trim().isEmpty()) {
            log.info("Get User Details from Keycloak DB for User Name: {}", userName);

            UserDetailsBean userDetailsBean = keycloakUserService.getUserByUsername(userName.trim());

            if (userDetailsBean == null) {
                return new ResponseEntity<>("User Details Not Found in Keycloak Database", HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(userDetailsBean, HttpStatus.OK);
        } else {
            log.info("Get All Users from Keycloak DB");

            List<UserDetailsBean> allUsers = keycloakUserService.getAllUsers();

            if (allUsers == null || allUsers.isEmpty()) {
                return new ResponseEntity<>("No Users Found in Keycloak Database", HttpStatus.NOT_FOUND);
            }

            return new ResponseEntity<>(allUsers, HttpStatus.OK);
        }
    }

    @GetMapping(value = "getAllUsers", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get All Users from Keycloak DB", description = "Retrieves all user details from Keycloak database. Requires authentication.")
    @UserDetailsApiResponses
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Object> getAllUsers() {
        log.info("Get All Users from Keycloak DB");

        List<UserDetailsBean> allUsers = keycloakUserService.getAllUsers();

        if (allUsers == null || allUsers.isEmpty()) {
            return new ResponseEntity<>("No Users Found in Keycloak Database", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(allUsers, HttpStatus.OK);
    }
}
