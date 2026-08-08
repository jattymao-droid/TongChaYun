-- Snapshot question title/type on answer items (survive question redesign)
ALTER TABLE biz_survey_answer_item ADD COLUMN IF NOT EXISTS question_title varchar(500);
ALTER TABLE biz_survey_answer_item ADD COLUMN IF NOT EXISTS q_type varchar(32);
COMMENT ON COLUMN biz_survey_answer_item.question_title IS '提交时题目快照';
COMMENT ON COLUMN biz_survey_answer_item.q_type IS '提交时题型快照';
