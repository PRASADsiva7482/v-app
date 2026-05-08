package com.va.v.v_app.social.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a poll with a post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePollRequest {

    @NotBlank(message = "Poll question is required")
    @Size(max = 500, message = "Poll question cannot exceed 500 characters")
    private String question;

    @NotNull(message = "Poll options are required")
    @Size(min = 2, max = 4, message = "Poll must have between 2 and 4 options")
    private List<@NotBlank(message = "Option text cannot be blank") @Size(max = 200, message = "Option text cannot exceed 200 characters") String> options;

    @Builder.Default
    private Integer durationHours = 24; // Default 24 hours
}
