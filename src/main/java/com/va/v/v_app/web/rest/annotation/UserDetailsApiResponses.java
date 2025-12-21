package com.va.v.v_app.web.rest.annotation;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom composed annotation for APIs that return UserDetails.
 * Includes both success (200) and all standard error responses.
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 * @UserDetailsApiResponses
 * public ResponseEntity<UserDetailsBean> getUser() {
 *     // implementation
 * }
 * }
 * </pre>
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", ref = "#/components/responses/UserDetailsResponse"),
        @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequestResponse"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/UnauthorizedResponse"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFoundResponse"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerErrorResponse")
})
public @interface UserDetailsApiResponses {
}
