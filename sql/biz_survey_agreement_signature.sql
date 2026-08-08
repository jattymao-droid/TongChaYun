-- agreement + signature question types (idempotent)
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 31, E'\u534f\u8bae\u540c\u610f', 'agreement', 'biz_question_type', '', 'warning', 'N', '0', 'admin', now(), E'\u5bcc\u6587\u672c\u534f\u8bae+\u52fe\u9009\u540c\u610f'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'agreement');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 32, E'\u624b\u5199\u7b7e\u540d', 'signature', 'biz_question_type', '', 'success', 'N', '0', 'admin', now(), E'\u7ebf\u4e0a\u624b\u5199\u7b7e\u540d\uff0c\u53ef\u7ed1\u5b9a\u534f\u8bae'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'signature');
