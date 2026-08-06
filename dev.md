# 通查云查询与问卷系统 — 开发文档

> 基于现有 **RuoYi-Vue PostgreSQL Version（v3.9.2）** 二次开发。  
> 文档版本：v0.1 · 更新日期：2026-08-01

---

## 1. 文档说明

| 项 | 内容 |
| :--- | :--- |
| 目标读者 | 本项目前后端开发、测试、产品 |
| 基座工程 | `RuoYi-Vue`（Spring Boot 4 + Vue2 + PostgreSQL） |
| 仓库 | https://gitee.com/jatty01/ruoyi-vue-postgresql-version |
| 文档目的 | 明确产品范围、架构、数据模型、接口与分期计划，作为研发落地依据 |

相关文档：

- 工程说明：[`README.md`](./README.md)
- 本文档：[`dev.md`](./dev.md)

---

## 2. 产品目标

在现有若依系统（用户/角色/菜单/权限/文件上传等）之上，建设两类业务能力：

### 2.1 查询系统（Query）

登录用户可：

1. **新建查询项目**
2. **上传 Excel**，解析表头与样例数据
3. **自定义可查询字段**（是否作为条件、展示、排序等）
4. **设计查询页效果**（标题、说明、主题色、字段布局、结果列等）
5. **发布查询**，生成对外链接（含 H5）
6. 访客通过链接 **免登录** 按条件查询并查看结果

### 2.2 问卷系统（Survey）

登录用户可：

1. **新建问卷**
2. **拖拽设计表单**（单选/多选/填空/下拉/评分/日期等）
3. **发布问卷**，生成对外链接（含 H5）
4. 访客通过链接 **免登录** 填写并提交
5. 创建者可查看答卷明细与基础统计

### 2.3 非目标（本期不做）

- 复杂 BI 报表 / OLAP
- 付费、多租户 SaaS 计费
- 小程序原生端（H5 可内嵌）
- Excel 超大文件分布式解析（首期限制行数/体积）

---

## 3. 角色与核心流程

### 3.1 角色

| 角色 | 说明 | 能力 |
| :--- | :--- | :--- |
| 系统管理员 | 若依 `admin` | 全部系统管理 + 业务管理 |
| 业务用户 | 注册登录用户 | 创建/设计/发布查询与问卷，查看自己的数据 |
| 匿名访客 | 持有公开链接 | 免登录查询 / 填写问卷 |

> 注册能力复用若依 `sys_user` + `SysRegisterController`，通过系统参数开启注册。

### 3.2 查询发布主流程

```text
注册/登录 → 新建查询 → 上传 Excel → 解析表头/入库
    → 配置查询字段与页面样式 → 预览 → 发布生成链接
    → 访客打开 H5/PC 链接 → 输入条件查询 → 展示结果
```

### 3.3 问卷发布主流程

```text
注册/登录 → 新建问卷 → 拖拽设计题目 → 配置逻辑/校验（可选）
    → 预览 → 发布生成链接 → 访客填写提交
    → 创建者查看答卷与统计
```

---

## 4. 总体架构

### 4.1 架构原则

- **复用若依**：认证、权限、菜单、字典、文件上传、操作日志
- **业务独立模块**：新建 `ruoyi-biz`（或 `tongchayun`）承载查询/问卷，避免污染 `ruoyi-system`
- **管理端 / 公开端分离**：
  - 管理端：`ruoyi-ui`（需登录）
  - 公开端：新建 `ruoyi-h5`（Vue3 + Vant 推荐）或 `ruoyi-ui` 下无鉴权公开路由
- **链接访问**：短码 / UUID，公开 API 走独立 Controller + Security 白名单

### 4.2 逻辑架构

