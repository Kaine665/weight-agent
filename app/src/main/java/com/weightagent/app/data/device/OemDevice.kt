package com.weightagent.app.data.device

import android.os.Build

object OemDevice {

    /** 小米 / 红米 / POCO 等，用于额外扫描系统录音机私有目录 */
    fun isXiaomiFamily(): Boolean {
        val m = Build.MANUFACTURER.lowercase()
        val b = Build.BRAND.lowercase()
        return m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") ||
            b.contains("xiaomi") || b.contains("redmi") || b.contains("poco")
    }
}
