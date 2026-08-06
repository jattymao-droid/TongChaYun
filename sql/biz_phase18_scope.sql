-- P18-4 dept data scope for survey/query projects
ALTER TABLE biz_survey ADD COLUMN IF NOT EXISTS dept_id int8;
ALTER TABLE biz_query ADD COLUMN IF NOT EXISTS dept_id int8;

COMMENT ON COLUMN biz_survey.dept_id IS '创建人部门，用于数据权限';
COMMENT ON COLUMN biz_query.dept_id IS '创建人部门，用于数据权限';

UPDATE biz_survey s SET dept_id = u.dept_id
FROM sys_user u
WHERE s.create_user_id = u.user_id AND s.dept_id IS NULL;

UPDATE biz_query q SET dept_id = u.dept_id
FROM sys_user u
WHERE q.create_user_id = u.user_id AND q.dept_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_biz_survey_dept ON biz_survey(dept_id);
CREATE INDEX IF NOT EXISTS idx_biz_query_dept ON biz_query(dept_id);
