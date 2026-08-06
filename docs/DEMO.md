# 通查云 演示指南

查询系统 + 问卷系统（`ruoyi-biz`）本地演示说明。

## 环境

- JDK 17+ · Maven · Node.js · PostgreSQL · Redis
- 后端默认 `8080`，前端 `npm run dev`（常见 `80`/`1024`）
- 账号：`admin` / `admin123`（需验证码，可从 Redis `captcha_codes:{uuid}` 读取）
- 也可自助注册：登录页「立即注册」→ 自动获得「业务用户」角色，可发布本人查询/问卷

## 启动

```bash
# 数据库（首次，按顺序执行）
psql -U postgres -d ry_vue -f sql/ry_postgresql.sql   # 若依基础库，按实际脚本名
psql -U postgres -d ry_vue -f sql/biz_postgresql.sql
psql -U postgres -d ry_vue -f sql/biz_phase4.sql
psql -U postgres -d ry_vue -f sql/biz_phase7.sql
psql -U postgres -d ry_vue -f sql/biz_phase9.sql
psql -U postgres -d ry_vue -f sql/biz_phase10.sql
psql -U postgres -d ry_vue -f sql/biz_phase12.sql
psql -U postgres -d ry_vue -f sql/biz_phase13.sql
psql -U postgres -d ry_vue -f sql/biz_phase18.sql
psql -U postgres -d ry_vue -f sql/biz_phase18_scope.sql
psql -U postgres -d ry_vue -f sql/biz_phase19.sql
psql -U postgres -d ry_vue -f sql/biz_survey_matrix_radio.sql
psql -U postgres -d ry_vue -f sql/biz_phase21.sql
psql -U postgres -d ry_vue -f sql/biz_phase22.sql
psql -U postgres -d ry_vue -f sql/biz_phase23_join.sql
psql -U postgres -d ry_vue -f sql/biz_phase24_query_security.sql
psql -U postgres -d ry_vue -f sql/biz_phase25_query_p2.sql
psql -U postgres -d ry_vue -f sql/biz_phase26_user_admin.sql
psql -U postgres -d ry_vue -f sql/biz_phase27_basic_settings.sql
psql -U postgres -d ry_vue -f sql/biz_mail_reset_password.sql
psql -U postgres -d ry_vue -f sql/biz_survey_question_types.sql
psql -U postgres -d ry_vue -f sql/biz_survey_question_types_extra.sql
psql -U postgres -d ry_vue -f sql/biz_register_role.sql
psql -U postgres -d ry_vue -f sql/biz_sidebar_menu.sql
psql -U postgres -d ry_vue -f sql/biz_demo_seed.sql

# 后端（敏感配置可用环境变量覆盖：DB_PASSWORD / TOKEN_SECRET / RUOYI_PROFILE）
mvn -pl ruoyi-admin -am package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 前端
cd ruoyi-ui && npm install && npm run dev
```

> 说明：仅执行到 `biz_phase21` 会缺少多数据集、查询安全、基础设置、忘记密码等能力。生产请关闭匿名 Swagger/Druid，并设置 `TOKEN_SECRET`、`DB_PASSWORD`。

## 演示路径

| 能力 | 入口 |
| :--- | :--- |
| 最近修改 / 答卷通知 | 左侧菜单「最近修改」「答卷通知」；侧栏顶部可新建/从模板创建 |
| 查询上传→配置→发布 | 业务中心 → 查询管理 |
| 公开查询 | `/q/{publicCode}` 条件页 → `/q/{publicCode}/result` 独立结果页 |
| 问卷设计→发布 | 业务中心 → 问卷管理 |
| 公开填写 | `/s/{publicCode}` |
| 统计饼图 | 问卷 → 统计 |
| Webhook | 问卷编辑 → Webhook |
| 查询分布图 | 查询 → 预览 → 字段分布 |
| 访问统计 | 列表浏览/查询次数；看板公开查询次数与今日 UV |
| 公开导出 | 查询公开页 / H5「导出」按条件下载 Excel |
| 字段控件 | 查询 → 字段：下拉/日期/区间；设计页 Banner 与布局 |
| 结果页样式 | 查询 → 设计 →「结果页」：预设/背景/遮罩/文字色调/布局/条件摘要；保存后看 `/q/{code}/result` |
| 查询设置向导 | 查询列表 →「设置」：导入 → 字段 → 设计/预览 → 发布（发布前校验条件列与结果列） |
| 问卷设置向导 | 问卷列表 →「设置」：基础设置 → 题目设计 → 预览 → 发布 |
| 扩展题型 | 是非/数字/NPS/说明段/邮箱/日期时间/滑块/图片单选等；预览与公开页共用渲染组件 |
| 附件上传 | 单题可配 1–10MB，服务端硬顶 10MB |
| 改造清单 | [`docs/IMPROVEMENT.md`](./IMPROVEMENT.md) P16–P21 已落地（评分趋势/服务端草稿/级联题/通知合并等） |
| 匹配方式扩展 | 字段配置：GT/LT/GTE/LTE/IN；可开默认排序；下方可见导入样例行 |
| 问卷显隐 | 设计页「显示条件」：仅当某题选中指定 value 时显示 |
| NPS 统计 | 问卷统计页展示推荐/被动/贬损与 NPS 分 |
| 答卷导出 | 选项导出为文案；附件导出文件名+路径；矩阵题「行:选项」；可按渠道/日期筛选导出 |
| 结果分布图 | 查询设计开启「结果分布图」后，公开/H5 结果页可按当前条件查看字段分布 |
| 矩阵单选 | 问卷设计添加「矩阵单选」；执行 `sql/biz_survey_matrix_radio.sql`（DEMO 启动清单已含） |
| 查询模板 | 「从模板创建」含演示数据，可直接配置条件后发布 |
| 答卷通知 | 看板深链打开指定答卷；顶栏铃铛含「答卷通知」页签 |
| 用户注册 | 开启后登录页可注册；默认角色「业务用户」仅管理本人查询/问卷并可发布 |


## 样例短码

执行 `sql/biz_demo_seed.sql` 后固定可用：

- 查询：`/q/q6jjyg79`
- 问卷：`/s/97vw7fqf`

## 冒烟脚本

```bash
./scripts/smoke_biz.sh
```

需本机后端已启动、Redis 可用。脚本会登录、拉看板、公开 meta、检查通知接口。

## 独立 H5

```bash
cd ruoyi-h5 && npm install && npm run dev
```

访问 `http://127.0.0.1:5173/q/{code}` 或 `/s/{code}`。

生产可参考 `ruoyi-h5/nginx.conf.example` 部署到 `/h5/`。
