#!/usr/bin/env python3
"""Create ry_vue database and import RuoYi PostgreSQL scripts."""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

SQL_DIR = Path(__file__).resolve().parent
DRUID_YML = SQL_DIR.parent / "ruoyi-admin/src/main/resources/application-druid.yml"


def read_db_config() -> tuple[str, str, str, str]:
    text = DRUID_YML.read_text(encoding="utf-8")
    url = re.search(r"url:\s*jdbc:postgresql://([^:]+):(\d+)/([^\?\s]+)", text)
    user = re.search(r"username:\s*(\S+)", text)
    password = re.search(r"password:\s*(\S+)", text)
    if not (url and user and password):
        raise SystemExit("Failed to parse database config from application-druid.yml")
    host, port, db = url.group(1), url.group(2), url.group(3)
    return host, port, db, user.group(1), password.group(1)  # type: ignore[return-value]


def psql(env: dict, args: list[str], check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["psql", *args],
        env=env,
        check=check,
        text=True,
        capture_output=True,
    )


def main() -> None:
    host, port, db, user, password = read_db_config()  # type: ignore[misc]
    env = {**dict(**{k: v for k, v in __import__("os").environ.items()}), "PGPASSWORD": password}

    print(f"Connecting as {user}@{host}:{port}, target database: {db}")
    # connectivity check
    r = psql(env, ["-h", host, "-p", port, "-U", user, "-d", "postgres", "-c", "SELECT 1;"], check=False)
    if r.returncode != 0:
        print(r.stderr or r.stdout)
        raise SystemExit("Cannot connect to PostgreSQL. Check service status / username / password.")

    exists = psql(
        env,
        ["-h", host, "-p", port, "-U", user, "-d", "postgres", "-tAc", f"SELECT 1 FROM pg_database WHERE datname='{db}'"],
    ).stdout.strip()
    if exists != "1":
        print(f"Creating database {db} ...")
        psql(env, ["-h", host, "-p", port, "-U", user, "-d", "postgres", "-c", f"CREATE DATABASE {db} ENCODING 'UTF8';"])
    else:
        print(f"Database {db} already exists, reloading schema scripts ...")

    for script in ("ry_postgresql.sql", "quartz_postgresql.sql"):
        path = SQL_DIR / script
        print(f"Importing {script} ...")
        r = psql(env, ["-h", host, "-p", port, "-U", user, "-d", db, "-v", "ON_ERROR_STOP=1", "-f", str(path)], check=False)
        if r.returncode != 0:
            print(r.stderr or r.stdout)
            raise SystemExit(f"Failed importing {script}")
        print(f"OK: {script}")

    tables = psql(
        env,
        ["-h", host, "-p", port, "-U", user, "-d", db, "-tAc", "SELECT count(*) FROM information_schema.tables WHERE table_schema='public'"],
    ).stdout.strip()
    users = psql(
        env,
        ["-h", host, "-p", port, "-U", user, "-d", db, "-tAc", "SELECT user_name FROM sys_user ORDER BY user_id"],
    ).stdout.strip().replace("\n", ", ")
    print(f"Done. public tables={tables}; users={users}")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as e:
        print(e.stderr or e.stdout or str(e), file=sys.stderr)
        raise SystemExit(1)
