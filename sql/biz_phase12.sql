-- Phase 12: query search count + atomic counters
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS search_count int8 DEFAULT 0;
COMMENT ON COLUMN biz_query.search_count IS 'public search count';
