package com.weightagent.app.data.cloud

/**
 * 用户选择的「上传到哪儿」顶层类型。
 *
 * - [OBJECT_COS]：公有云对象存储（当前实现：腾讯云 COS）
 * - [CONSUMER_ALIYUN_DRIVE]：消费级网盘（当前实现：阿里云盘 OpenAPI + refresh_token）
 */
enum class CloudStorageKind {
    OBJECT_COS,
    CONSUMER_ALIYUN_DRIVE,
}
