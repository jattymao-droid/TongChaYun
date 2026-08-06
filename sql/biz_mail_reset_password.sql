-- Forgot password email reset switch
INSERT INTO sys_config (config_name, config_key, config_value, config_type, create_by, create_time, remark)
SELECT 'forgot password email reset', 'sys.mail.resetEnabled', 'false', 'Y', 'admin', now(), 'Allow reset password by email code when enabled'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'sys.mail.resetEnabled');
