<p align="center">
  <img src="https://oscimg.oschina.net/oscnet/up-d3d0a9303e11d522a06cd263f3079027715.png" alt="RuoYi Logo" width="120">
</p>

<h1 align="center">RuoYi-Vue PostgreSQL Version</h1>

<p align="center">
  <b>基于 RuoYi-Vue v3.9.2 · 前后端分离 · PostgreSQL 适配版</b>
</p>

<p align="center">
  <a href="https://gitee.com/jatty01/ruoyi-vue-postgresql-version"><img src="https://gitee.com/jatty01/ruoyi-vue-postgresql-version/badge/star.svg?theme=dark" alt="Gitee star"></a>
  <img src="https://img.shields.io/badge/RuoYi-v3.9.2-brightgreen" alt="RuoYi">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/PostgreSQL-14%2B-4169E1?logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Vue-2.x-42B883?logo=vue.js&logoColor=white" alt="Vue">
  <img src="https://img.shields.io/badge/JDK-17%2B-orange?logo=openjdk&logoColor=white" alt="JDK">
  <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue" alt="License"></a>
</p>

<p align="center">
  <a href="#-快速开始">快速开始</a> ·
  <a href="#-技术栈">技术栈</a> ·
  <a href="#-主要改动">主要改动</a> ·
  <a href="#-内置功能">内置功能</a> ·
  <a href="#-常见问题">常见问题</a>
</p>

---

## ✨ 项目简介

本仓库是在官方 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) 基础上改造的版本，将默认 **MySQL** 替换为 **PostgreSQL**，保留若依完整的权限、代码生成与系统管理能力，适合本地开发与二次扩展。

