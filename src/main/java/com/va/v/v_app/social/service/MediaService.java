package com.va.v.v_app.social.service;

import com.va.v.v_app.social.dto.response.MediaResponse;
import com.va.v.v_app.social.model.Media;
import com.va.v.v_app.social.model.Post;
import com.va.v.v_app.social.repository.MediaRepository;
import com.va.v.v_app.social.repository.PostRepository;
import com.va.v.v_app.social.exception.BusinessException;
import com.va.v.v_app.social.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing media files (images, videos)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final PostRepository postRepository;

    @Value("${feature.media.storage.base-path}")
    private String basePath;

    @Value("${feature.media.storage.image-path}")
    private String imagePath;

    @Value("${feature.media.storage.video-path}")
    private String videoPath;

    @Value("${feature.media.storage.thumbnail-path}")
    private String thumbnailPath;

    @Value("${feature.media.upload.max-image-size}")
    private long maxImageSize;

    @Value("${feature.media.upload.max-video-size}")
    private long maxVideoSize;

    @Value("${feature.media.upload.allowed-image-types}")
    private String allowedImageTypes;

    @Value("${feature.media.upload.allowed-video-types}")
    private String allowedVideoTypes;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    /**
     * Upload a single media file
     */
    @Transactional
    public Media uploadMedia(MultipartFile file, String userId) throws IOException {
        validateFile(file);

        // Determine media type
        Media.MediaType mediaType = determineMediaType(file);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = generateUniqueFilename(userId, extension);

        // Determine storage path
        String storagePath = mediaType == Media.MediaType.VIDEO ? videoPath : imagePath;
        Path targetPath = Paths.get(storagePath, uniqueFilename);

        // Create directories if they don't exist
        Files.createDirectories(targetPath.getParent());

        // Save file
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Build file URL for serving
        String fileUrl = buildFileUrl(mediaType, uniqueFilename);

        // Create Media entity
        Media media = Media.builder()
                .mediaType(mediaType)
                .fileName(originalFilename)
                .filePath(targetPath.toString())
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .build();

        // Extract metadata
        if (mediaType == Media.MediaType.IMAGE || mediaType == Media.MediaType.GIF) {
            extractImageMetadata(media, targetPath.toFile());
        }

        Media savedMedia = mediaRepository.save(media);
        log.info("Uploaded media file: {} (ID: {}) for user: {}", uniqueFilename, savedMedia.getId(), userId);

        return savedMedia;
    }

    /**
     * Upload multiple media files
     */
    @Transactional
    public List<Media> uploadMultipleMedia(List<MultipartFile> files, String userId) throws IOException {
        if (files.size() > 4) {
            throw new IllegalArgumentException("Cannot upload more than 4 media files per post");
        }

        return files.stream()
                .map(file -> {
                    try {
                        return uploadMedia(file, userId);
                    } catch (IOException e) {
                        log.error("Failed to upload media file: {}", file.getOriginalFilename(), e);
                        throw new BusinessException("UPLOAD_FAILED",
                                "Failed to upload media: " + file.getOriginalFilename());
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Attach media to a post
     */
    @Transactional
    public void attachMediaToPost(Long postId, List<Long> mediaIds) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        List<Media> mediaList = mediaRepository.findAllById(mediaIds);

        if (mediaList.size() != mediaIds.size()) {
            throw new BusinessException("INVALID_MEDIA_IDS", "Some media IDs are invalid");
        }

        // Attach media to post
        for (Media media : mediaList) {
            media.setPost(post);
        }
        mediaRepository.saveAll(mediaList);

        // Update post media count
        post.setMediaCount(mediaList.size());
        postRepository.save(post);

        log.info("Attached {} media files to post ID: {}", mediaList.size(), postId);
    }

    /**
     * Get media by ID
     */
    @Transactional(readOnly = true)
    public Media getMediaById(Long mediaId) {
        return mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media", "id", mediaId));
    }

    /**
     * Get media by post ID
     */
    @Transactional(readOnly = true)
    public List<Media> getMediaByPostId(Long postId) {
        return mediaRepository.findByPostId(postId);
    }

    /**
     * Delete media
     */
    @Transactional
    public void deleteMedia(Long mediaId) {
        Media media = getMediaById(mediaId);

        // Delete physical file
        try {
            Path filePath = Paths.get(media.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("Deleted media file: {}", media.getFilePath());
        } catch (IOException e) {
            log.error("Failed to delete media file: {}", media.getFilePath(), e);
        }

        mediaRepository.delete(media);
    }

    /**
     * Map Media entity to MediaResponse DTO
     */
    public MediaResponse mapToResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .postId(media.getPost() != null ? media.getPost().getId() : null)
                .mediaType(media.getMediaType())
                .fileName(media.getFileName())
                .fileUrl(media.getFileUrl())
                .fileSize(media.getFileSize())
                .width(media.getWidth())
                .height(media.getHeight())
                .duration(media.getDuration())
                .thumbnailUrl(media.getThumbnailUrl())
                .createdAt(media.getCreatedAt())
                .build();
    }

    /**
     * Map list of Media to list of MediaResponse
     */
    public List<MediaResponse> mapToResponseList(List<Media> mediaList) {
        return mediaList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Private Helper Methods ====================

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Filename is invalid");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        List<String> allowedImages = Arrays.asList(allowedImageTypes.split(","));
        List<String> allowedVideos = Arrays.asList(allowedVideoTypes.split(","));

        boolean isImage = allowedImages.contains(extension);
        boolean isVideo = allowedVideos.contains(extension);

        if (!isImage && !isVideo) {
            throw new IllegalArgumentException("File type not allowed: " + extension);
        }

        // Check file size
        if (isImage && file.getSize() > maxImageSize) {
            throw new IllegalArgumentException("Image file size exceeds maximum allowed size of " +
                    (maxImageSize / 1024 / 1024) + "MB");
        }

        if (isVideo && file.getSize() > maxVideoSize) {
            throw new IllegalArgumentException("Video file size exceeds maximum allowed size of " +
                    (maxVideoSize / 1024 / 1024) + "MB");
        }
    }

    private Media.MediaType determineMediaType(MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename()).toLowerCase();

        if (extension.equals("gif")) {
            return Media.MediaType.GIF;
        }

        List<String> imageTypes = Arrays.asList(allowedImageTypes.split(","));
        if (imageTypes.contains(extension)) {
            return Media.MediaType.IMAGE;
        }

        return Media.MediaType.VIDEO;
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    private String generateUniqueFilename(String userId, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s.%s", userId, timestamp, uuid, extension);
    }

    private String buildFileUrl(Media.MediaType mediaType, String filename) {
        String basePath = contextPath.isEmpty() ? "" : contextPath;
        String mediaTypePath = mediaType == Media.MediaType.VIDEO ? "videos" : "images";
        return String.format("%s/api/v1/media/%s/%s", basePath, mediaTypePath, filename);
    }

    private void extractImageMetadata(Media media, File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if (image != null) {
                media.setWidth(image.getWidth());
                media.setHeight(image.getHeight());
            }
        } catch (IOException e) {
            log.warn("Failed to extract image metadata for: {}", imageFile.getName(), e);
        }
    }
}
