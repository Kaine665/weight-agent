package com.weightagent.app.data.cos

import android.content.Context
import com.tencent.cos.xml.exception.CosXmlClientException
import com.tencent.cos.xml.exception.CosXmlServiceException
import com.tencent.cos.xml.listener.CosXmlResultListener
import com.tencent.cos.xml.model.CosXmlRequest
import com.tencent.cos.xml.model.CosXmlResult
import com.tencent.cos.xml.model.bucket.HeadBucketRequest
import com.tencent.cos.xml.transfer.COSXMLUploadTask
import com.tencent.cos.xml.transfer.TransferConfig
import com.tencent.cos.xml.transfer.TransferManager
import com.weightagent.app.data.settings.CosSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CosRepository(private val context: Context) {

    suspend fun headBucket(settings: CosSettings): Unit = withContext(Dispatchers.IO) {
        val cosXmlService = CosClientFactory.createService(context, settings)
        val request = HeadBucketRequest(settings.bucket.trim())
        suspendCancellableCoroutine { cont ->
            cosXmlService.headBucketAsync(request, object : CosXmlResultListener {
                override fun onSuccess(request: CosXmlRequest, result: CosXmlResult) {
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onFail(
                    request: CosXmlRequest,
                    clientException: CosXmlClientException?,
                    serviceException: CosXmlServiceException?,
                ) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            clientException ?: serviceException ?: IllegalStateException("未知错误"),
                        )
                    }
                }
            })
        }
    }

    suspend fun uploadFile(
        settings: CosSettings,
        localAbsolutePath: String,
        objectKey: String,
    ): UploadOutcome = withContext(Dispatchers.IO) {
        val cosXmlService = CosClientFactory.createService(context, settings)
        val transferConfig = TransferConfig.Builder()
            .setForceSimpleUpload(true)
            .build()
        val transferManager = TransferManager(cosXmlService, transferConfig)
        suspendCancellableCoroutine { cont ->
            val task: COSXMLUploadTask = transferManager.upload(
                settings.bucket.trim(),
                objectKey,
                localAbsolutePath,
                null,
            )
            task.setCosXmlResultListener(object : CosXmlResultListener {
                override fun onSuccess(request: CosXmlRequest, result: CosXmlResult) {
                    val uploadResult = result as? COSXMLUploadTask.COSXMLUploadTaskResult
                    val etag = uploadResult?.eTag
                    if (cont.isActive) {
                        cont.resume(
                            UploadOutcome(
                                etag = etag,
                            ),
                        )
                    }
                }

                override fun onFail(
                    request: CosXmlRequest,
                    clientException: CosXmlClientException?,
                    serviceException: CosXmlServiceException?,
                ) {
                    if (cont.isActive) {
                        cont.resumeWithException(
                            clientException ?: serviceException ?: IllegalStateException("上传失败"),
                        )
                    }
                }
            })
            cont.invokeOnCancellation {
                task.cancel()
            }
        }
    }

    data class UploadOutcome(
        val etag: String?,
    )
}
