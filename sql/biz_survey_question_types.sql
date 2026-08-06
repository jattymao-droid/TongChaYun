-- extend survey question types (idempotent)
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 10, '是非题', 'yesno', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '是/否'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'yesno');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 11, '数字', 'number', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '数字输入'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'number');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 12, 'NPS', 'nps', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '净推荐值 0-10'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'nps');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 13, '说明段落', 'section', 'biz_question_type', '', 'info', 'N', '0', 'admin', now(), '展示说明，不作答'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'section');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 14, '邮箱', 'email', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '邮箱地址'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'email');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 15, '日期时间', 'datetime', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '日期时间'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'datetime');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 16, '滑块', 'slider', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '滑块打分'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'slider');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 17, '图片单选', 'image_radio', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '带图片的单选'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'image_radio');
