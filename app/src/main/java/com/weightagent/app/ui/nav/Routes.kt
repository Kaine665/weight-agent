package com.weightagent.app.ui.nav

object Routes {
    const val LIST = "list"
    /** 选择对象存储 vs 消费级网盘 */
    const val CLOUD_SELECT = "cloud_select"
    /** 云端入口：查看当前方式、跳转各配置、更改上传方式 */
    const val CLOUD_HUB = "cloud_hub"
    /** 腾讯云 COS */
    const val CONFIG_COS = "config_cos"
    /** 阿里云盘 refresh_token */
    const val CONFIG_ALIYUN_DRIVE = "config_aliyun_drive"

    @Deprecated("使用 CONFIG_COS", ReplaceWith("Routes.CONFIG_COS"))
    const val CONFIG = CONFIG_COS
}
