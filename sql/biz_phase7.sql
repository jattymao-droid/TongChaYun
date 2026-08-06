-- Phase 7: file question type dict
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 228, 9, '附件上传', 'file', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), ''
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'file');
SELECT setval(pg_get_serial_sequence('sys_dict_data', 'dict_code'), greatest(coalesce((select max(dict_code) from sys_dict_data), 1), 228));
