# RuoYi-Vue PostgreSQL Version

基于 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) **v3.9.2** 改造的前后端分离快速开发框架，数据库从 **MySQL 切换为 PostgreSQL**。

仓库地址：https://gitee.com/jatty01/ruoyi-vue-postgresql-version

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Spring Boot 4.x、Spring Security、JWT、MyBatis、Druid、Redis、PageHelper |
| 前端 | Vue 2、Element UI、Vuex、Vue Router、Axios |
| 数据库 | **PostgreSQL 14+**（推荐 16/18） |
| 运行环境 | JDK 17+、Maven 3.8+、Node.js 16+、Redis |

## 相对官方版的主要改动

- 数据源驱动与连接串改为 PostgreSQL
- PageHelper 方言改为 `postgresql`
- Mapper / 数据权限 SQL 适配（如 `sysdate`→`now()`、`ifnull`→`coalesce`、`find_in_set`→`string_to_array` 等）
- 代码生成器元数据查询改为读取 `pg_catalog`
- 提供 PostgreSQL 初始化脚本与导入工具

## 内置功能

用户、角色、菜单、部门、岗位、字典、参数、通知公告、操作/登录日志、在线用户、定时任务、代码生成、系统接口、服务与缓存监控、连接池监视等（与官方 RuoYi-Vue 能力一致）。

## 环境准备

1. 安装并启动 **PostgreSQL**、**Redis**
2. 安装 **JDK 17+**、**Maven**、**Node.js**

## 快速开始

### 1. 初始化数据库

默认库名：`ry_vue`（可在配置中修改）

```bash
# 方式一：使用脚本（会读取 application-druid.yml 中的连接信息）
python3 sql/init_postgresql.py

# 方式二：手动创建库后导入
# createdb -U postgres ry_vue
psql -U postgres -d ry_vue -f sql/ry_postgresql.sql
psql -U postgres -d ry_vue -f sql/quartz_postgresql.sql
```

说明：

- `sql/ry_postgresql.sql`：业务表与初始数据
- `sql/quartz_postgresql.sql`：Quartz 表（可选，当前默认内存调度也可不导入）
- `sql/convert_mysql_to_pg.py`：从官方 MySQL 脚本重新生成 PG 脚本时使用

### 2. 修改配置

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

按需修改：

- `application.yml` → `ruoyi.profile`（上传目录）
- `logback.xml` → `log.path`（日志目录）
- Redis 连接（默认 `localhost:6379`）

### 3. 启动后端

```bash
mvn clean package -DskipTests
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

后端默认地址：http://localhost:8080

也可在 IDE 中直接运行 `com.ruoyi.RuoYiApplication`。

### 4. 启动前端

```bash
cd ruoyi-ui
npm install
npm run dev
```

前端默认地址：http://localhost:1024

## 默认账号

| 账号 | 密码 | 说明 |
| --- | --- | --- |
| admin | admin123 | 超级管理员 |
| ry | admin123 | 普通角色示例用户 |

## 目录结构

```text
├── ruoyi-admin          # 启动模块（Web 入口）
├── ruoyi-common         # 通用工具
├── ruoyi-framework      # 框架核心（安全、数据源等）
├── ruoyi-system         # 系统业务
├── ruoyi-quartz         # 定时任务
├── ruoyi-generator      # 代码生成
├── ruoyi-ui             # Vue 前端
└── sql
    ├── ry_postgresql.sql
    ├── quartz_postgresql.sql
    ├── init_postgresql.py
    └── convert_mysql_to_pg.py
```

## 常见问题

**1. `character = integer` 报错**  
PostgreSQL 类型更严格，`char` 字段请与字符串比较，例如 `status = '0'`，不要写 `status = 0`。

**2. 日志目录不存在导致启动失败**  
检查 `logback.xml` 中的 `log.path`，确保目录已创建且可写。

**3. 代码生成导入表为空**  
确认表注释已写入（脚本末尾有 `COMMENT ON`），并且当前连接用户默认 schema 为 `public`。

## 致谢

本项目基于若依开源框架 [RuoYi-Vue](https://gitee.com/y_project/RuoYi-Vue) 二次开发，感谢原作者与社区贡献者。

- 官方文档：http://doc.ruoyi.vip
- 官方演示：http://vue.ruoyi.vip

## License

沿用若依开源协议，详见 [LICENSE](./LICENSE)。
