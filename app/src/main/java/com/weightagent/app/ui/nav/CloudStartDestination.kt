package com.weightagent.app.ui.nav

import com.weightagent.app.data.cloud.CloudStorageKind
import com.weightagent.app.di.AppContainer

/**
 * 冷启动导航：未选类型 → 选择页；已选但未配凭证 → 对应配置页；否则列表。
 */
suspend fun resolveCloudStartRoute(container: AppContainer): String {
    container.cloudStorageSelectionStore.migrateDefaultFromLegacyCosIfNeeded()
    val kind = container.cloudStorageSelectionStore.readKindOrNull()
    if (kind == null) {
        return Routes.CLOUD_SELECT
    }
    return when (kind) {
        CloudStorageKind.OBJECT_COS -> {
            if (container.cosSettingsStore.read()?.isComplete() == true) {
                Routes.LIST
            } else {
                Routes.CONFIG_COS
            }
        }
        CloudStorageKind.CONSUMER_ALIYUN_DRIVE -> {
            if (container.aliyunDriveSettingsStore.read()?.isComplete() == true) {
                Routes.LIST
            } else {
                Routes.CONFIG_ALIYUN_DRIVE
            }
        }
    }
}
