-- =============================================================================
-- Migration V36: Add YouTube Video & Media Fields to Taxoryn Learn Contents
-- =============================================================================

ALTER TABLE contents ADD COLUMN IF NOT EXISTS youtube_video_id VARCHAR(64);
ALTER TABLE contents ADD COLUMN IF NOT EXISTS video_duration_seconds INTEGER;

CREATE INDEX IF NOT EXISTS idx_contents_youtube_video_id ON contents(youtube_video_id);
