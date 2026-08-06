-- P21 server draft + cascade_select dict
CREATE TABLE IF NOT EXISTS biz_survey_draft (
    draft_id      bigserial PRIMARY KEY,
    survey_id     int8 NOT NULL,
    client_token  varchar(64) NOT NULL,
    draft_json    text NOT NULL,
    update_time   timestamp DEFAULT now()
);
COMMENT ON TABLE biz_survey_draft IS 'open survey draft by client_token';
CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_survey_draft_token ON biz_survey_draft (survey_id, client_token);

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 19, '级联选择', 'cascade_select', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), 'cascade 2-level'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'cascade_select'
);
