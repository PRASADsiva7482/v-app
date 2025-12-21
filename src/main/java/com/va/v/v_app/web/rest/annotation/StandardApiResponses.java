package com.va.v.v_app.web.rest.annotation;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom composed annotation that includes standard API responses for all
 * endpoints.
 * This eliminates the need to repeatedly define common error responses (400,
 * 401, 404, 500)
 * on every API endpoint.
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 * &#64;StandardApiResponses
 * @ApiResponse(responseCode = "200", ref = "#/components/responses/UserDetailsResponse")
 * public ResponseEntity<UserDetailsBean> getUser() {
 *     // implementation
 * }
 * }
 * </pre>
 * 
 * Or simply:
 * 
 * <pre>
 * {@code
 * @StandardApiResponses(successRef = "UserDetailsResponse")
 * public ResponseEntity<UserDetailsBean> getUser() {
 *     // implementation
 * }
 * }
 * </pre>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
        @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequestResponse"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/UnauthorizedResponse"),
        @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFoundResponse"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerErrorResponse")
})
public @interface StandardApiResponses {
    /**
     * Optional: Specify the success response reference name (without the full
     * path).
     * If provided, a 200 response will be automatically added.
     * Example: "UserDetailsResponse" will be converted to
     * "#/components/responses/UserDetailsResponse"
     */
    String successRef() default "";
}
