-- Phase 4 performance helpers (safe to re-run)
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS parse_status char(1) default '0';
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS parse_msg varchar(500) default null;
CREATE INDEX IF NOT EXISTS idx_biz_query_row_data_gin ON biz_query_row USING gin (row_data jsonb_path_ops);
CREATE INDEX IF NOT EXISTS idx_biz_query_row_qid_rowno ON biz_query_row(query_id, row_no);
COMMENT ON COLUMN biz_query.parse_status IS '0 ready 1 parsing 2 failed';
