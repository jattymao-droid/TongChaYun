-- Rename brand: ����ͨ -> ͨ����
UPDATE sys_config SET config_value = 'ͨ����', remark = 'վ����ʾ����'
WHERE config_key = 'sys.info.title' AND config_value = '����ͨ';

UPDATE sys_config SET config_value = '? ͨ���� �� ��ѯ���ʾ�ƽ̨'
WHERE config_key = 'sys.info.copyright' AND config_value LIKE '%����ͨ%';

UPDATE sys_config SET config_value = REPLACE(config_value, '����ͨ', 'ͨ����')
WHERE config_value LIKE '%����ͨ%';