```text
┌─────────────────┐     ┌──────────────────┐
│  ruoyi-ui       │     │  ruoyi-h5        │
│  管理后台(登录)  │     │  公开查询/问卷    │
└────────┬────────┘     └────────┬─────────┘
         │ JWT                    │ 公开 Token/无登录
         ▼                        ▼
┌────────────────────────────────────────────┐
│              ruoyi-admin                   │
│  /system/**   /biz/**   /open/**           │
└────────────────────┬───────────────────────┘
                     │
         ┌───────────┴───────────┐
         ▼                       ▼
   PostgreSQL                 Redis
   业务表/Excel数据           限流·短链缓存
```

### 4.3 推荐模块划分

```text
ruoyi-biz/                         # 新建 Maven 模块
  ├── domain/                      # 实体
  ├── mapper/                      # MyBatis
  ├── service/
  ├── controller/
  │     ├── admin/                 # 需登录管理接口
  │     └── open/                  # 免登录公开接口
  └── resources/mapper/

ruoyi-ui/src/views/biz/            # 管理端页面
  ├── query/                       # 查询项目管理
  └── survey/                      # 问卷项目管理

ruoyi-h5/                          # 可选：公开 H5 工程
  ├── views/query/
  └── views/survey/
```

---

## 5. 功能需求明细

### 5.1 账号

| 编号 | 需求 | 优先级 | 说明 |
| :--- | :--- | :---: | :--- |
| A01 | 用户注册 | P0 | 开启若依注册开关；可扩展手机号/邮箱校验 |
| A02 | 登录/登出 | P0 | 复用现有 JWT |
| A03 | 个人中心 | P1 | 复用 `SysProfile` |

### 5.2 查询系统

| 编号 | 需求 | 优先级 | 说明 |
| :--- | :--- | :---: | :--- |
| Q01 | 查询项目 CRUD | P0 | 名称、描述、状态（草稿/已发布/停用） |
| Q02 | 上传 Excel | P0 | `.xlsx/.xls`，解析 sheet、表头、类型推断 |
| Q03 | 数据入库 | P0 | 表头映射 + 行数据存储（见数据模型） |
| Q04 | 字段配置 | P0 | 是否查询条件、控件类型、字典、是否结果列、排序 |
| Q05 | 页面设计 | P0 | 标题、副标题、主题色、Banner、结果空态文案 |
| Q06 | 预览 | P0 | 管理端内嵌预览 |
| Q07 | 发布/停用 | P0 | 生成 `public_code`，拼装 PC/H5 链接 |
| Q08 | 公开查询 | P0 | 免登录条件查询 + 分页 |
| Q09 | 导出结果 | P1 | 访客或管理员导出当前结果 |
| Q10 | 访问统计 | P2 | PV/UV、查询次数 |
| Q11 | 数据追加/覆盖更新 | P1 | 再次上传 Excel |

**Excel 限制（首期建议）**

| 项 | 限制 |
| :--- | :--- |
| 文件大小 | ≤ 10MB（可配置） |
| 行数 | ≤ 20,000 |
| 列数 | ≤ 50 |
| Sheet | 默认第一个；可选手动选择 |

### 5.3 问卷系统

| 编号 | 需求 | 优先级 | 说明 |
| :--- | :--- | :---: | :--- |
| S01 | 问卷项目 CRUD | P0 | 草稿/已发布/停用/已结束 |
| S02 | 拖拽表单设计器 | P0 | 题目增删改排序 |
| S03 | 题型支持 | P0 | 单选、多选、单行填空、多行填空、下拉 |
| S04 | 扩展题型 | P1 | 评分、日期、手机号、附件上传 |
| S05 | 题目校验 | P0 | 必填、字数、正则（手机号等） |
| S06 | 预览 | P0 | 管理端预览 |
| S07 | 发布生成链接 | P0 | 同查询短链机制 |
| S08 | 公开填写提交 | P0 | 免登录提交；防重复可配置 |
| S09 | 答卷列表 | P0 | 创建者查看 |
| S10 | 基础统计 | P1 | 选择题选项占比、填空列表 |
| S11 | 逻辑跳转 | P2 | 选 A 跳到第 N 题 |
| S12 | 截止时间/份数上限 | P1 | 发布设置 |

