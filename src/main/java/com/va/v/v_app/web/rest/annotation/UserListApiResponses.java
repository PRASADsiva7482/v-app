package com.va.v.v_app.web.rest.annotation;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.va.v.v_app.model.UserDetailsBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Standard API responses annotation for endpoints returning a list of
 * UserDetailsBean
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user details", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserDetailsBean.class)))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing authentication token", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "No users found", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(mediaType = "application/json"))
})
public @interface UserListApiResponses {
}
