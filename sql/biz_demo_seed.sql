-- P19 demo seed (idempotent by public_code)
-- Query: q6jjyg79 / Survey: 97vw7fqf

-- ===== Query demo =====
DO $$
DECLARE
  qid bigint;
BEGIN
  SELECT query_id INTO qid FROM biz_query WHERE public_code = 'q6jjyg79' LIMIT 1;
  IF qid IS NULL THEN
    INSERT INTO biz_query (
      query_name, query_desc, public_code, status, row_count, view_count, search_count,
      parse_status, create_user_id, create_by, create_time, dept_id
    ) VALUES (
      '成绩查询（演示）', '演示用成绩查询，学号/姓名', 'q6jjyg79', '1', 5, 0, 0,
      '0', 1, 'admin', now(), (SELECT dept_id FROM sys_user WHERE user_id = 1)
    ) RETURNING query_id INTO qid;

    INSERT INTO biz_query_page (query_id, title, subtitle, theme_color, result_tips)
    VALUES (qid, '成绩查询', '请输入学号或姓名', '#1677ff', '未查询到相关成绩');

    INSERT INTO biz_query_field (query_id, field_key, field_name, field_label, data_type, is_query, query_type, html_type, is_list, is_sortable, sort, width) VALUES
      (qid, 'student_no', 'student_no', '学号', 'string', '1', 'EQ', 'input', '1', '0', 1, 120),
      (qid, 'name', 'name', '姓名', 'string', '1', 'LIKE', 'input', '1', '0', 2, 100),
      (qid, 'subject', 'subject', '科目', 'string', '0', 'EQ', 'input', '1', '0', 3, 100),
      (qid, 'score', 'score', '成绩', 'number', '0', 'EQ', 'input', '1', '1', 4, 80);

    INSERT INTO biz_query_row (query_id, row_no, row_data) VALUES
      (qid, 1, '{"student_no":"2026001","name":"张三","subject":"语文","score":"92"}'::jsonb),
      (qid, 2, '{"student_no":"2026002","name":"李四","subject":"数学","score":"88"}'::jsonb),
      (qid, 3, '{"student_no":"2026003","name":"王五","subject":"英语","score":"95"}'::jsonb),
      (qid, 4, '{"student_no":"2026001","name":"张三","subject":"数学","score":"85"}'::jsonb),
      (qid, 5, '{"student_no":"2026004","name":"赵六","subject":"语文","score":"78"}'::jsonb);
  ELSE
    UPDATE biz_query SET status = '1', query_name = COALESCE(NULLIF(query_name,''), '成绩查询（演示）'), update_time = now()
    WHERE query_id = qid;
  END IF;
END $$;

-- ===== Survey demo =====
DO $$
DECLARE
  sid bigint;
BEGIN
  SELECT survey_id INTO sid FROM biz_survey WHERE public_code = '97vw7fqf' LIMIT 1;
  IF sid IS NULL THEN
    INSERT INTO biz_survey (
      survey_name, survey_desc, public_code, status, max_answers, allow_multi,
      theme_json, view_count, answer_count, create_user_id, create_by, create_time,
      daily_limit, need_captcha, dept_id
    ) VALUES (
      '满意度调研（演示）', '演示问卷：满意度 + 是否推荐', '97vw7fqf', '1', 0, '1',
      '{"color":"#1677ff"}', 0, 0, 1, 'admin', now(),
      0, '0', (SELECT dept_id FROM sys_user WHERE user_id = 1)
    ) RETURNING survey_id INTO sid;

    INSERT INTO biz_survey_question (survey_id, q_type, title, required, options_json, props_json, sort) VALUES
      (sid, 'radio', '整体满意度', '1',
       '[{"label":"非常满意","value":"5"},{"label":"满意","value":"4"},{"label":"一般","value":"3"},{"label":"不满意","value":"2"}]',
       NULL, 0),
      (sid, 'yesno', '是否愿意推荐给朋友？', '1',
       '[{"label":"是","value":"1"},{"label":"否","value":"0"}]',
       NULL, 1),
      (sid, 'textarea', '其他建议', '0', NULL, '{"maxLength":300}', 2);
  ELSE
    UPDATE biz_survey SET status = '1', update_time = now() WHERE survey_id = sid;
    IF NOT EXISTS (SELECT 1 FROM biz_survey_question WHERE survey_id = sid AND q_type IN ('radio','select','image_radio')) THEN
      INSERT INTO biz_survey_question (survey_id, q_type, title, required, options_json, props_json, sort) VALUES
        (sid, 'radio', '整体满意度', '1',
         '[{"label":"非常满意","value":"5"},{"label":"满意","value":"4"},{"label":"一般","value":"3"},{"label":"不满意","value":"2"}]',
         NULL, 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM biz_survey_question WHERE survey_id = sid AND q_type = 'yesno') THEN
      INSERT INTO biz_survey_question (survey_id, q_type, title, required, options_json, props_json, sort) VALUES
        (sid, 'yesno', '是否愿意推荐给朋友？', '1',
         '[{"label":"是","value":"1"},{"label":"否","value":"0"}]',
         NULL, 1);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM biz_survey_question WHERE survey_id = sid AND q_type = 'textarea') THEN
      INSERT INTO biz_survey_question (survey_id, q_type, title, required, options_json, props_json, sort) VALUES
        (sid, 'textarea', '其他建议', '0', NULL, '{"maxLength":300}', 2);
    END IF;
  END IF;
END $$;
