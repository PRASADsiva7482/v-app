package com.va.v.v_app.social.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Request DTO for creating a new post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @Size(max = 5000, message = "Post content cannot exceed 5000 characters")
    private String content;

    @Size(max = 4, message = "Cannot attach more than 4 media files")
    private List<Long> mediaIds;

    private List<String> mentionedUserIds;

    // Optional poll data
    @Valid
    private CreatePollRequest poll;

    private Boolean isDraft;
    private LocalDateTime scheduledFor;
}
