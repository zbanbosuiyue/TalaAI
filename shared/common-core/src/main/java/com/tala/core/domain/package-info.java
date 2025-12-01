/**
 * Core Domain Package - Unified Attachment Protocol
 * 
 * <h2>Overview</h2>
 * This package provides a unified attachment management protocol following industry best practices.
 * All entities that need to store references to files or media should use this protocol.
 * 
 * <h2>Architecture</h2>
 * 
 * <h3>1. AttachmentSupport Interface</h3>
 * <p>Marker interface for entities that support attachments. Provides:</p>
 * <ul>
 *   <li>getAttachmentIds() / setAttachmentIds() - CRUD operations</li>
 *   <li>getAttachmentSourceType() - Identifies source (FILE_SERVICE, MEDIA_SERVICE, MIXED)</li>
 *   <li>hasAttachments() - Convenience check</li>
 *   <li>getAttachmentCount() - Get count</li>
 * </ul>
 * 
 * <h3>2. BaseAttachmentEntity Abstract Class</h3>
 * <p>Extends BaseEntity and implements AttachmentSupport.</p>
 * <p>Provides standard JSONB column mapping for PostgreSQL.</p>
 * <p>Usage: Entities should extend this class when they need attachment support.</p>
 * 
 * <h3>3. BaseEntity Abstract Class</h3>
 * <p>Foundation class providing:</p>
 * <ul>
 *   <li>Snowflake ID generation</li>
 *   <li>Audit timestamps (createdAt, updatedAt)</li>
 *   <li>Soft delete support (deletedAt)</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Pattern 1: Extend BaseAttachmentEntity (Recommended)</h3>
 * <pre>{@code
 * @Entity
 * @Table(name = "chat_messages")
 * public class ChatMessage extends BaseAttachmentEntity {
 *     // Entity fields...
 *     // Automatically has: id, timestamps, attachmentIds
 * }
 * }</pre>
 * 
 * <h3>Pattern 2: Implement AttachmentSupport Interface</h3>
 * <p>Use when entity already extends another base class:</p>
 * <pre>{@code
 * @Entity
 * @Table(name = "daily_summaries")
 * public class DailySummary extends BaseEntity implements AttachmentSupport {
 *     
 *     @Type(JsonBinaryType.class)
 *     @Column(name = "candidate_media_ids", columnDefinition = "jsonb")
 *     private List<Long> attachmentIds = new ArrayList<>();
 *     
 *     @Override
 *     public AttachmentSourceType getAttachmentSourceType() {
 *         return AttachmentSourceType.MEDIA_SERVICE;
 *     }
 * }
 * }</pre>
 * 
 * <h3>Pattern 3: Override Column Name (Legacy Compatibility)</h3>
 * <pre>{@code
 * @Entity
 * public class OriginalEvent extends BaseAttachmentEntity {
 *     
 *     // Override to use legacy column name
 *     @Column(name = "attachment_file_ids", columnDefinition = "jsonb")
 *     private List<Long> attachmentIds = new ArrayList<>();
 * }
 * }</pre>
 * 
 * <h2>Database Schema</h2>
 * 
 * <h3>Standard Column Definition</h3>
 * <pre>{@code
 * ALTER TABLE your_table
 *     ADD COLUMN attachment_ids JSONB NOT NULL DEFAULT '[]';
 * 
 * -- Optional: Index for querying entities with attachments
 * CREATE INDEX idx_has_attachments 
 *     ON your_table((jsonb_array_length(attachment_ids) > 0));
 * }</pre>
 * 
 * <h2>Service Layer Integration</h2>
 * 
 * <h3>AttachmentResolver Interface</h3>
 * <p>Services should implement this interface to resolve attachment IDs to AttachmentRef DTOs:</p>
 * <pre>{@code
 * @Service
 * public class FileAttachmentResolver implements AttachmentResolver {
 *     
 *     @Override
 *     public List<AttachmentRef> resolve(List<Long> ids, AttachmentSourceType type) {
 *         // Call file-service or media-service to get metadata
 *         // Return list of AttachmentRef
 *     }
 *     
 *     @Override
 *     public boolean supports(AttachmentSourceType type) {
 *         return type == AttachmentSourceType.FILE_SERVICE;
 *     }
 * }
 * }</pre>
 * 
 * <h2>DTO Layer</h2>
 * 
 * <h3>AttachmentRef DTO</h3>
 * <p>Unified DTO for API responses containing attachment metadata:</p>
 * <ul>
 *   <li>source - FILE_SERVICE, MEDIA_SERVICE, EXTERNAL_URL</li>
 *   <li>resourceId - ID in source system</li>
 *   <li>url - Direct access URL</li>
 *   <li>thumbnailUrl - Thumbnail URL (optional)</li>
 *   <li>mediaType - MIME type</li>
 *   <li>label - Human-readable name</li>
 *   <li>sizeBytes - File size</li>
 *   <li>width/height/durationSeconds - Media metadata</li>
 * </ul>
 * 
 * <h2>Industry Best Practices</h2>
 * 
 * <h3>1. Separation of Concerns</h3>
 * <ul>
 *   <li>Entity stores only IDs (lightweight)</li>
 *   <li>File metadata stored in file-service/media-service</li>
 *   <li>Resolver service handles ID-to-metadata conversion</li>
 * </ul>
 * 
 * <h3>2. Flexibility</h3>
 * <ul>
 *   <li>JSONB allows variable-length arrays without schema changes</li>
 *   <li>Supports GIN indexing for efficient queries</li>
 *   <li>No foreign key constraints (microservices independence)</li>
 * </ul>
 * 
 * <h3>3. Consistency</h3>
 * <ul>
 *   <li>All entities use same interface/base class</li>
 *   <li>Polymorphic handling via AttachmentSupport interface</li>
 *   <li>Unified DTO for all attachment types</li>
 * </ul>
 * 
 * <h3>4. Extensibility</h3>
 * <ul>
 *   <li>Easy to add new attachment sources</li>
 *   <li>Strategy pattern for different resolvers</li>
 *   <li>Backward compatible with legacy schemas</li>
 * </ul>
 * 
 * <h2>Migration Guide</h2>
 * 
 * <h3>For New Entities</h3>
 * <ol>
 *   <li>Extend BaseAttachmentEntity</li>
 *   <li>Add migration: ALTER TABLE ADD COLUMN attachment_ids JSONB DEFAULT '[]'</li>
 *   <li>Use AttachmentRef in response DTOs</li>
 *   <li>Implement AttachmentResolver if needed</li>
 * </ol>
 * 
 * <h3>For Existing Entities</h3>
 * <ol>
 *   <li>If column exists: Override column name in entity</li>
 *   <li>If no column: Add migration for attachment_ids column</li>
 *   <li>Change extends BaseEntity to extends BaseAttachmentEntity</li>
 *   <li>Update service layer to use AttachmentResolver</li>
 * </ol>
 * 
 * @author Tala Backend Team
 * @version 1.0
 * @since 2024-12-01
 */
package com.tala.core.domain;
