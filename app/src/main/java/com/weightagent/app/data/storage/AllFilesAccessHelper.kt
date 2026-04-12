package com.weightagent.app.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
object AllFilesAccessHelper {

    fun hasAllFilesAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return true
        }
        return Environment.isExternalStorageManager()
    }

    fun openManageAllFilesSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return
        }
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            context.startActivity(fallback)
        }
    }
}
