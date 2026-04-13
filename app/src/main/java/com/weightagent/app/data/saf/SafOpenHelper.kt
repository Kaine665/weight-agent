package com.weightagent.app.data.saf

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

/** 为 [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree] 提供初始位置（主存储 Download）。 */
object SafOpenHelper {

    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    fun initialTreeUriForPicker(): Uri? =
        runCatching {
            DocumentsContract.buildTreeDocumentUri(
                EXTERNAL_STORAGE_AUTHORITY,
                "primary:${Environment.DIRECTORY_DOWNLOADS}",
            )
        }.getOrNull()
}
