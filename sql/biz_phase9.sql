-- Phase 9: survey webhook + query field distribution support
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS webhook_url varchar(500) default null;
COMMENT ON COLUMN biz_survey.webhook_url IS '答卷提交 Webhook 回调地址';
