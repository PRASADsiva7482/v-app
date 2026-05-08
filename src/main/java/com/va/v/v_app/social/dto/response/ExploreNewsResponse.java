package com.va.v.v_app.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for explore news item
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreNewsResponse {

    private Long id;
    private String headline;
    private String description;
    private String source;
    private String sourceUrl;
    private String imageUrl;
    private String categoryName;
    private String categoryDisplayName;
    private String categoryIcon;
    private Long postCount;
    private Boolean isBreaking;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
