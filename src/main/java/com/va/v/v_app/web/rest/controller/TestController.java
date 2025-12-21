package com.va.v.v_app.web.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Test REST API Controller
 * Provides sample endpoints to test the application and Swagger UI
 */
@RestController
@RequestMapping("/api/test")
@Tag(name = "Test API", description = "Test endpoints for API verification")
public class TestController {

        /**
         * Simple health check endpoint
         */
        @GetMapping("/health")
        @Operation(summary = "Health Check", description = "Returns the health status of the application")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Application is healthy", content = @Content(mediaType = "application/json", schema = @Schema(implementation = HealthResponse.class))),
                        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerErrorResponse")
        })
        public ResponseEntity<HealthResponse> healthCheck() {
                HealthResponse response = new HealthResponse(
                                "UP",
                                "Application is running",
                                LocalDateTime.now());
                return ResponseEntity.ok(response);
        }

        /**
         * Echo endpoint that returns the input message
         */
        @PostMapping("/echo")
        @Operation(summary = "Echo Message", description = "Returns the same message that was sent in the request body")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully echoed the message", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EchoResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid request")
        })
        public ResponseEntity<EchoResponse> echo(
                        @Parameter(description = "Message to echo", required = true) @RequestBody EchoRequest request) {
                EchoResponse response = new EchoResponse(
                                request.message(),
                                LocalDateTime.now());
                return ResponseEntity.ok(response);
        }

        /**
         * Greeting endpoint with path variable
         */
        @GetMapping("/greet/{name}")
        @Operation(summary = "Greet User", description = "Returns a personalized greeting message")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully generated greeting", content = @Content(mediaType = "application/json", schema = @Schema(implementation = GreetingResponse.class)))
        })
        public ResponseEntity<GreetingResponse> greet(
                        @Parameter(description = "Name of the person to greet", required = true) @PathVariable String name) {
                GreetingResponse response = new GreetingResponse(
                                String.format("Hello, %s! Welcome to V-App API.", name),
                                name,
                                LocalDateTime.now());
                return ResponseEntity.ok(response);
        }

        /**
         * Sample endpoint with query parameters
         */
        @GetMapping("/calculate")
        @Operation(summary = "Simple Calculator", description = "Performs basic arithmetic operations")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Calculation successful", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalculationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid operation")
        })
        public ResponseEntity<CalculationResponse> calculate(
                        @Parameter(description = "First number", required = true) @RequestParam Double num1,
                        @Parameter(description = "Second number", required = true) @RequestParam Double num2,
                        @Parameter(description = "Operation: add, subtract, multiply, divide", required = true) @RequestParam String operation) {
                Double result;

                switch (operation.toLowerCase()) {
                        case "add":
                                result = num1 + num2;
                                break;
                        case "subtract":
                                result = num1 - num2;
                                break;
                        case "multiply":
                                result = num1 * num2;
                                break;
                        case "divide":
                                if (num2 == 0) {
                                        return ResponseEntity.badRequest().body(
                                                        new CalculationResponse(null, num1, num2, operation,
                                                                        "Cannot divide by zero"));
                                }
                                result = num1 / num2;
                                break;
                        default:
                                return ResponseEntity.badRequest().body(
                                                new CalculationResponse(null, num1, num2, operation,
                                                                "Invalid operation"));
                }

                CalculationResponse response = new CalculationResponse(
                                result,
                                num1,
                                num2,
                                operation,
                                "Success");
                return ResponseEntity.ok(response);
        }

        // DTO Classes

        /**
         * Health check response
         */
        @Schema(description = "Health check response")
        public record HealthResponse(
                        @Schema(description = "Application status", example = "UP") String status,

                        @Schema(description = "Status message", example = "Application is running") String message,

                        @Schema(description = "Current timestamp") LocalDateTime timestamp) {
        }

        /**
         * Echo request
         */
        @Schema(description = "Echo request payload")
        public record EchoRequest(
                        @Schema(description = "Message to echo", example = "Hello World") String message) {
        }

        /**
         * Echo response
         */
        @Schema(description = "Echo response")
        public record EchoResponse(
                        @Schema(description = "Echoed message") String message,

                        @Schema(description = "Response timestamp") LocalDateTime timestamp) {
        }

        /**
         * Greeting response
         */
        @Schema(description = "Greeting response")
        public record GreetingResponse(
                        @Schema(description = "Greeting message") String greeting,

                        @Schema(description = "Name of the person greeted") String name,

                        @Schema(description = "Response timestamp") LocalDateTime timestamp) {
        }

        /**
         * Calculation response
         */
        @Schema(description = "Calculation response")
        public record CalculationResponse(
                        @Schema(description = "Calculation result") Double result,

                        @Schema(description = "First operand") Double num1,

                        @Schema(description = "Second operand") Double num2,

                        @Schema(description = "Operation performed") String operation,

                        @Schema(description = "Status message") String message) {
        }
}
