-- P30 / ROADMAP P24: risk blacklist, access log channel, funnel support

CREATE TABLE IF NOT EXISTS biz_project_blacklist (
  id bigserial PRIMARY KEY,
  target_type varchar(16) NOT NULL,
  target_id int8 NOT NULL,
  kind varchar(16) NOT NULL,
  value varchar(128) NOT NULL,
  reason varchar(200) DEFAULT '',
  create_by varchar(64) DEFAULT '',
  create_time timestamp DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_project_blacklist
  ON biz_project_blacklist(target_type, target_id, kind, value);
CREATE INDEX IF NOT EXISTS idx_biz_project_blacklist_tid
  ON biz_project_blacklist(target_type, target_id);
COMMENT ON TABLE biz_project_blacklist IS 'project blacklist ip/device';
COMMENT ON COLUMN biz_project_blacklist.kind IS 'ip or device';
COMMENT ON COLUMN biz_project_blacklist.target_type IS 'survey or query';

ALTER TABLE biz_access_log ADD COLUMN IF NOT EXISTS channel_code varchar(64);
COMMENT ON COLUMN biz_access_log.channel_code IS 'optional channel for funnel/UV';
CREATE INDEX IF NOT EXISTS idx_biz_access_log_channel
  ON biz_access_log(target_type, target_id, action, channel_code);
