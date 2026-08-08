-- page_break question type for manual multi-page fill (idempotent)
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 30, E'\u5206\u9875\u7b26', 'page_break', 'biz_question_type', '', 'info', 'N', '0', 'admin', now(), E'\u624b\u52a8\u5206\u9875\uff0c\u4e0d\u4f5c\u7b54'
WHERE NOT EXISTS (SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'page_break');