### 5.4 公开链接与 H5

| 编号 | 需求 | 优先级 | 说明 |
| :--- | :--- | :---: | :--- |
| O01 | 短链格式 | P0 | `/q/{code}` 查询 · `/s/{code}` 问卷 |
| O02 | 响应式/H5 | P0 | 移动端优先布局 |
| O03 | 链接复制/二维码 | P1 | 管理端一键复制、生成二维码 |
| O04 | 密码访问（可选） | P2 | 链接 + 访问密码 |
| O05 | 限流防刷 | P0 | IP + code 维度限流 |

---

## 6. 数据模型（PostgreSQL）

> 命名前缀：`biz_`。主键统一 `bigint identity`，时间字段 `timestamp`。

### 6.1 ER 概览

```text
biz_query 1───* biz_query_field
biz_query 1───* biz_query_row          # 或 JSONB 存行
biz_query 1───1 biz_query_page         # 页面设计

biz_survey 1───* biz_survey_question
biz_survey 1───* biz_survey_answer
biz_survey_answer 1───* biz_survey_answer_item
```

### 6.2 查询相关表

#### `biz_query` 查询项目

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| query_id | int8 PK | 主键 |
| query_name | varchar(100) | 名称 |
| query_desc | varchar(500) | 描述 |
| public_code | varchar(32) UK | 公开短码 |
| status | char(1) | 0草稿 1已发布 2停用 |
| source_file | varchar(255) | 原始 Excel 路径 |
| sheet_name | varchar(100) | Sheet 名 |
| row_count | int4 | 数据行数 |
| access_pwd | varchar(64) | 可选访问密码（加密） |
| view_count | int8 | 访问次数 |
| create_by / create_time / update_by / update_time / remark | | 审计字段 |
| create_user_id | int8 | 所属用户 |

#### `biz_query_field` 字段配置

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| field_id | int8 PK | |
| query_id | int8 | |
| field_key | varchar(64) | 内部键，如 `c1` |
| field_name | varchar(100) | 表头原名 |
| field_label | varchar(100) | 展示名 |
| data_type | varchar(20) | string/number/date |
| is_query | char(1) | 是否查询条件 |
| query_type | varchar(20) | EQ/LIKE/BETWEEN/IN |
| html_type | varchar(20) | input/select/date |
| is_list | char(1) | 是否结果列 |
| is_sortable | char(1) | 是否可排序 |
| dict_options | text | 下拉选项 JSON |
| sort | int4 | 排序 |
| width | int4 | 列宽建议 |

#### `biz_query_row` 行数据（方案 A，推荐首期）

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| row_id | int8 PK | |
| query_id | int8 | |
| row_no | int4 | Excel 行号 |
| row_data | jsonb | `{"c1":"张三","c2":"100"}` |

> **方案 B（后期）**：动态建物理表 `biz_q_data_{queryId}`，适合超大数据量检索。首期用 JSONB + GIN 索引即可。

索引建议：

```sql
create index idx_biz_query_row_qid on biz_query_row(query_id);
create index idx_biz_query_row_data on biz_query_row using gin(row_data);
```

#### `biz_query_page` 页面设计

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| page_id | int8 PK | |
| query_id | int8 UK | |
| title | varchar(100) | |
| subtitle | varchar(255) | |
| theme_color | varchar(20) | |
| banner_url | varchar(255) | |
| layout_json | text | 布局扩展 JSON |
| result_tips | varchar(255) | 无结果提示 |

### 6.3 问卷相关表

#### `biz_survey` 问卷

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| survey_id | int8 PK | |
| survey_name | varchar(100) | |
| survey_desc | varchar(500) | |
| public_code | varchar(32) UK | |
| status | char(1) | 0草稿 1发布 2停用 3结束 |
| start_time / end_time | timestamp | 有效期 |
| max_answers | int4 | 最大答卷数，0 不限 |
| allow_multi | char(1) | 是否允许多次提交 |
| theme_json | text | 主题样式 |
| view_count / answer_count | int8 | 统计 |
| create_user_id | int8 | |
| 审计字段 | | |

