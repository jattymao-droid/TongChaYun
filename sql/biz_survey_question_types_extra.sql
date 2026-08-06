-- extend survey question types (idempotent)
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 18, '图片多选', 'image_checkbox', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '带图片的多选'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'image_checkbox');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 19, '量表', 'likert', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '李克特五点量表'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'likert');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 20, '时间', 'time', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '时间 HH:mm'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'time');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 21, '网址', 'url', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '网址 URL'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'url');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 22, '身份证', 'idcard', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '18位身份证号'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'idcard');
