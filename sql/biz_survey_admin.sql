-- Survey co-admins (idempotent)
CREATE TABLE IF NOT EXISTS biz_survey_admin (
  id           bigserial PRIMARY KEY,
  survey_id    int8 NOT NULL,
  user_id      int8 NOT NULL,
  create_by    varchar(64) default '',
  create_time  timestamp default now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_biz_survey_admin_su
  ON biz_survey_admin(survey_id, user_id);
CREATE INDEX IF NOT EXISTS idx_biz_survey_admin_user
  ON biz_survey_admin(user_id);
CREATE INDEX IF NOT EXISTS idx_biz_survey_admin_survey
  ON biz_survey_admin(survey_id);

COMMENT ON TABLE biz_survey_admin IS '问卷协作管理员';
