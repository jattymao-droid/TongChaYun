-- 通查云: register role for biz users
-- Enable self-registration; create role "业务用户" (data_scope=5 self-only); grant biz menus.

UPDATE sys_config
SET config_value = 'true',
    update_by = 'admin',
    update_time = now()
WHERE config_key = 'sys.account.registerUser';

INSERT INTO sys_role (
  role_id, role_name, role_key, role_sort, data_scope,
  menu_check_strictly, dept_check_strictly, status, del_flag,
  create_by, create_time, remark
)
SELECT 3, '业务用户', 'biz_user', 3, '5',
       1, 1, '0', '0',
       'admin', now(), '注册用户默认角色：仅可管理本人查询/问卷'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_id = 3 OR role_key = 'biz_user');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_key = 'biz_user'
  AND m.menu_id BETWEEN 2000 AND 2099
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu rm
    WHERE rm.role_id = r.role_id AND rm.menu_id = m.menu_id
  );

SELECT setval(pg_get_serial_sequence('sys_role', 'role_id'),
  greatest(coalesce((SELECT max(role_id) FROM sys_role), 1), 3));
