package com.va.v.v_app.social.dto.response;

import com.va.v.v_app.social.model.Media;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for media
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {

    private Long id;
    private Long postId;
    private Media.MediaType mediaType;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private Integer duration;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
}
