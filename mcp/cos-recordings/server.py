#!/usr/bin/env python3
"""
weight-agent 通用腾讯云 COS MCP（stdio）

供 Cursor、Claude Desktop 等支持 MCP 的 Host 使用：列对象、生成预签名下载链接。
凭证一律从环境变量读取，勿写入仓库。

环境变量（必填）:
  COS_SECRET_ID      或 TENCENTCLOUD_SECRET_ID
  COS_SECRET_KEY     或 TENCENTCLOUD_SECRET_KEY
  COS_REGION         如 ap-guangzhou、ap-nanjing
  COS_BUCKET         完整桶名，如 mybucket-1250000000

可选:
  COS_PREFIX         对象前缀，默认 recordings/（须以 / 结尾便于列举）
"""

from __future__ import annotations

import json
import os
import sys
from typing import Any

from mcp.server.fastmcp import FastMCP
from qcloud_cos import CosConfig, CosS3Client
from qcloud_cos.cos_exception import CosClientError, CosServiceError

mcp = FastMCP(
    "weight-agent-cos",
    instructions=(
        "腾讯云 COS 只读工具：列举桶内对象、生成限时下载链接。"
        " object_key 须位于配置的 COS_PREFIX 之下。"
    ),
)


def _secret_id() -> str:
    return (
        os.environ.get("COS_SECRET_ID", "").strip()
        or os.environ.get("TENCENTCLOUD_SECRET_ID", "").strip()
    )


def _secret_key() -> str:
    return (
        os.environ.get("COS_SECRET_KEY", "").strip()
        or os.environ.get("TENCENTCLOUD_SECRET_KEY", "").strip()
    )


def _region() -> str:
    return os.environ.get("COS_REGION", "").strip()


def _bucket() -> str:
    return os.environ.get("COS_BUCKET", "").strip()


def _prefix() -> str:
    p = os.environ.get("COS_PREFIX", "recordings/").strip()
    if not p:
        return ""
    return p if p.endswith("/") else f"{p}/"


def _client() -> CosS3Client:
    sid, sk, reg, bkt = _secret_id(), _secret_key(), _region(), _bucket()
    if not sid or not sk or not reg or not bkt:
        raise RuntimeError(
            "缺少 COS 环境变量：请设置 COS_SECRET_ID、COS_SECRET_KEY、COS_REGION、COS_BUCKET"
            "（或 TENCENTCLOUD_SECRET_ID / TENCENTCLOUD_SECRET_KEY）。"
        )
    cfg = CosConfig(Region=reg, SecretId=sid, SecretKey=sk, Scheme="https")
    return CosS3Client(cfg)


def _normalize_key(user_key: str) -> str:
    k = user_key.strip().lstrip("/")
    if not k:
        raise ValueError("object_key 不能为空")
    return k


def _assert_key_allowed(key: str) -> None:
    prefix = _prefix()
    if not prefix:
        return
    if not (key == prefix.rstrip("/") or key.startswith(prefix)):
        raise ValueError(f"object_key 必须以配置的 COS_PREFIX 为前缀：{prefix!r}")


@mcp.tool()
def cos_list_objects(
    max_keys: int = 50,
    continuation_token: str = "",
) -> str:
    """列出 COS_BUCKET 中 COS_PREFIX 下的对象（最多 max_keys 条）。返回 JSON：keys、truncated、next_token。"""
    max_keys = max(1, min(max_keys, 1000))
    client = _client()
    bucket = _bucket()
    prefix = _prefix()
    kwargs: dict[str, Any] = {
        "Bucket": bucket,
        "Prefix": prefix,
        "MaxKeys": max_keys,
    }
    if continuation_token.strip():
        kwargs["Marker"] = continuation_token.strip()
    try:
        resp = client.list_objects(**kwargs)
    except (CosClientError, CosServiceError) as e:
        return json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False)

    contents = resp.get("Contents") or []
    items = []
    for c in contents:
        items.append(
            {
                "Key": c.get("Key"),
                "Size": c.get("Size"),
                "LastModified": str(c.get("LastModified", "")),
            }
        )
    is_truncated = bool(resp.get("IsTruncated"))
    next_marker = resp.get("NextMarker") or ""
    return json.dumps(
        {
            "ok": True,
            "prefix": prefix,
            "bucket": bucket,
            "count": len(items),
            "is_truncated": is_truncated,
            "next_marker": next_marker,
            "objects": items,
        },
        ensure_ascii=False,
        indent=2,
    )


@mcp.tool()
def cos_presigned_download_url(
    object_key: str,
    expires_seconds: int = 3600,
) -> str:
    """为桶内 object_key 生成 HTTPS GET 预签名下载链接（默认 1 小时有效）。object_key 须在 COS_PREFIX 下。"""
    key = _normalize_key(object_key)
    _assert_key_allowed(key)
    expires_seconds = max(60, min(int(expires_seconds), 24 * 3600))
    client = _client()
    bucket = _bucket()
    try:
        url = client.get_presigned_url(
            Method="GET",
            Bucket=bucket,
            Key=key,
            Expired=expires_seconds,
        )
    except (CosClientError, CosServiceError) as e:
        return json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False)
    return json.dumps(
        {
            "ok": True,
            "object_key": key,
            "expires_seconds": expires_seconds,
            "url": url,
        },
        ensure_ascii=False,
        indent=2,
    )


@mcp.tool()
def cos_head_bucket() -> str:
    """探测当前凭证能否访问 COS_BUCKET（不列举对象）。"""
    client = _client()
    bucket = _bucket()
    try:
        client.head_bucket(Bucket=bucket)
    except (CosClientError, CosServiceError) as e:
        return json.dumps({"ok": False, "error": str(e)}, ensure_ascii=False)
    return json.dumps(
        {
            "ok": True,
            "bucket": bucket,
            "region": _region(),
            "prefix": _prefix(),
        },
        ensure_ascii=False,
        indent=2,
    )


def main() -> None:
    # stdio：由 Cursor / 其他 Host 拉起子进程
    mcp.run()


if __name__ == "__main__":
    main()
