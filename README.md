<p align="center">
  <img src="./ruoyi-ui/public/logo.svg" alt="通查云" width="88" height="88">
</p>

<h1 align="center">通查云</h1>

<p align="center">
  <b>查询 · 问卷 · 一站式发布平台</b><br>
  <sub>基于 RuoYi-Vue 3.9.2 · 前后端分离 · PostgreSQL</sub>
</p>

<p align="center">
  <a href="https://github.com/jattymao-droid/TongChaYun"><img src="https://img.shields.io/github/stars/jattymao-droid/TongChaYun?style=flat&logo=github" alt="Stars"></a>
  <img src="https://img.shields.io/badge/RuoYi-v3.9.2-brightgreen" alt="RuoYi">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-14%2B-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Vue-2%20%2F%203-42B883?logo=vue.js&logoColor=white" alt="Vue">
  <img src="https://img.shields.io/badge/JDK-17%2B-orange?logo=openjdk&logoColor=white" alt="JDK">
  <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue" alt="License"></a>
</p>

<p align="center">
  <a href="#-产品能力">产品能力</a> ·
  <a href="#-技术栈">技术栈</a> ·
  <a href="#-快速开始">快速开始</a> ·
  <a href="#-目录结构">目录结构</a> ·
  <a href="#-文档">文档</a>
</p>

---

## ✨ 产品简介

**通查云**面向成绩查询、录取查询、满意度问卷、活动报名等场景，提供管理端配置与免登录公开访问：

- **快查**：Excel / 多数据集导入 → 条件配置 → 短链发布 → 结果页展示
- **问卷**：拖拽设计、跳题逻辑、答卷统计、导出与 Webhook 通知
- **发布**：短链 `/q/{code}`、`/s/{code}`，支持二维码、访问控制与限流
- **运营**：业务看板、模板、基础设置、微信 / QQ 登录（可选）

底层保留若依完整的权限、菜单、字典、定时任务与代码生成能力，适合私有化部署与二次开发。

