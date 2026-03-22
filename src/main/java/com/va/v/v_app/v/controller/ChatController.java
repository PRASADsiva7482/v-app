package com.va.v.v_app.v.controller;

import com.va.v.v_app.config.security.SecurityContextUtil;
import com.va.v.v_app.v.dto.request.SendMessageRequest;
import com.va.v.v_app.v.dto.request.StartConversationRequest;
import com.va.v.v_app.v.dto.response.ConversationResponse;
import com.va.v.v_app.v.dto.response.MessageResponse;
import com.va.v.v_app.v.model.Media;
import com.va.v.v_app.v.service.ChatMessageService;
import com.va.v.v_app.v.service.ConversationService;
import com.va.v.v_app.v.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * REST controller for Chat.
 *
 * Purpose | API | Technology
 * ───────────────────────┼────────────────────────────────────────────┼───────────
 * Initial chat list │ GET /api/v1/chat/conversations │ REST
 * Start conversation │ POST /api/v1/chat/conversations │ REST
 * Message history │ GET /api/v1/chat/messages/{convId} │ REST
 * Edit message │ PUT /api/v1/chat/messages/{msgId} │ REST
 * Delete message │ DELETE /api/v1/chat/messages/{msgId} │ REST
 * Upload chat media │ POST /api/v1/chat/media/upload │ REST
 * Serve chat files │ GET /api/v1/chat/media/files/{filename} │ REST
 * Send with media │ POST /api/v1/chat/messages/send │ REST
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final ChatMessageService chatMessageService;
    private final MediaService mediaService;

    @Value("${feature.media.storage.base-path}")
    private String baseStoragePath;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    // ─── Conversations ───

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getConversations() {
        String userId = SecurityContextUtil.getCurrentUsername();
        return ResponseEntity.ok(conversationService.getConversations(userId));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable Long conversationId) {
        String userId = SecurityContextUtil.getCurrentUsername();
        return ResponseEntity.ok(conversationService.getConversation(conversationId, userId));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationResponse> startConversation(
            @RequestBody StartConversationRequest request) {
        String userId = SecurityContextUtil.getCurrentUsername();
        // Support both DIRECT and GROUP via the type field
        if ("GROUP".equalsIgnoreCase(request.getType()) && request.getParticipantIds() != null) {
            return ResponseEntity.ok(conversationService.createGroupConversation(userId, request));
        }
        return ResponseEntity.ok(conversationService.startDirectConversation(userId, request));
    }

    @PostMapping("/conversations/group")
    public ResponseEntity<ConversationResponse> createGroupChat(
            @RequestBody StartConversationRequest request) {
        String userId = SecurityContextUtil.getCurrentUsername();
        return ResponseEntity.ok(conversationService.createGroupConversation(userId, request));
    }

    @PostMapping("/conversations/{conversationId}/members")
    public ResponseEntity<ConversationResponse> addGroupMember(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> body) {
        String userId = SecurityContextUtil.getCurrentUsername();
        String newMemberId = body.get("userId");
        return ResponseEntity.ok(conversationService.addGroupMember(conversationId, userId, newMemberId));
    }

    @DeleteMapping("/conversations/{conversationId}/members/{memberId}")
    public ResponseEntity<Void> removeGroupMember(
            @PathVariable Long conversationId,
            @PathVariable String memberId) {
        String userId = SecurityContextUtil.getCurrentUsername();
        conversationService.removeGroupMember(conversationId, userId, memberId);
        return ResponseEntity.noContent().build();
    }

    // ─── Messages ───

    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<Page<MessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        String userId = SecurityContextUtil.getCurrentUsername();
        return ResponseEntity.ok(chatMessageService.getMessages(conversationId, userId, page, size));
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent) {
        String userId = SecurityContextUtil.getCurrentUsername();
        return ResponseEntity.ok(chatMessageService.editMessage(messageId, userId, newContent));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        String userId = SecurityContextUtil.getCurrentUsername();
        chatMessageService.deleteMessage(messageId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/messages/send")
    public ResponseEntity<MessageResponse> sendMessage(
            @RequestBody SendMessageRequest request) {
        String userId = SecurityContextUtil.getCurrentUsername();
        return ResponseEntity.ok(chatMessageService.sendMessage(userId, request));
    }

    // ─── Chat Media Upload & Serving ───

    /**
     * Upload media for a chat message.
     * Supports ALL file types: images, videos, audio, documents.
     *
     * - Images/videos → delegated to MediaService (stored in images/ or videos/)
     * - Audio/documents → stored in chat-files/ directory
     *
     * Returns metadata that the client includes in the SendMessageRequest.
     */
    @PostMapping(value = "/media/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadChatMedia(
            @RequestParam("file") MultipartFile file) throws IOException {

        String userId = SecurityContextUtil.getCurrentUsername();
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        Map<String, Object> response = new HashMap<>();

        if (contentType.startsWith("image/") || contentType.startsWith("video/")) {
            // Use MediaService for images/videos (handles metadata extraction, thumbnails)
            Media media = mediaService.uploadMedia(file, userId);
            response.put("fileUrl", media.getFileUrl());
            response.put("fileName", media.getFileName());
            response.put("fileType", contentType);
            response.put("fileSize", media.getFileSize());
            response.put("thumbnailUrl", media.getThumbnailUrl());
            response.put("mediaId", media.getId());
        } else {
            // Audio, documents, and other files → store in chat-files directory
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(userId, extension);

            Path chatFilesDir = Paths.get(baseStoragePath, "chat-files");
            Files.createDirectories(chatFilesDir);
            Path targetPath = chatFilesDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String basePath = contextPath.isEmpty() ? "" : contextPath;
            String fileUrl = String.format("%s/api/v1/chat/media/files/%s", basePath, uniqueFilename);

            response.put("fileUrl", fileUrl);
            response.put("fileName", originalFilename);
            response.put("fileType", contentType);
            response.put("fileSize", file.getSize());
            response.put("thumbnailUrl", null);
            response.put("mediaId", null);

            log.info("Chat file uploaded: {} (type: {}) for user: {}", uniqueFilename, contentType, userId);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Serve chat files (audio, documents, etc.).
     * Images/videos are served via MediaController (/api/v1/media/images|videos/).
     */
    @GetMapping("/media/files/{filename:.+}")
    public ResponseEntity<Resource> serveChatFile(@PathVariable String filename, @RequestHeader HttpHeaders headers) {
        try {
            Path filePath = Paths.get(baseStoragePath, "chat-files").resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri().normalize());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineChatFileContentType(filename);

                return ResponseEntity.status(HttpStatus.OK)
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\""
                                        + (resource.getFilename() != null ? resource.getFilename() : filename) + "\"")
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .body(resource);
            } else {
                log.error("Chat file not found: {}", filename);
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            log.error("Error serving chat file: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    // ─── Helpers ───

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains("."))
            return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private String generateUniqueFilename(String userId, String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return String.format("%s_%s_%s.%s", userId, timestamp, uuid, extension);
    }

    private String determineChatFileContentType(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "m4a" -> "audio/mp4";
            case "aac" -> "audio/aac";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }
}
