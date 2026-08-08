-- TongChaYun production incremental patch
-- Baseline: 2026-08-08 release
-- Adds: P28/P29/P30 (ROADMAP P22/P23/P24)
-- P25: no DDL
-- Safe to re-run

BEGIN;

-- ========== biz_phase28_reach.sql ==========
-- P28 / ROADMAP P22: reach and schedule (publish_at, expire, remind, answer mail)

-- Survey
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS publish_at timestamp;
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS mail_notify char(1) DEFAULT '0';
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS mail_notify_to varchar(500) DEFAULT '';
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS remind_hours int4 DEFAULT 24;
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS remind_sent char(1) DEFAULT '0';
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS remind_mail char(1) DEFAULT '0';
COMMENT ON COLUMN biz_survey.publish_at IS 'scheduled publish time; auto status=1 when due';
COMMENT ON COLUMN biz_survey.mail_notify IS 'answer email notify 0 off 1 on';
COMMENT ON COLUMN biz_survey.mail_notify_to IS 'notify emails comma-separated; empty uses owner email';
COMMENT ON COLUMN biz_survey.remind_hours IS 'hours before end_time to remind; 0=off';
COMMENT ON COLUMN biz_survey.remind_sent IS 'deadline remind sent 0/1';
COMMENT ON COLUMN biz_survey.remind_mail IS 'also email on deadline remind 0/1';
CREATE INDEX IF NOT EXISTS idx_biz_survey_publish_at ON biz_survey(publish_at) WHERE publish_at IS NOT NULL AND status = '0';
CREATE INDEX IF NOT EXISTS idx_biz_survey_end_time ON biz_survey(end_time) WHERE end_time IS NOT NULL AND status = '1';

-- Query
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS start_time timestamp;
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS end_time timestamp;
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS publish_at timestamp;
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS remind_hours int4 DEFAULT 24;
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS remind_sent char(1) DEFAULT '0';
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS remind_mail char(1) DEFAULT '0';
COMMENT ON COLUMN biz_query.start_time IS 'open start time';
COMMENT ON COLUMN biz_query.end_time IS 'open end time';
COMMENT ON COLUMN biz_query.publish_at IS 'scheduled publish time';
COMMENT ON COLUMN biz_query.remind_hours IS 'hours before end_time to remind; 0=off';
COMMENT ON COLUMN biz_query.remind_sent IS 'deadline remind sent';
COMMENT ON COLUMN biz_query.remind_mail IS 'email on deadline remind';
CREATE INDEX IF NOT EXISTS idx_biz_query_publish_at ON biz_query(publish_at) WHERE publish_at IS NOT NULL AND status = '0';
CREATE INDEX IF NOT EXISTS idx_biz_query_end_time ON biz_query(end_time) WHERE end_time IS NOT NULL AND status = '1';

COMMENT ON COLUMN biz_survey.status IS '0 draft 1 published 2 offline 3 ended';
COMMENT ON COLUMN biz_query.status IS '0 draft 1 published 2 offline 3 ended';

ALTER TABLE biz_survey_notify ALTER COLUMN survey_id DROP NOT NULL;
COMMENT ON COLUMN biz_survey_notify.survey_id IS 'survey id; nullable for non-survey notifies';

-- dict: query ended status
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 214, 4, '已截止', '3', 'biz_query_status', '', 'danger', 'N', '0', 'admin', now(), 'P28 auto expire'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_query_status' AND dict_value = '3');

-- ========== biz_phase29_version.sql ==========
-- P29 / ROADMAP P23: versions, query co-admin, publish approval, project audit