| 项目 | 说明 |
| :--- | :--- |
| 仓库 | https://github.com/jattymao-droid/TongChaYun |
| 上游 | [RuoYi-Vue v3.9.2](https://gitee.com/y_project/RuoYi-Vue) |
| 默认端口 | 后端 `8080` · 管理端 `1024` · H5 `5173` |

---

## 🎯 产品能力

| 模块 | 能力 |
| :--- | :--- |
| 查询管理 | 多字段条件、多数据集、关联、结果页主题、访问密码、UV/次数统计、导出 |
| 问卷管理 | 丰富题型、跳题、草稿、答卷通知、交叉分析、Webhook |
| 工作台 | 最近修改、从模板创建、业务看板 |
| 公开访问 | 管理端内置公开页 + 独立 `ruoyi-h5`（Vue 3） |
| 系统管理 | 用户角色权限、基础设置（站点名/Logo/邮件/第三方登录） |
| 第三方登录 | 微信开放平台网站应用扫码、QQ 互联 OAuth2（可选启用） |

---

## 🛠 技术栈

| 层级 | 技术 |
| :--- | :--- |
| 后端 | Spring Boot 4.x · Spring Security · JWT · MyBatis · Druid · Redis |
| 管理端 | Vue 2 · Element UI · Vuex · Axios |
| 公开 H5 | Vue 3 · Vite |
| 数据库 | **PostgreSQL 14+**（推荐 16） |
| 环境 | JDK 17+ · Maven 3.8+ · Node.js 16+ · Redis |

---

## 🚀 快速开始

> 前置：本机已启动 **PostgreSQL**、**Redis**；已安装 JDK 17+、Maven、Node.js。

### 1. 克隆仓库

```bash
git clone https://github.com/jattymao-droid/TongChaYun.git
cd TongChaYun
```

### 2. 初始化数据库

默认库名：`ry_vue`

```bash
# 推荐：一键建库导入若依基础脚本
python3 sql/init_postgresql.py

# 或手动
psql -U postgres -d ry_vue -f sql/ry_postgresql.sql
psql -U postgres -d ry_vue -f sql/quartz_postgresql.sql
```

再导入业务扩展（按需，可依次执行）：

```bash
psql -U postgres -d ry_vue -f sql/biz_postgresql.sql
psql -U postgres -d ry_vue -f sql/biz_phase27_basic_settings.sql
psql -U postgres -d ry_vue -f sql/biz_oauth_bind.sql
# 其余增量见 sql/biz_phase*.sql
```

### 3. 配置环境变量

数据库口令**不要写死在仓库**，请用环境变量：

```bash
export DB_HOST=127.0.0.1
export DB_PORT=5432
export DB_NAME=ry_vue
export DB_USERNAME=postgres
export DB_PASSWORD=你的密码
```

可选：`RUOYI_PROFILE`（上传目录）、Redis 配置见 `ruoyi-admin/src/main/resources/application.yml`。

### 4. 启动后端

```bash
mvn -pl ruoyi-admin -am package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

- 接口：http://127.0.0.1:8080  
- 也可 IDE 运行 `com.ruoyi.RuoYiApplication`

### 5. 启动管理端

```bash
cd ruoyi-ui
npm install
npm run dev
```

- 地址：http://127.0.0.1:1024

### 6. （可选）启动公开 H5

```bash
cd ruoyi-h5
npm install
npm run dev
```

- 地址：http://127.0.0.1:5173  
- 路由：`/q/:code` 查询 · `/s/:code` 问卷

---

## 🔑 默认账号

| 账号 | 密码 | 说明 |
| :---: | :---: | :--- |
| `admin` | `admin123` | 超级管理员 |
| `ry` | `admin123` | 普通角色示例 |

> 生产环境请立即修改默认密码；勿将真实数据库口令、OAuth 密钥提交到 Git。

---

## 📁 目录结构

```text
TongChaYun
├── ruoyi-admin       # 启动入口 / Controller
├── ruoyi-framework   # 安全、缓存、OAuth 登录等
├── ruoyi-system      # 系统用户、基础设置
├── ruoyi-biz         # 查询 / 问卷业务模块
├── ruoyi-ui          # 管理端（Vue 2）
├── ruoyi-h5          # 公开 H5（Vue 3）
├── sql               # PostgreSQL 脚本（含 biz_*）
├── scripts           # 冒烟脚本等
├── docs              # 演示与改造说明
├── dev.md            # 需求与设计
└── development-plan.md
```

---

## 📚 文档

| 文档 | 说明 |
| :--- | :--- |
| [dev.md](./dev.md) | 需求与设计 |
| [development-plan.md](./development-plan.md) | 分阶段开发计划 |
| [docs/DEMO.md](./docs/DEMO.md) | 演示指南 |
| [docs/IMPROVEMENT.md](./docs/IMPROVEMENT.md) | 改造清单 |
| [ruoyi-h5/README.md](./ruoyi-h5/README.md) | 独立 H5 部署 |

冒烟检查（后端已启动时）：

```bash
./scripts/smoke_biz.sh
```

---

## ❓ 常见问题

<details>
<summary><b>1. PostgreSQL：character = integer</b></summary>

<br>

`char` / 状态字段请与字符串比较：`status = '0'`，不要写 `status = 0`。

</details>

<details>
<summary><b>2. 数据库连接失败</b></summary>

<br>

确认已设置 `DB_PASSWORD`，且 `application-druid.yml` 使用 `${DB_PASSWORD:}`。可用：

```bash
export DB_PASSWORD=你的密码
```

</details>

<details>
<summary><b>3. 微信 / QQ 登录按钮无法跳转</b></summary>

<br>

在 **系统管理 → 基础设置 → 第三方登录** 启用并填写 AppID / 密钥，同时在开放平台登记回调地址：

`{callbackBase}/login/oauth/callback/wechat|qq`

</details>

<details>
<summary><b>4. 日志目录不存在导致启动失败</b></summary>

<br>

检查 `logback.xml` 中的 `log.path`，确保目录存在且可写。

</details>

---

## 🙏 致谢

本项目基于开源框架 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) 二次开发，感谢原作者与社区。

| 资源 | 链接 |
| :--- | :--- |
| 若依文档 | http://doc.ruoyi.vip |
| 若依演示 | http://vue.ruoyi.vip |
| 上游仓库 | https://gitee.com/y_project/RuoYi-Vue |

---

## 📄 License

沿用若依开源协议，详见 [LICENSE](./LICENSE)。

<p align="center">
  <sub>如果通查云对你有帮助，欢迎在 GitHub 点一颗 ⭐</sub>
</p>