#### `biz_survey_question` 题目

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| question_id | int8 PK | |
| survey_id | int8 | |
| q_type | varchar(20) | radio/checkbox/input/textarea/select/rate/date |
| title | varchar(500) | 题干 |
| required | char(1) | |
| options_json | text | 选项 `[{label,value}]` |
| props_json | text | 校验、占位符、跳转规则等 |
| sort | int4 | |

#### `biz_survey_answer` 答卷

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| answer_id | int8 PK | |
| survey_id | int8 | |
| submit_ip | varchar(128) | |
| submit_ua | varchar(500) | |
| submit_time | timestamp | |
| cost_ms | int4 | 填写耗时 |

#### `biz_survey_answer_item` 答卷明细

| 字段 | 类型 | 说明 |
| :--- | :--- | :--- |
| item_id | int8 PK | |
| answer_id | int8 | |
| question_id | int8 | |
| answer_value | text | 单值或 JSON 数组 |

### 6.4 公开短码生成规则

- 使用 **8~10 位** 不混淆字符集（去掉 `0OIl1`）随机生成
- 冲突重试；或使用 UUID 截断 + Base62
- Redis 缓存 `open:query:{code}` → `queryId`，减少 DB 命中

---

## 7. 接口设计（草案）

> 统一响应：若依 `AjaxResult` / `TableDataInfo`。  
> 管理端前缀：`/biz` · 公开端前缀：`/open`

### 7.1 管理端 — 查询

| Method | Path | 说明 |
| :--- | :--- | :--- |
| GET | `/biz/query/list` | 我的查询列表 |
| GET | `/biz/query/{id}` | 详情（含字段、页面） |
| POST | `/biz/query` | 新建 |
| PUT | `/biz/query` | 更新基础信息 |
| DELETE | `/biz/query/{ids}` | 删除 |
| POST | `/biz/query/upload` | 上传 Excel 并解析 |
| PUT | `/biz/query/fields` | 保存字段配置 |
| PUT | `/biz/query/page` | 保存页面设计 |
| POST | `/biz/query/publish/{id}` | 发布 |
| POST | `/biz/query/offline/{id}` | 停用 |
| GET | `/biz/query/preview/{id}` | 预览查询 |
| GET | `/biz/query/link/{id}` | 获取链接与二维码参数 |

### 7.2 公开端 — 查询

| Method | Path | 说明 |
| :--- | :--- | :--- |
| GET | `/open/query/{code}/meta` | 页面元数据 + 可查询字段（脱敏） |
| POST | `/open/query/{code}/search` | 条件分页查询 |

`search` 请求示例：

```json
{
  "pageNum": 1,
  "pageSize": 10,
  "params": {
    "c1": "张三",
    "c3": "2024-01-01,2024-12-31"
  }
}
```

### 7.3 管理端 — 问卷

| Method | Path | 说明 |
| :--- | :--- | :--- |
| GET/POST/PUT/DELETE | `/biz/survey` | 问卷 CRUD |
| PUT | `/biz/survey/questions` | 保存题目列表（全量覆盖或差量） |
| POST | `/biz/survey/publish/{id}` | 发布 |
| GET | `/biz/survey/answers` | 答卷分页 |
| GET | `/biz/survey/stats/{id}` | 统计 |

### 7.4 公开端 — 问卷

| Method | Path | 说明 |
| :--- | :--- | :--- |
| GET | `/open/survey/{code}/meta` | 问卷结构 |
| POST | `/open/survey/{code}/submit` | 提交答卷 |

### 7.5 Security 白名单

在 `SecurityConfig`（或若依等价配置）放行：

```text
/open/query/** ,
/open/survey/**
```

并对公开接口增加：

- 接口限流（Redis）
- 可选验证码（高频时开启）
- 仅返回已发布且未过期数据

---

## 8. 前端设计

### 8.1 管理端（ruoyi-ui）

