# 腾讯云 COS MCP（通用）

与 **weight-agent Android 应用** 使用同一套桶配置：在任意支持 **MCP** 的 Host（Cursor、Claude Desktop 等）里 **只读** 列举对象、生成 **预签名下载 URL**。

**给人 / 给 Agent 的完整步骤与可复制链接**：请先读仓库 **`docs/MCP_COS.md`**（README 文档索引里也有入口）。

## 安装（本机）

```bash
cd mcp/cos-recordings
python3 -m venv .venv
source .venv/bin/activate   # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

## 环境变量

| 变量 | 说明 |
|------|------|
| `COS_SECRET_ID` | 必填；也可用 `TENCENTCLOUD_SECRET_ID` |
| `COS_SECRET_KEY` | 必填；也可用 `TENCENTCLOUD_SECRET_KEY` |
| `COS_REGION` | 必填，如 `ap-nanjing` |
| `COS_BUCKET` | 必填，完整桶名 |
| `COS_PREFIX` | 可选，默认 `recordings/`，列举与下载 key 必须在此前缀下 |

子账号策略需包含对该桶的 **HeadBucket、ListBucket、GetObject**（生成预签名 GET 需要读权限）。

## Cursor 配置

1. 复制仓库内 **`.cursor/mcp.json.example`** 为 **`.cursor/mcp.json`**（或写入全局 `~/.cursor/mcp.json`）。
2. 把 `command` 改成你本机 **虚拟环境里 `python` 的绝对路径`**，或保持 `python3` 并确保 PATH 能找到已安装依赖的解释器。
3. 在 `env` 或 `envFile` 中填入密钥（**勿提交 `mcp.json` / `.env` 含真实密钥**）。
4. 重启 Cursor，在 MCP 日志中确认 `weight-agent-cos` 已连接。

## 暴露的工具（Tools）

- **`cos_head_bucket`**：探测凭证与桶是否可用。
- **`cos_list_objects`**：按前缀列举对象（JSON）。
- **`cos_presigned_download_url`**：为指定 `object_key` 生成限时 HTTPS 下载链接。

## 安全

- 本进程**不写入** COS，仅列举与预签名读。
- 预签名链接在 `expires_seconds` 内有效，勿转发给不可信方。

## 手动冒烟

```bash
export COS_SECRET_ID=... COS_SECRET_KEY=... COS_REGION=... COS_BUCKET=...
source .venv/bin/activate
python server.py
```

若 Host 未连接，进程会阻塞读 stdin，属正常现象；用 Cursor 连接即可。
