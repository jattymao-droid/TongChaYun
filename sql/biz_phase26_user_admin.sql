-- Admin: user project hub + ownership transfer permissions
-- menu 2006 用户业务, buttons 2030/2031

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT 2006, '用户业务', 2000, 4, 'users', 'biz/users/index', '', '',
       1, 0, 'C', '0', '0', 'biz:user:list', 'peoples', 'admin', now(), '', null, '按用户查看并管理查询/问卷'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2006);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT 2030, '用户业务查询', 2006, 1, '', '', '', '',
       1, 0, 'F', '0', '0', 'biz:user:list', '#', 'admin', now(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2030);

INSERT INTO sys_menu (
  menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark
)
SELECT 2031, '项目归属转让', 2006, 2, '', '', '', '',
       1, 0, 'F', '0', '0', 'biz:user:transfer', '#', 'admin', now(), '', null, ''
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = 2031);

-- Grant to admin role only (not biz_user)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id
FROM sys_menu m
WHERE m.menu_id IN (2006, 2030, 2031)
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
  );

-- Prefer admin-facing labels for query/survey menus
UPDATE sys_menu SET menu_name = '查询管理' WHERE menu_id = 2001 AND menu_name LIKE '%查询%';
UPDATE sys_menu SET menu_name = '问卷管理' WHERE menu_id = 2002 AND menu_name LIKE '%问卷%';
