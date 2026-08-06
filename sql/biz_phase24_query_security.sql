-- Phase 24: open query captcha + daily IP limit
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS need_captcha char(1) DEFAULT '0';
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS daily_limit int4 DEFAULT 0;
COMMENT ON COLUMN biz_query.need_captcha IS '1 require captcha on open search';
COMMENT ON COLUMN biz_query.daily_limit IS 'max open searches per IP per day; 0 unlimited';
