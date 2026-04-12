package com.weightagent.app.data.saf

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

/**
 * 为 [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree] 提供初始位置，
 * 尽量落在主存储 **Download** 等可授权目录，减少用户误入 `Android/data` 被系统拒绝。
 */
object SafOpenHelper {

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    /** 主存储「下载」目录的树 URI，供 SAF 选择器打开；失败时返回 null（由系统决定起始位置）。 */
    fun initialTreeUriForPicker(): Uri? =
        runCatching {
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_AUTHORITY,
                "primary:${Environment.DIRECTORY_DOWNLOADS}",
            )
        }.getOrNull()
}
