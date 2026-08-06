-- Basic settings: site info / mail + menus
-- menu 118 under system (1)

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '系统名称', 'sys.info.title', '通查云', 'Y', 'admin', now(), '站点显示名称'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.info.title');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '系统Logo', 'sys.info.logo', '/logo.svg', 'Y', 'admin', now(), 'Logo path or URL'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.info.logo');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '页脚版权', 'sys.info.copyright', '© 通查云 · 查询与问卷平台', 'Y', 'admin', now(), 'footer copyright'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.info.copyright');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '备案号', 'sys.info.icp', '', 'Y', 'admin', now(), 'ICP number'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.info.icp');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '显示页脚', 'sys.info.footerVisible', 'true', 'Y', 'admin', now(), 'show admin footer'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.info.footerVisible');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '邮件服务开关', 'sys.mail.enabled', 'false', 'Y', 'admin', now(), 'enable SMTP'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.enabled');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'SMTP主机', 'sys.mail.host', '', 'Y', 'admin', now(), 'smtp host'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.host');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'SMTP端口', 'sys.mail.port', '465', 'Y', 'admin', now(), 'smtp port'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.port');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '邮箱账号', 'sys.mail.username', '', 'Y', 'admin', now(), 'mail user'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.username');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '邮箱密码', 'sys.mail.password', '', 'Y', 'admin', now(), 'mail password'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.password');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '发件人地址', 'sys.mail.from', '', 'Y', 'admin', now(), 'mail from'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.from');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'SMTP SSL', 'sys.mail.ssl', 'true', 'Y', 'admin', now(), 'ssl switch'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.ssl');

INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT '注册邮箱验证', 'sys.mail.verifyEnabled', 'false', 'Y', 'admin', now(), 'register email verify'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.verifyEnabled');

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT 118, '基础设置', 1, 10, 'basic', 'system/basic/index', '', '',
       1, 0, 'C', '0', '0', 'system:basic:list', 'edit', 'admin', now(), '', null, 'site and mail settings'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 118);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT 1181, '基础设置查询', 118, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'system:basic:query', '#', 'admin', now(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1181);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT 1182, '基础设置修改', 118, 2, '', '', '', '',
       1, 0, 'F', '0', '0', 'system:basic:edit', '#', 'admin', now(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 1182);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.menu_id IN (118, 1181, 1182)
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id);