新增菜单（建议挂在「业务中心」）：

```text
业务中心
├── 查询管理
│   ├── 查询列表
│   ├── 字段配置（路由隐藏，由列表进入）
│   ├── 页面设计（路由隐藏）
│   └── 数据预览
└── 问卷管理
    ├── 问卷列表
    ├── 表单设计器（路由隐藏）
    └── 答卷/统计
```

**关键页面**

1. **查询列表**：新建、上传状态、发布状态、复制链接、二维码
2. **字段配置**：表格编辑查询控件类型、是否展示
3. **页面设计**：表单式配置 + 右侧实时预览
4. **问卷设计器**：
   - 左：题型组件面板
   - 中：画布拖拽排序（`vuedraggable`）
   - 右：题目属性面板

依赖建议：

- `xlsx` / `exceljs`：前端预览解析（真正解析入库以后端为准）
- `vuedraggable`：拖拽
- `qrcodejs2`：二维码

### 8.2 公开 H5

**方案推荐**：独立 `ruoyi-h5`（Vue3 + Vant + Vite），部署为静态资源或 Nginx 子路径 `/h5/`。

路由：

```text
/h5/q/:code     查询页
/h5/s/:code     问卷页
/h5/s/:code/ok  提交成功页
```

交互要点：

- 首屏只拉 `meta`，再按需 `search/submit`
- 查询结果用简易表格 / 卡片列表（小屏卡片）
- 问卷一页多题或一题一页（可配置，首期一页多题）

---

## 9. Excel 解析与查询实现要点

### 9.1 上传解析流程

```text
上传文件 → Common 文件存储 → Apache POI / EasyExcel 解析
  → 生成 field_key(c1..cn) → 批量写入 biz_query_field
  → 行转 JSONB 批量 insert biz_query_row
  → 更新 row_count
```

### 9.2 动态条件查询（JSONB）

示例（等值）：

```sql
select row_id, row_data
from biz_query_row
where query_id = #{queryId}
  and row_data ->> 'c1' = #{val}
order by row_no
limit #{limit} offset #{offset}
```

模糊：

```sql
and row_data ->> 'c1' ilike '%' || #{val} || '%'
```

数值/日期：入库时规范化类型，或查询时 `::numeric` / `::date` 转换（注意脏数据容错）。

### 9.3 权限

- 管理接口：登录 + `@PreAuthorize` + **数据归属**（只能操作自己的 `create_user_id`，管理员除外）
- 公开接口：校验 `status=已发布`、时间窗、访问密码、限流

---

## 10. 非功能需求

| 类别 | 要求 |
| :--- | :--- |
| 性能 | 公开查询 P95 < 800ms（2 万行内，带索引） |
| 安全 | XSS 过滤（问卷富文本慎用）、SQL 注入杜绝（禁止拼接字段名，field_key 白名单） |
| 可用 | 发布前强制预览；停用不删除历史答卷 |
| 审计 | 复用操作日志；公开提交记 IP/UA |
| 兼容 | 管理端 Chrome 最新两个版本；H5 iOS/Android 主流浏览器 |

---

## 11. 分期开发计划

### Phase 0 — 工程骨架（约 2~3 天）

- [ ] 新建 `ruoyi-biz` 模块并接入 `ruoyi-admin`
- [ ] 建表 SQL（PostgreSQL）与菜单权限 SQL
- [ ] Security 放行 `/open/**`
- [ ] 管理端空菜单与路由打通

### Phase 1 — 查询 MVP（约 1.5~2 周）

- [ ] 查询 CRUD + Excel 上传解析入库
- [ ] 字段配置 + 页面基础设计
- [ ] 发布短链 + 公开查询 API
- [ ] H5/自适应查询页
- [ ] 管理端复制链接

### Phase 2 — 问卷 MVP（约 1.5~2 周）

- [ ] 问卷 CRUD + 拖拽设计器（基础 5 题型）
- [ ] 发布 + 公开提交
- [ ] 答卷列表
- [ ] H5 填写页

