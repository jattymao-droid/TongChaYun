-- P18 operational features
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS daily_limit int4 DEFAULT 0;
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS need_captcha char(1) DEFAULT '0';
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS webhook_secret varchar(128);

COMMENT ON COLUMN biz_survey.daily_limit IS '每日答卷上限，0不限';
COMMENT ON COLUMN biz_survey.need_captcha IS '公开提交是否需要验证码 0否 1是';
COMMENT ON COLUMN biz_survey.webhook_secret IS 'Webhook HMAC 签名密钥';

ALTER TABLE biz_survey_answer ADD COLUMN IF NOT EXISTS channel_code varchar(64);
COMMENT ON COLUMN biz_survey_answer.channel_code IS '渠道码';

CREATE INDEX IF NOT EXISTS idx_biz_survey_answer_channel ON biz_survey_answer(survey_id, channel_code);
CREATE INDEX IF NOT EXISTS idx_biz_survey_answer_day ON biz_survey_answer(survey_id, submit_time);
