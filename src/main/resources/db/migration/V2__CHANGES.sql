-- Safe ALTER migration: rename created_at -> occurred_at and add published_at
-- This preserves existing data and avoids recreating the table.
ALTER TABLE event_store RENAME COLUMN created_at TO occurred_at;

ALTER TABLE event_store ADD COLUMN IF NOT EXISTS published_at timestamp with time zone NULL;