### Phase 3 — 增强（约 1~2 周）

- [ ] 问卷统计图表
- [ ] 查询结果导出
- [ ] 访问密码、截止时间、提交上限
- [ ] 二维码、访问统计
- [ ] Excel 追加/覆盖更新

### Phase 4 — 优化（持续）

- [ ] 逻辑跳题
- [ ] 更大文件异步解析（消息队列/线程池）
- [ ] JSONB → 物理分表评估
- [ ] 管理端主题模板市场（可选）

---

## 12. 目录与命名约定

| 类型 | 约定 |
| :--- | :--- |
| Java 包 | `com.ruoyi.biz.*` |
| 表名 | `biz_*` |
| 权限标识 | `biz:query:list` / `biz:survey:add` 等 |
| 前端 API | `src/api/biz/query.js` · `src/api/biz/survey.js` |
| 字典类型 | `biz_query_status` · `biz_survey_status` · `biz_question_type` |

---

## 13. 验收标准（MVP）

### 查询

1. 用户注册登录后可创建查询并上传 Excel，正确识别表头
2. 可勾选查询字段与结果字段，保存后预览生效
3. 发布后生成链接，无登录可打开并按条件查出正确数据
4. 手机浏览器可正常使用查询页

### 问卷

1. 可通过拖拽完成至少 5 种基础题型设计并保存
2. 发布后无登录可填写提交
3. 创建者可在后台看到答卷记录
4. 手机浏览器可正常填写

### 安全

1. 未发布/已停用链接不可访问
2. 用户 A 无法通过接口操作用户 B 的项目
3. 公开接口具备基础限流

---

## 14. 风险与对策

| 风险 | 对策 |
| :--- | :--- |
| Excel 脏数据导致查询异常 | 解析阶段类型清洗；查询容错；错误行报告 |
| JSONB 大数据量变慢 | 限制行数；GIN 索引；后期动态表 |
| 公开接口被刷 | Redis 限流 + 验证码 + 监控告警 |
| 拖拽设计器复杂度高 | 首期只做排序+属性面板，不做自由画布绝对定位 |
| H5 与管理端两套前端成本 | MVP 可先用 ruoyi-ui 公开路由响应式顶上，再拆 h5 |

---

## 15. 即时技术决策（已拍板建议）

| 决策点 | 建议 |
| :--- | :--- |
| Excel 数据存储 | 首期 `biz_query_row.row_data JSONB` |
| 公开前端 | 优先独立 `ruoyi-h5`；若赶工可先做响应式公开页 |
| 表单设计器 | `vuedraggable` + 左侧组件/右侧属性 |
| 短链 | 随机 `public_code`，不用自增 ID 暴露 |
| 权限模型 | 登录用户只管理自己的数据；`admin` 可管理全部 |

---

## 16. 下一步行动

1. 评审本文档，确认范围与分期
2. 输出 `sql/biz_postgresql.sql`（建表 + 菜单）
3. 搭建 `ruoyi-biz` 模块骨架
4. 启动 Phase 1 查询 MVP 开发

---

## 附录 A · 状态机

**查询状态**

```text
草稿 --发布--> 已发布 --停用--> 停用
                ^                 |
                └------重新发布────┘
```

**问卷状态**

```text
草稿 --发布--> 已发布 --停用--> 停用
                │
                ├--到截止时间/达上限--> 已结束
                └--停用后可再发布（未结束时）
```

## 附录 B · 页面设计 JSON 示例

```json
{
  "title": "2026 成绩查询",
  "subtitle": "请输入姓名与考号",
  "themeColor": "#1677ff",
  "showSerial": true,
  "resultLayout": "table"
}
```

## 附录 C · 题目 props 示例

```json
{
  "placeholder": "请输入手机号",
  "maxLength": 11,
  "pattern": "^1\\d{10}$",
  "patternMessage": "手机号格式不正确"
}
```

---

*本文档随迭代更新。重大范围变更请修改版本号并同步评审。*
