-- Rename brand: 明查通 -> 通查云
UPDATE sys_config SET config_value = '通查云', remark = '站点显示名称'
WHERE config_key = 'sys.info.title' AND config_value = '明查通';

UPDATE sys_config SET config_value = '? 通查云 · 查询与问卷平台'
WHERE config_key = 'sys.info.copyright' AND config_value LIKE '%明查通%';

UPDATE sys_config SET config_value = REPLACE(config_value, '明查通', '通查云')
WHERE config_value LIKE '%明查通%';
