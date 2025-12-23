package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for like
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponse {

    private Long id;
    private String userId;
    private LocalDateTime createdAt;

    // Related data
    private UserProfileResponse user;
}
