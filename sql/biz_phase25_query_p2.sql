-- Phase 25: 1:N multi-match mode + query audit detail + access log index
ALTER TABLE biz_query_relation ADD COLUMN IF NOT EXISTS multi_match varchar(20) DEFAULT 'EXPAND';
COMMENT ON COLUMN biz_query_relation.multi_match IS '1:N strategy: EXPAND | FIRST | LAST | CONCAT';

ALTER TABLE biz_access_log ADD COLUMN IF NOT EXISTS detail_json text;
COMMENT ON COLUMN biz_access_log.detail_json IS 'masked params / hit count for audit';

CREATE INDEX IF NOT EXISTS idx_biz_access_log_target_action
  ON biz_access_log(target_type, target_id, action, create_time DESC);

CREATE INDEX IF NOT EXISTS idx_biz_query_ds_row_ds_rowno
  ON biz_query_dataset_row(dataset_id, row_no);
