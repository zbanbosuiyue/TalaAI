package com.tala.ai.domain;

import com.tala.core.domain.BaseAttachmentEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Chat Message Entity
 * 
 * Records all chat interactions between user and AI assistant.
 * Extends BaseAttachmentEntity for id, timestamps, soft delete, and attachment support.
 * 
 * Supports multimodal AI interactions with images, audio, and documents.
 * 
 * @author Tala Team
 */
@Entity
@Table(name = "chat_messages", schema = "ai", indexes = {
        @Index(name = "idx_profile_id", columnList = "profile_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_deleted_at", columnList = "deleted_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage extends BaseAttachmentEntity {

    /**
     * Baby profile ID
     */
    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    /**
     * User ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Message role: USER, ASSISTANT, SYSTEM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    /**
     * Message content
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Message type: TEXT, EVENT, SUGGESTION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 20)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    /**
     * Metadata (JSON format) - stores raw AI response for auditing
     */
    @Column(name = "metadata", columnDefinition = "JSONB")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String metadata;

    /**
     * Extracted records (JSON format) - for frontend display
     */
    @Column(name = "extracted_records_json", columnDefinition = "JSONB")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String extractedRecordsJson;

    /**
     * AI thinking process (JSON format) - stores AI reasoning details
     * Only for ASSISTANT role messages
     */
    @Column(name = "thinking_process", columnDefinition = "JSONB")
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    private String thinkingProcess;

    /**
     * Interaction type from classification
     */
    @Column(name = "interaction_type", length = 50)
    private String interactionType;

    /**
     * Confidence score (0.0 - 1.0)
     */
    @Column(name = "confidence")
    private Double confidence;

    /**
     * Message role enum
     */
    public enum MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }

    /**
     * Message type enum
     */
    public enum MessageType {
        TEXT,
        EVENT,
        SUGGESTION
    }
}
