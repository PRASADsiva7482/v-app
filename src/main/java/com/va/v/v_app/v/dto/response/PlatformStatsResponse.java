package com.va.v.v_app.v.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for platform statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsResponse {
    private Long totalUsers;
    private Long totalPosts;
    private Long totalLikes;
    private Long totalViews;
    private Long totalComments;
    private Long totalFollows;
    private Long activeUsersToday;
    private Long postsToday;
}
