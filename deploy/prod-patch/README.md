# 通查云 · 生产库增量补丁（P28–P30）

相对 **2026-08-08** 已上线包，本次新增能力对应库脚本如下。

| 脚本 | ROADMAP | 内容 |
| :--- | :--- | :--- |
| `biz_phase28_reach.sql` | P22 触达与预约 | 问卷/查询预约发布、截止、提醒、答卷邮件字段 |
| `biz_phase29_version.sql` | P23 版本与协作 | 查询/问卷版本、协作者、发布审批、项目审计 |
| `biz_phase30_risk.sql` | P24 风控与洞察 | 黑名单表、访问日志渠道字段 |
| （无） | P25 打印与凭证 | **无库变更**（凭证号由 answerId 派生） |

**不要**在生产执行 `biz_demo_seed.sql`。

## 推荐：一键执行合并脚本

```bash
# 上传本目录到服务器后
cd /www/wwwroot/wj.xmls.vip

# 方式 A：宝塔 Docker PostgreSQL 18（容器名以 env.sh 为准）
docker exec -i postgresql_18_p5mm-postgresql_18_p5mm-1 \
  psql -U postgres -d tongchayun < prod-patch/prod_patch_p28_p30.sql

# 方式 B：本机 psql
export PGPASSWORD='你的密码'
psql -h 127.0.0.1 -p 35432 -U postgres -d tongchayun -f prod-patch/prod_patch_p28_p30.sql

# 或
chmod +x prod-patch/apply.sh && ./prod-patch/apply.sh
```

也可按顺序分文件：

```bash
psql ... -f prod-patch/biz_phase28_reach.sql
psql ... -f prod-patch/biz_phase29_version.sql
psql ... -f prod-patch/biz_phase30_risk.sql
```

脚本可重复执行（`IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`）。

## 执行后

1. 部署新版应用包（jar + 前端）
2. `./bin/stop.sh && SKIP_DB_INIT=1 ./start.sh`（避免误跑全量初始化）
3. 抽查：预约/邮件、版本与协作者、风控看板

## 校验

合并脚本末尾会输出表/字段存在性（期望 count=1）。
