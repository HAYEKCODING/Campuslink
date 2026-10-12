-- Run once on the existing MySQL database before deploying the new backend.
ALTER TABLE messages
    ADD COLUMN media_storage_key VARCHAR(120) NULL,
    ADD COLUMN media_original_name VARCHAR(255) NULL,
    ADD COLUMN media_content_type VARCHAR(120) NULL,
    ADD COLUMN media_size_bytes BIGINT NULL;

CREATE INDEX idx_message_match_media ON messages (match_id, media_content_type);