| 项目 | 说明 |
| :--- | :--- |
| 仓库地址 | https://gitee.com/jatty01/ruoyi-vue-postgresql-version |
| 上游项目 | [RuoYi-Vue v3.9.2](https://gitee.com/y_project/RuoYi-Vue) |
| 架构模式 | 前后端分离（Spring Boot + Vue） |
| 默认端口 | 后端 `8080` · 前端 `1024` |

---

## 🛠 技术栈

<table>
  <tr>
    <td width="25%"><b>后端</b></td>
    <td>Spring Boot 4.x · Spring Security · JWT · MyBatis · Druid · Redis · PageHelper</td>
  </tr>
  <tr>
    <td><b>前端</b></td>
    <td>Vue 2 · Element UI · Vuex · Vue Router · Axios</td>
  </tr>
  <tr>
    <td><b>数据库</b></td>
    <td><b>PostgreSQL 14+</b>（推荐 16 / 18）</td>
  </tr>
  <tr>
    <td><b>环境</b></td>
    <td>JDK 17+ · Maven 3.8+ · Node.js 16+ · Redis</td>
  </tr>
</table>

---

## 🔄 主要改动

相对官方 MySQL 版，本仓库完成了以下适配：

- ✅ 数据源驱动与 JDBC 连接串切换为 PostgreSQL
- ✅ PageHelper 方言改为 `postgresql`
- ✅ Mapper / 数据权限 SQL 语法适配  
  `sysdate()` → `now()` · `ifnull` → `coalesce` · `find_in_set` → `string_to_array`
- ✅ 代码生成器元数据查询改为读取 `pg_catalog`
- ✅ 提供完整 PostgreSQL 初始化脚本与一键导入工具

---

## 📦 内置功能

<details open>
<summary><b>点击展开 / 收起功能清单</b></summary>

<br>

| 模块 | 说明 |
| :--- | :--- |
| 用户管理 | 系统用户配置与维护 |
| 部门管理 | 组织机构树，支持数据权限 |
| 岗位管理 | 职务配置 |
| 菜单管理 | 菜单、操作权限、按钮权限标识 |
| 角色管理 | 菜单权限 + 数据范围权限 |
| 字典 / 参数 | 系统字典与动态参数配置 |
| 通知公告 | 公告发布与已读记录 |
| 日志审计 | 操作日志、登录日志 |
| 在线用户 | 活跃会话监控 |
| 定时任务 | 任务调度与执行日志 |
| 代码生成 | 一键生成前后端 CRUD 代码 |
| 系统接口 | SpringDoc API 文档 |
| 监控中心 | 服务监控、缓存监控、连接池监视 |

</details>

---

## 🚀 快速开始

> **前置条件**：本机已安装并启动 PostgreSQL、Redis；已安装 JDK 17+、Maven、Node.js。

### ① 初始化数据库

默认库名：`ry_vue`

```bash
# 推荐：读取 application-druid.yml 自动建库导入
python3 sql/init_postgresql.py

# 或手动导入
# createdb -U postgres ry_vue
psql -U postgres -d ry_vue -f sql/ry_postgresql.sql
psql -U postgres -d ry_vue -f sql/quartz_postgresql.sql
```

| 文件 | 用途 |
| :--- | :--- |
| `sql/ry_postgresql.sql` | 业务表 + 初始数据 |
| `sql/quartz_postgresql.sql` | Quartz 表（可选） |
| `sql/init_postgresql.py` | 一键建库导入 |
| `sql/convert_mysql_to_pg.py` | MySQL 脚本 → PG 脚本转换 |

### ② 修改配置

编辑 `ruoyi-admin/src/main/resources/application-druid.yml`：

```yaml
spring:
  datasource:
    driverClassName: org.postgresql.Driver
    druid:
      master:
        url: jdbc:postgresql://localhost:5432/ry_vue?stringtype=unspecified&TimeZone=Asia/Shanghai
        username: postgres
        password: your_password
```

同时按需修改：

| 配置项 | 文件 | 说明 |
| :--- | :--- | :--- |
| `ruoyi.profile` | `application.yml` | 上传文件目录 |
| `log.path` | `logback.xml` | 日志目录 |
| Redis | `application.yml` | 默认 `localhost:6379` |

### ③ 启动后端

```bash
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

🌐 后端地址：http://localhost:8080  

也可在 IDE 中直接运行 `com.ruoyi.RuoYiApplication`。

### ④ 启动前端

```bash
cd ruoyi-ui
npm install
npm run dev
```

🌐 前端地址：http://localhost:1024

---

## 🔑 默认账号

| 账号 | 密码 | 角色 |
| :---: | :---: | :--- |
| `admin` | `admin123` | 超级管理员 |
| `ry` | `admin123` | 普通角色示例用户 |

> ⚠️ 生产环境请务必修改默认密码，勿将真实数据库口令提交到仓库。

---

## 📁 目录结构

```text
ruoyi-vue-postgresql-version
├── ruoyi-admin          # Web 启动入口
├── ruoyi-common         # 通用工具与常量
├── ruoyi-framework      # 安全、数据源、AOP 等框架层
├── ruoyi-system         # 系统业务模块
├── ruoyi-quartz         # 定时任务
├── ruoyi-generator      # 代码生成
├── ruoyi-ui             # Vue 前端工程
└── sql                  # PostgreSQL 脚本与工具
    ├── ry_postgresql.sql
    ├── quartz_postgresql.sql
    ├── init_postgresql.py
    └── convert_mysql_to_pg.py
```

---

## ❓ 常见问题

<details>
<summary><b>1. 报错 character = integer</b></summary>

<br>

PostgreSQL 类型校验更严格。`char` 字段请与字符串比较：

```sql
-- ❌ 错误
status = 0

-- ✅ 正确
status = '0'
```

</details>

<details>
<summary><b>2. 启动失败：日志目录不存在</b></summary>

<br>

检查 `logback.xml` 中的 `log.path`，确保目录已创建且进程可写。

</details>

<details>
<summary><b>3. 代码生成“导入表”为空</b></summary>

<br>

确认表注释已写入（脚本末尾包含 `COMMENT ON`），且当前连接用户默认 schema 为 `public`。

</details>

---

## 🙏 致谢

本项目基于若依开源框架 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) 二次开发，感谢原作者与社区贡献者。

| 资源 | 链接 |
| :--- | :--- |
| 官方文档 | http://doc.ruoyi.vip |
| 官方演示 | http://vue.ruoyi.vip |
| 上游仓库 | https://gitee.com/y_project/RuoYi-Vue |

---

## 📄 License

沿用若依开源协议，详见 [LICENSE](./LICENSE)。

<p align="center">
  <sub>If this project helps you, please give it a ⭐ on Gitee.</sub>
</p>
