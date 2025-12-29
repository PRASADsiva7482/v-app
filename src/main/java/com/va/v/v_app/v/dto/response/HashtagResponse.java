package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Hashtag
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HashtagResponse {

    private Long id;
    private String tagName; // Without # symbol
    private Long usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}
