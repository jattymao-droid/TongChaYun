-- P22: answer validity + query optional/mask fields
ALTER TABLE biz_survey_answer ADD COLUMN IF NOT EXISTS valid_flag char(1) DEFAULT '1';
ALTER TABLE biz_survey_answer ADD COLUMN IF NOT EXISTS remark varchar(500);
COMMENT ON COLUMN biz_survey_answer.valid_flag IS '是否有效答卷（1有效 0无效）';
COMMENT ON COLUMN biz_survey_answer.remark IS '答卷备注（如无效原因）';
CREATE INDEX IF NOT EXISTS idx_biz_survey_answer_valid ON biz_survey_answer(survey_id, valid_flag);

ALTER TABLE biz_query_field ADD COLUMN IF NOT EXISTS is_required char(1) DEFAULT '1';
ALTER TABLE biz_query_field ADD COLUMN IF NOT EXISTS mask_type varchar(20) DEFAULT 'none';
COMMENT ON COLUMN biz_query_field.is_required IS '查询条件是否必填（1必填 0选填）';
COMMENT ON COLUMN biz_query_field.mask_type IS '结果脱敏：none/phone/idcard/name/email';
UPDATE biz_query_field SET is_required = '1' WHERE is_required IS NULL;
UPDATE biz_query_field SET mask_type = 'none' WHERE mask_type IS NULL OR mask_type = '';
