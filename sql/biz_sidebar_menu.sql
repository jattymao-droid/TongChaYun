-- 通查云: menu labels aligned with workbench sidebar UX
UPDATE sys_menu SET menu_name = '仪表盘', icon = 'dashboard', order_num = 0
WHERE menu_id = 2003;

UPDATE sys_menu SET menu_name = '我的查询', icon = 'search', order_num = 1
WHERE menu_id = 2001;

UPDATE sys_menu SET menu_name = '我的问卷', icon = 'form', order_num = 2
WHERE menu_id = 2002;

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 2005, '答卷通知', 2000, 3, 'notify', 'biz/dashboard/index', '', '',
       1, 0, 'C', '0', '0', 'biz:dashboard:list', 'message', 'admin', now(), '答卷通知'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2005);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, 2005
FROM sys_role r
WHERE r.role_key = 'biz_user'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = 2005
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, 2005
WHERE NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = 2005);
