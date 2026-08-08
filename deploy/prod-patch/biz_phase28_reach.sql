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
