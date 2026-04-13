package com.weightagent.app.data.aliyundrive

import com.weightagent.app.data.settings.aliyundrive.AliyunDriveSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 阿里云盘 Web API（refresh_token），单分片上传；适用于常见录音体积。
 *
 * 官方与接口域名可能变更；若失败请核对 refresh_token 与开放平台说明。
 */
class AliyunDriveRepository {

    private val http = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    data class Session(
        val accessToken: String,
        val driveId: String,
    )

    suspend fun refreshSession(settings: AliyunDriveSettings): Session = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("grant_type", "refresh_token")
            .put("refresh_token", settings.refreshToken.trim())
            .toString()
            .toRequestBody(jsonMedia)
        val req = Request.Builder()
            .url("https://api.aliyundrive.com/v2/account/token")
            .post(body)
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("获取访问令牌失败（${resp.code}）：${text.take(200)}")
            }
            val jo = JSONObject(text)
            val access = jo.optString("access_token", "")
            val driveId = jo.optString("default_drive_id", "")
            if (access.isBlank()) {
                throw IllegalStateException("返回中无 access_token，请检查 refresh_token 是否有效")
            }
            if (driveId.isBlank()) {
                throw IllegalStateException("返回中无 default_drive_id")
            }
            Session(accessToken = access, driveId = driveId)
        }
    }

    suspend fun uploadFile(
        settings: AliyunDriveSettings,
        localFile: File,
        /** 网盘内相对路径，如 `recordings/xxx.m4a`（不含前导 /） */
        remoteRelativePath: String,
    ): String? = withContext(Dispatchers.IO) {
        val session = refreshSession(settings)
        val normalized = remoteRelativePath.trim().removePrefix("/")
        val slash = normalized.lastIndexOf('/')
        val parentPath = if (slash >= 0) normalized.substring(0, slash) else ""
        val fileName = if (slash >= 0) normalized.substring(slash + 1) else normalized
        if (fileName.isBlank()) throw IllegalStateException("无效远端路径")

        val parentFileId = ensureFolderPath(session, parentPath)
        val size = localFile.length()
        if (size <= 0L) throw IllegalStateException("本地文件大小为 0")

        val partList = JSONArray().put(
            JSONObject().put("part_number", 1).put("part_size", size),
        )
        val createBody = JSONObject()
            .put("drive_id", session.driveId)
            .put("parent_file_id", parentFileId)
            .put("name", fileName)
            .put("type", "file")
            .put("check_name_mode", "ignore")
            .put("size", size)
            .put("part_info_list", partList)
            .toString()
            .toRequestBody(jsonMedia)

        val createReq = Request.Builder()
            .url("https://api.aliyundrive.com/adrive/v2/file/createWithFolders")
            .header("Authorization", "Bearer ${session.accessToken}")
            .post(createBody)
            .build()

        val createJson = http.newCall(createReq).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("创建远端文件失败（${resp.code}）：${text.take(300)}")
            }
            JSONObject(text)
        }

        val fileId = createJson.optString("file_id", "")
        val uploadId = createJson.optString("upload_id", "")
        val parts = createJson.optJSONArray("part_info_list") ?: JSONArray()
        if (fileId.isBlank() || uploadId.isBlank() || parts.length() < 1) {
            throw IllegalStateException("创建文件返回数据不完整")
        }
        val part0 = parts.getJSONObject(0)
        val uploadUrl = part0.optString("upload_url", "")
        if (uploadUrl.isBlank()) throw IllegalStateException("无分片上传地址")

        val putReq = Request.Builder()
            .url(uploadUrl)
            .put(localFile.asRequestBody(null))
            .build()
        val etag = http.newCall(putReq).execute().use { putResp ->
            if (!putResp.isSuccessful) {
                val err = putResp.body?.string().orEmpty()
                throw IllegalStateException("上传分片失败（${putResp.code}）：${err.take(200)}")
            }
            putResp.header("ETag")?.trim('"')?.trim().orEmpty()
        }

        val completeParts = JSONArray().put(
            JSONObject()
                .put("part_number", 1)
                .put("etag", etag),
        )
        val completeBody = JSONObject()
            .put("drive_id", session.driveId)
            .put("file_id", fileId)
            .put("upload_id", uploadId)
            .put("part_info_list", completeParts)
            .toString()
            .toRequestBody(jsonMedia)

        val completeReq = Request.Builder()
            .url("https://api.aliyundrive.com/v2/file/complete")
            .header("Authorization", "Bearer ${session.accessToken}")
            .post(completeBody)
            .build()

        http.newCall(completeReq).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("完成上传失败（${resp.code}）：${text.take(300)}")
            }
        }
        fileId
    }

    /** 从 root 起按路径逐级创建文件夹，返回最后一级 folder 的 file_id；空路径返回 root */
    private fun ensureFolderPath(session: Session, path: String): String {
        var parentId = "root"
        val segments = path.split('/').map { it.trim() }.filter { it.isNotEmpty() }
        for (name in segments) {
            parentId = findOrCreateFolder(session, parentId, name)
        }
        return parentId
    }

    private fun findOrCreateFolder(session: Session, parentFileId: String, folderName: String): String {
        val listBody = JSONObject()
            .put("drive_id", session.driveId)
            .put("parent_file_id", parentFileId)
            .put("limit", 200)
            .put("all", false)
            .put("order_by", "name")
            .put("order_direction", "ASC")
            .toString()
            .toRequestBody(jsonMedia)

        val listReq = Request.Builder()
            .url("https://api.aliyundrive.com/adrive/v3/file/list")
            .header("Authorization", "Bearer ${session.accessToken}")
            .post(listBody)
            .build()

        http.newCall(listReq).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("列举目录失败（${resp.code}）：${text.take(200)}")
            }
            val items = JSONObject(text).optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val it = items.getJSONObject(i)
                if (it.optString("type") == "folder" && it.optString("name") == folderName) {
                    return it.optString("file_id")
                }
            }
        }

        val createFolderBody = JSONObject()
            .put("drive_id", session.driveId)
            .put("parent_file_id", parentFileId)
            .put("name", folderName)
            .put("type", "folder")
            .put("check_name_mode", "ignore")
            .toString()
            .toRequestBody(jsonMedia)

        val createReq = Request.Builder()
            .url("https://api.aliyundrive.com/adrive/v2/file/createWithFolders")
            .header("Authorization", "Bearer ${session.accessToken}")
            .post(createFolderBody)
            .build()

        return http.newCall(createReq).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IllegalStateException("创建文件夹失败（${resp.code}）：${text.take(200)}")
            }
            JSONObject(text).optString("file_id").takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("创建文件夹未返回 file_id")
        }
    }
}
