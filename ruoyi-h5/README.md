# ruoyi-h5

通查云独立公开 H5（Vue 3 + Vite），仅依赖后端公开接口：

- 查询：`/open/query/**`
- 问卷：`/open/survey/**`

管理端仍使用 `ruoyi-ui`；本工程用于更轻的移动端部署或独立域名。

## 路由

| 路径 | 说明 |
| :--- | :--- |
| `/` | 入口说明 |
| `/q/:code` | 公开查询 |
| `/s/:code` | 公开问卷 |

## 本地运行

1. 先启动后端 `8080`
2. 安装并启动 H5：

```bash
cd ruoyi-h5
npm install
npm run dev
```

默认开发地址：`http://127.0.0.1:5173`  
接口代理：`/dev-api` → `http://127.0.0.1:8080`

示例（若样例数据仍在）：

- http://127.0.0.1:5173/q/q6jjyg79
- http://127.0.0.1:5173/s/97vw7fqf

## 生产构建

子路径 `/h5/` 部署示例：

```bash
cp .env.production.example .env.production
npm run build
```

产物在 `dist/`。参考 `nginx.conf.example`：将 `dist/` 部署到 `/h5/`，并把 `/prod-api` 反代到后端 `8080`。

独立域名时可设 `VITE_BASE=/`、`VITE_APP_BASE_API=/prod-api`。

## 与 ruoyi-ui 公开页关系

`ruoyi-ui` 内 `/q`、`/s` 仍可用（管理端联调方便）。独立部署公开流量时优先使用本工程。
