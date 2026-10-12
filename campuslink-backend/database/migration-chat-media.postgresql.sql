BEGIN;

ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS media_storage_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS media_original_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS media_content_type VARCHAR(120),
    ADD COLUMN IF NOT EXISTS media_size_bytes BIGINT;

CREATE INDEX IF NOT EXISTS idx_message_match_media
    ON messages (match_id, media_content_type);

COMMIT;
