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
