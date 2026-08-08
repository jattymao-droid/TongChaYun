-- TongChaYun production: rename admin login to jatty01
-- Run on server DB only. Does not change password.
--
-- Docker example:
--   docker exec -i postgresql_18_p5mm-postgresql_18_p5mm-1 \
--     psql -U postgres -d tongchayun < prod-patch/rename_admin_to_jatty01.sql
--
-- Host psql example:
--   psql -h 127.0.0.1 -p 35432 -U postgres -d tongchayun -f prod-patch/rename_admin_to_jatty01.sql

BEGIN;

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM sys_user
     WHERE user_name = 'jatty01' AND user_id <> 1
  ) THEN
    RAISE EXCEPTION 'user_name jatty01 already exists on another account';
  END IF;
END $$;

UPDATE sys_user
   SET user_name = 'jatty01',
       update_by = 'admin',
       update_time = now()
 WHERE user_id = 1
   AND user_name = 'admin';

UPDATE sys_user
   SET user_name = 'jatty01',
       update_by = coalesce(nullif(update_by, ''), 'admin'),
       update_time = now()
 WHERE user_id = 1
   AND user_name <> 'jatty01';

COMMIT;

SELECT user_id, user_name, nick_name, status, update_time
  FROM sys_user
 WHERE user_id = 1;
