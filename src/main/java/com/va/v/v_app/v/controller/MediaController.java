package com.va.v.v_app.v.controller;

import com.va.v.v_app.v.dto.response.MediaResponse;
import com.va.v.v_app.v.model.Media;
import com.va.v.v_app.v.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for media operations
 */
@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media", description = "Media management APIs")
public class MediaController {

    private final MediaService mediaService;

    @Value("${feature.media.storage.image-path}")
    private String imagePath;

    @Value("${feature.media.storage.video-path}")
    private String videoPath;

    @Value("${feature.media.storage.profile-picture-path}")
    private String profilePicturePath;

    @Operation(summary = "Upload a single media file")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaResponse> uploadMedia(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        String userId = authentication.getName();
        Media media = mediaService.uploadMedia(file, userId);
        MediaResponse response = mediaService.mapToResponse(media);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Upload multiple media files (up to 4)")
    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadMultipleMedia(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) throws IOException {
        String userId = authentication.getName();
        List<Media> mediaList = mediaService.uploadMultipleMedia(files, userId);
        List<MediaResponse> responses = mediaService.mapToResponseList(mediaList);

        Map<String, Object> result = new HashMap<>();
        result.put("count", responses.size());
        result.put("media", responses);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Operation(summary = "Get media by ID")
    @GetMapping("/{mediaId}")
    public ResponseEntity<MediaResponse> getMedia(@PathVariable Long mediaId) {
        Media media = mediaService.getMediaById(mediaId);
        MediaResponse response = mediaService.mapToResponse(media);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get media by post ID")
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<MediaResponse>> getMediaByPost(@PathVariable Long postId) {
        List<Media> mediaList = mediaService.getMediaByPostId(postId);
        List<MediaResponse> responses = mediaService.mapToResponseList(mediaList);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Delete media by ID")
    @DeleteMapping("/{mediaId}")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable Long mediaId,
            Authentication authentication) {
        // TODO: Add authorization check - only owner or admin can delete
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Serve image file")
    @GetMapping("/images/{filename:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable String filename) {
        return serveFile(imagePath, filename);
    }

    @Operation(summary = "Serve video file")
    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<Resource> serveVideo(@PathVariable String filename) {
        return serveFile(videoPath, filename);
    }

    @Operation(summary = "Serve profile picture file")
    @GetMapping("/profile-pictures/{filename:.+}")
    public ResponseEntity<Resource> serveProfilePicture(@PathVariable String filename) {
        return serveFile(profilePicturePath, filename);
    }

    // ==================== Private Helper Methods ====================

    private ResponseEntity<Resource> serveFile(String basePath, String filename) {
        try {
            Path filePath = Paths.get(basePath).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Determine content type
                String contentType = determineContentType(filename);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.error("File not found or not readable: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL for file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mov" -> "video/quicktime";
            default -> "application/octet-stream";
        };
    }
}
