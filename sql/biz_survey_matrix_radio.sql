-- matrix_radio question type
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 18, '矩阵单选', 'matrix_radio', 'biz_question_type', '', 'default', 'N', '0', 'admin', now(), '矩阵量表'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_dict_data WHERE dict_type = 'biz_question_type' AND dict_value = 'matrix_radio'
);
