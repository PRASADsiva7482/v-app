package com.va.v.v_app.social.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating an existing post.
 * Supports updating content and managing media attachments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @Size(max = 5000, message = "Post content cannot exceed 5000 characters")
    private String content;

    /**
     * New media IDs to attach to the post (already uploaded via /media/upload).
     * These are ADDED to the post.
     */
    @Size(max = 4, message = "Cannot attach more than 4 media files")
    private List<Long> addMediaIds;

    /**
     * Existing media IDs to remove from the post.
     * The corresponding files will be deleted from storage.
     */
    private List<Long> removeMediaIds;

    // IDs of mentioned users
    private List<String> mentionedUserIds;
}
