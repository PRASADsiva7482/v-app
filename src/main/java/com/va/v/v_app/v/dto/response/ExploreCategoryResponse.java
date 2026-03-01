package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for explore category
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExploreCategoryResponse {

    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String icon;
    private Integer sortOrder;
}
