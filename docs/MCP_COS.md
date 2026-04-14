# weight-agent · 腾讯云 COS MCP 使用说明

本页说明：**如何在电脑上启用本仓库自带的 MCP**，让支持 MCP 的 Agent（如 Cursor）能**列举桶内录音、生成限时下载链接**。与 Android App 上传到**同一 COS 桶、同一前缀**即可配合使用。

---

## 给 Agent 的一句话（复制用）

你可以把下面整段发给任意能联网、能读 URL 的 Agent（或 Cursor 里 @ 文档）：

> 请阅读 weight-agent 仓库里的 COS MCP 说明：  
> **https://github.com/Kaine665/weight-agent/blob/main/docs/MCP_COS.md**  
> 按文档在 **本机** 完成：克隆或打开仓库、`mcp/cos-recordings` 下安装依赖、配置 Cursor 的 **`mcp.json`**（密钥用环境变量，勿写入 Git）。MCP 代码路径：**`mcp/cos-recordings/server.py`**，配置模板：**`.cursor/mcp.json.example`**。

**说明**：Agent 只能根据文档**指导你或生成配置**；**真正连上 COS** 仍须在你电脑上的 Cursor（或其它 Host）里**写好 `mcp.json` 并重启**，无法仅凭「知道 GitHub 地址」就自动获得你桶的访问权（否则等于密钥泄露）。

---

## 文档与代码位置（地址一览）

| 用途 | GitHub 上路径（`main` 分支） |
|------|------------------------------|
| **本使用说明（网页）** | `https://github.com/Kaine665/weight-agent/blob/main/docs/MCP_COS.md` |
| **本页 Raw（便于复制/部分工具抓取）** | `https://raw.githubusercontent.com/Kaine665/weight-agent/main/docs/MCP_COS.md` |
| **MCP 源码** | 仓库内 `mcp/cos-recordings/server.py` |
| **Python 依赖** | `mcp/cos-recordings/requirements.txt` |
| **Cursor 配置模板** | `.cursor/mcp.json.example`（复制为 `.cursor/mcp.json` 再改） |

本地克隆后，上述路径相对于**仓库根目录**。

---

## 你需要准备什么

1. **已能上传的 COS**：与 App 里一致的 **SecretId / SecretKey、region、bucket、prefix**（默认前缀多为 `recordings/`）。
2. **本机 Python 3.10+**。
3. **Cursor**（或其它支持 **stdio MCP** 的客户端）。

子账号策略建议至少包含对该桶的 **HeadBucket、ListBucket、GetObject**（预签名下载依赖读权限）。

---

## 安装步骤（本机一次）

在**仓库根目录**执行：

```bash
cd mcp/cos-recordings
python3 -m venv .venv
source .venv/bin/activate
# Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

记下虚拟环境里 **`python` 的绝对路径**（后面写进 `mcp.json` 的 `command` 最稳）。

---

## 环境变量（必填 / 可选）

| 变量 | 必填 | 说明 |
|------|------|------|
| `COS_SECRET_ID` | 是* | 也可用 `TENCENTCLOUD_SECRET_ID` |
| `COS_SECRET_KEY` | 是* | 也可用 `TENCENTCLOUD_SECRET_KEY` |
| `COS_REGION` | 是 | 如 `ap-nanjing` |
| `COS_BUCKET` | 是 | 完整桶名，如 `audio-record-1357780693` |
| `COS_PREFIX` | 否 | 默认 `recordings/`；列举与下载的 key **必须**以此前缀开头 |

\* 二选一命名即可。

---

## 接入 Cursor（推荐）

1. 复制 **`.cursor/mcp.json.example`** → **`.cursor/mcp.json`**（或写入用户目录 **`~/.cursor/mcp.json`** 做全局生效）。
2. 修改其中：
   - **`command`**：改为上一步 venv 里 **`python` 的绝对路径**（Windows 用反斜杠或正斜杠均可）。
   - **`args`**：保持指向 **`${workspaceFolder}/mcp/cos-recordings/server.py`**（在 Cursor 里打开本仓库时 `${workspaceFolder}` 即根目录）。
   - **`env`**：填入真实 `COS_*`，或使用 **`envFile`** 指向本机 `.env`（**勿把含密钥的 `mcp.json` / `.env` 提交 Git**）。
3. **完全退出并重启 Cursor**。
4. 打开 **输出 → MCP**，确认 **`weight-agent-cos`** 无报错。

官方 Cursor 文档：**[MCP 配置](https://cursor.com/docs/cookbook/building-mcp-server)**（stdio、`envFile` 等以最新页为准）。

---

## MCP 提供的工具（Tools）

| 工具名 | 作用 |
|--------|------|
| `cos_head_bucket` | 探测凭证与桶是否可用 |
| `cos_list_objects` | 按 `COS_PREFIX` 列举对象（JSON） |
| `cos_presigned_download_url` | 为 `object_key` 生成 **限时 HTTPS** 下载链接（key 须在前缀下） |

与 App 上传的对象键规则一致时，列出的 **Key** 即为 App 已同步到桶内的路径。

---

## 安全与限制

- MCP 进程为 **只读**（列举 + 预签名 GET），不修改桶内对象。
- **预签名 URL 在有效期内等同临时下载权**，勿发到不可信渠道。
- 本 MCP **不提供** STS、不提供账号密码登录网页；仅 **环境变量里的长期密钥** 或你自行扩展的临时密钥（需改代码）。

---

## 故障排查

| 现象 | 建议 |
|------|------|
| MCP 日志里 import 失败 | 确认在 **同一 venv** 下执行过 `pip install -r requirements.txt`，且 `mcp.json` 的 `command` 指向该 venv 的 `python`。 |
| `403` / 鉴权失败 | 检查子账号策略、桶名、地域是否与控制台一致。 |
| 列举为空 | 检查 `COS_PREFIX` 与 App 里 **prefix** 是否一致（注意末尾 `/`）。 |

更细的实现说明见仓库 **`mcp/cos-recordings/README.md`**（与本文互补，以本页为「给 Agent / 给人」的总入口）。
