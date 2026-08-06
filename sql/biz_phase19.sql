-- P19 client token for anti-repeat
ALTER TABLE biz_survey_answer ADD COLUMN IF NOT EXISTS client_token varchar(64);
COMMENT ON COLUMN biz_survey_answer.client_token IS '客户端防重标记（localStorage UUID）';
CREATE INDEX IF NOT EXISTS idx_biz_survey_answer_token ON biz_survey_answer(survey_id, client_token);
