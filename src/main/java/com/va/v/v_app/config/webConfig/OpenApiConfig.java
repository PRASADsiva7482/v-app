package com.va.v.v_app.config.webConfig;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) Configuration
 * Configures the API documentation settings with JWT Bearer Token
 * authentication and common response components
 */
@Configuration
public class OpenApiConfig {

        private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

        @Value("${server.servlet.context-path:/}")
        private String contextPath;

        @Value("${server.port:8080}")
        private String serverPort;

        @Bean
        public OpenAPI customOpenAPI() {
                Server server = new Server();
                server.setUrl("http://localhost:" + serverPort + contextPath);
                server.setDescription("Development Server");

                Contact contact = new Contact();
                contact.setName("V-App Development Team");
                contact.setEmail("dev@v-app.com");

                License license = new License();
                license.setName("Apache 2.0");
                license.setUrl("https://www.apache.org/licenses/LICENSE-2.0.html");

                Info info = new Info()
                                .title("V-App REST API")
                                .version("1.0.0")
                                .description("REST API documentation for V-App application. " +
                                                "This API provides endpoints for managing application resources and operations. "
                                                +
                                                "\n\n**Authentication:** This API uses JWT Bearer token authentication. "
                                                +
                                                "To authenticate, click the 'Authorize' button and enter your JWT token.")
                                .contact(contact)
                                .license(license);

                // Define the security scheme
                SecurityScheme securityScheme = new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token in the format: your-jwt-token-here (without 'Bearer' prefix)");

                // Add security requirement
                SecurityRequirement securityRequirement = new SecurityRequirement()
                                .addList(SECURITY_SCHEME_NAME);

                // Create common response components
                Components components = new Components()
                                .addSecuritySchemes(SECURITY_SCHEME_NAME, securityScheme)
                                // Common Success Responses
                                .addResponses("SuccessResponse",
                                                createCommonResponse("200", "Successful operation",
                                                                createSchema("object", "Success response")))
                                // Common Error Responses
                                .addResponses("BadRequestResponse",
                                                createCommonResponse("400", "Bad Request - Invalid input parameters",
                                                                createErrorSchema("Bad Request",
                                                                                "The request parameters are invalid or missing required fields")))
                                .addResponses("UnauthorizedResponse",
                                                createCommonResponse("401", "Unauthorized - Authentication required",
                                                                createErrorSchema("Unauthorized",
                                                                                "Valid authentication token is required to access this resource")))
                                .addResponses("ForbiddenResponse",
                                                createCommonResponse("403", "Forbidden - Insufficient permissions",
                                                                createErrorSchema("Forbidden",
                                                                                "You don't have permission to access this resource")))
                                .addResponses("NotFoundResponse",
                                                createCommonResponse("404", "Not Found - Resource does not exist",
                                                                createErrorSchema("Not Found",
                                                                                "The requested resource was not found")))
                                .addResponses("InternalServerErrorResponse",
                                                createCommonResponse("500", "Internal Server Error",
                                                                createErrorSchema("Internal Server Error",
                                                                                "An unexpected error occurred while processing the request")))
                                // Common Schema Components
                                .addSchemas("ErrorResponse", createErrorResponseSchema())
                                .addSchemas("SuccessMessage", createSuccessMessageSchema())
                                // Common Success Responses with specific schemas
                                .addResponses("UserDetailsResponse",
                                                createResponseWithImplementation(
                                                                "Successfully validated token and retrieved user details",
                                                                com.va.v.v_app.model.UserDetailsBean.class));

                return new OpenAPI()
                                .info(info)
                                .servers(List.of(server))
                                .components(components)
                                .addSecurityItem(securityRequirement);
        }

        /**
         * Create a common API response
         */
        private ApiResponse createCommonResponse(String code, String description, Schema<?> schema) {
                return new ApiResponse()
                                .description(description)
                                .content(new Content()
                                                .addMediaType("application/json",
                                                                new MediaType().schema(schema)));
        }

        /**
         * Create API response with specific implementation class
         */
        private ApiResponse createResponseWithImplementation(String description, Class<?> implementationClass) {
                Schema<?> schema = new Schema<>();
                schema.set$ref("#/components/schemas/" + implementationClass.getSimpleName());

                return new ApiResponse()
                                .description(description)
                                .content(new Content()
                                                .addMediaType("application/json",
                                                                new MediaType().schema(schema)));
        }

        /**
         * Create a generic schema
         */
        private Schema<?> createSchema(String type, String description) {
                Schema<?> schema = new Schema<>();
                schema.setType(type);
                schema.setDescription(description);
                return schema;
        }

        /**
         * Create error schema with example
         */
        private Schema<?> createErrorSchema(String errorType, String exampleMessage) {
                Schema<?> schema = new Schema<>();
                schema.setType("object");
                schema.addProperty("error", new Schema<>().type("string").example(errorType));
                schema.addProperty("message", new Schema<>().type("string").example(exampleMessage));
                schema.addProperty("timestamp", new Schema<>().type("string").format("date-time")
                                .example("2025-12-21T18:00:00Z"));
                schema.addProperty("path", new Schema<>().type("string").example("/api/resource"));
                return schema;
        }

        /**
         * Create standard error response schema
         */
        private Schema<?> createErrorResponseSchema() {
                Schema<?> schema = new Schema<>();
                schema.setType("object");
                schema.setDescription("Standard error response");
                schema.addProperty("error", new Schema<>().type("string").description("Error type"));
                schema.addProperty("message", new Schema<>().type("string").description("Error message"));
                schema.addProperty("timestamp", new Schema<>().type("string").format("date-time")
                                .description("Timestamp when error occurred"));
                schema.addProperty("path", new Schema<>().type("string").description("Request path"));
                schema.setRequired(List.of("error", "message", "timestamp"));
                return schema;
        }

        /**
         * Create standard success message schema
         */
        private Schema<?> createSuccessMessageSchema() {
                Schema<?> schema = new Schema<>();
                schema.setType("object");
                schema.setDescription("Standard success response");
                schema.addProperty("status", new Schema<>().type("string").example("success")
                                .description("Operation status"));
                schema.addProperty("message", new Schema<>().type("string")
                                .example("Operation completed successfully")
                                .description("Success message"));
                schema.addProperty("timestamp", new Schema<>().type("string").format("date-time")
                                .example("2025-12-21T18:00:00Z")
                                .description("Timestamp of operation"));
                schema.setRequired(List.of("status", "message"));
                return schema;
        }
}
