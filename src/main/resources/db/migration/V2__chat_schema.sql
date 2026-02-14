-- ============================================================
-- V2: Chat System Schema
-- WhatsApp-like real-time chat for social media platform
-- Execute manually against your MySQL database
-- ============================================================

-- 1. Conversations table (1:1 or group)
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('DIRECT', 'GROUP') NOT NULL DEFAULT 'DIRECT',
    group_name VARCHAR(200) DEFAULT NULL,
    group_avatar_url VARCHAR(500) DEFAULT NULL,
    created_by VARCHAR(255) NOT NULL COMMENT 'Keycloak user_id of creator',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_conv_created_by (created_by),
    INDEX idx_conv_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Chat conversations (1:1 or group)';

-- 2. Conversation participants
CREATE TABLE IF NOT EXISTS conversation_participant (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL COMMENT 'Keycloak user_id',
    role ENUM('OWNER', 'ADMIN', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP NULL DEFAULT NULL,
    is_muted BOOLEAN NOT NULL DEFAULT FALSE,
    last_read_at TIMESTAMP NULL DEFAULT NULL COMMENT 'Timestamp of last message the user has read',
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    UNIQUE KEY uk_conv_user (conversation_id, user_id),
    INDEX idx_cp_user_id (user_id),
    INDEX idx_cp_conv_id (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Participants in each conversation';

-- 3. Messages
CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id VARCHAR(255) NOT NULL COMMENT 'Keycloak user_id',
    content TEXT NULL COMMENT 'Message text (NULL for media-only)',
    type ENUM('TEXT', 'IMAGE', 'VIDEO', 'AUDIO', 'FILE', 'SYSTEM') NOT NULL DEFAULT 'TEXT',
    reply_to_id BIGINT NULL COMMENT 'If this is a reply, reference parent message',
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to_id) REFERENCES message(id) ON DELETE SET NULL,
    INDEX idx_msg_conv_id (conversation_id),
    INDEX idx_msg_sender_id (sender_id),
    INDEX idx_msg_created_at (created_at),
    INDEX idx_msg_conv_created (conversation_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Individual chat messages';

-- 4. Message delivery/read status per recipient
CREATE TABLE IF NOT EXISTS message_status (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id VARCHAR(255) NOT NULL COMMENT 'Recipient user_id',
    status ENUM('SENT', 'DELIVERED', 'READ') NOT NULL DEFAULT 'SENT',
    delivered_at TIMESTAMP NULL DEFAULT NULL,
    read_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE,
    UNIQUE KEY uk_msg_user (message_id, user_id),
    INDEX idx_ms_user_id (user_id),
    INDEX idx_ms_status (status),
    INDEX idx_ms_msg_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Per-user delivery and read status for each message';

-- 5. Message attachments (images, videos, files)
CREATE TABLE IF NOT EXISTS message_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    message_id BIGINT NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(500) DEFAULT NULL,
    file_type VARCHAR(100) DEFAULT NULL COMMENT 'MIME type',
    file_size BIGINT DEFAULT 0 COMMENT 'Size in bytes',
    thumbnail_url VARCHAR(1000) DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE,
    INDEX idx_ma_msg_id (message_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='File attachments for messages';

-- 6. User presence tracking (optional DB backup, primary is in-memory)
CREATE TABLE IF NOT EXISTS user_presence (
    user_id VARCHAR(255) PRIMARY KEY COMMENT 'Keycloak user_id',
    is_online BOOLEAN NOT NULL DEFAULT FALSE,
    last_seen_at TIMESTAMP NULL DEFAULT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_up_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='User online/offline presence (DB backup for in-memory registry)';