CREATE TABLE IF NOT EXISTS biz_query_revision (
  rev_id bigserial PRIMARY KEY,
  query_id int8 NOT NULL,
  rev_no int4 NOT NULL,
  row_count int4 DEFAULT 0,
  fields_json text,
  create_by varchar(64) DEFAULT '',
  create_time timestamp DEFAULT now(),
  remark varchar(200) DEFAULT ''
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_query_revision_qn ON biz_query_revision(query_id, rev_no);
CREATE INDEX IF NOT EXISTS idx_biz_query_revision_qid ON biz_query_revision(query_id, rev_id DESC);
COMMENT ON TABLE biz_query_revision IS 'query result snapshot meta';

CREATE TABLE IF NOT EXISTS biz_query_revision_row (
  id bigserial PRIMARY KEY,
  rev_id int8 NOT NULL,
  row_no int4 DEFAULT 0,
  row_data jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX IF NOT EXISTS idx_biz_query_revision_row_rev ON biz_query_revision_row(rev_id);

CREATE TABLE IF NOT EXISTS biz_survey_revision (
  rev_id bigserial PRIMARY KEY,
  survey_id int8 NOT NULL,
  rev_no int4 NOT NULL,
  design_json text NOT NULL,
  theme_json text,
  create_by varchar(64) DEFAULT '',
  create_time timestamp DEFAULT now(),
  remark varchar(200) DEFAULT ''
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_survey_revision_sn ON biz_survey_revision(survey_id, rev_no);
CREATE INDEX IF NOT EXISTS idx_biz_survey_revision_sid ON biz_survey_revision(survey_id, rev_id DESC);
COMMENT ON TABLE biz_survey_revision IS 'survey design snapshot';

CREATE TABLE IF NOT EXISTS biz_query_admin (
  id bigserial PRIMARY KEY,
  query_id int8 NOT NULL,
  user_id int8 NOT NULL,
  create_by varchar(64) DEFAULT '',
  create_time timestamp DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_query_admin_qu ON biz_query_admin(query_id, user_id);
CREATE INDEX IF NOT EXISTS idx_biz_query_admin_user ON biz_query_admin(user_id);
CREATE INDEX IF NOT EXISTS idx_biz_query_admin_query ON biz_query_admin(query_id);
COMMENT ON TABLE biz_query_admin IS 'query co-admin';

CREATE TABLE IF NOT EXISTS biz_publish_request (
  request_id bigserial PRIMARY KEY,
  project_type varchar(16) NOT NULL,
  project_id int8 NOT NULL,
  project_name varchar(100) DEFAULT '',
  status char(1) DEFAULT '0',
  apply_by varchar(64) DEFAULT '',
  apply_user_id int8,
  apply_time timestamp DEFAULT now(),
  review_by varchar(64) DEFAULT '',
  review_user_id int8,
  review_time timestamp,
  review_remark varchar(500) DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_biz_publish_request_status ON biz_publish_request(status, apply_time DESC);
CREATE INDEX IF NOT EXISTS idx_biz_publish_request_proj ON biz_publish_request(project_type, project_id);
COMMENT ON COLUMN biz_publish_request.status IS '0 pending 1 approved 2 rejected';
COMMENT ON COLUMN biz_publish_request.project_type IS 'query or survey';

CREATE TABLE IF NOT EXISTS biz_project_audit (
  audit_id bigserial PRIMARY KEY,
  project_type varchar(16) NOT NULL,
  project_id int8 NOT NULL,
  action varchar(64) NOT NULL,
  detail varchar(500) DEFAULT '',
  oper_name varchar(64) DEFAULT '',
  oper_time timestamp DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_biz_project_audit_proj ON biz_project_audit(project_type, project_id, audit_id DESC);

-- site switch: publish approval (default off)
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'Biz publish approval', 'sys.biz.publishApprove', 'false', 'Y', 'admin', now(), 'P23 optional publish approval'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.biz.publishApprove');

-- ========== biz_phase30_risk.sql ==========
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

COMMIT;

SELECT 'biz_query_revision' AS t, count(*) FROM information_schema.tables WHERE table_name='biz_query_revision'
UNION ALL SELECT 'biz_survey_revision', count(*) FROM information_schema.tables WHERE table_name='biz_survey_revision'
UNION ALL SELECT 'biz_query_admin', count(*) FROM information_schema.tables WHERE table_name='biz_query_admin'
UNION ALL SELECT 'biz_publish_request', count(*) FROM information_schema.tables WHERE table_name='biz_publish_request'
UNION ALL SELECT 'biz_project_audit', count(*) FROM information_schema.tables WHERE table_name='biz_project_audit'
UNION ALL SELECT 'biz_project_blacklist', count(*) FROM information_schema.tables WHERE table_name='biz_project_blacklist'
UNION ALL SELECT 'biz_survey.publish_at', count(*) FROM information_schema.columns WHERE table_name='biz_survey' AND column_name='publish_at'
UNION ALL SELECT 'biz_access_log.channel_code', count(*) FROM information_schema.columns WHERE table_name='biz_access_log' AND column_name='channel_code';
