package com.weightagent.app.data.cos

import android.content.Context
import com.tencent.cos.xml.CosXmlService
import com.tencent.cos.xml.CosXmlServiceConfig
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider
import com.weightagent.app.data.settings.CosSettings

object CosClientFactory {

    fun createService(context: Context, settings: CosSettings): CosXmlService {
        val credentialProvider = ShortTimeCredentialProvider(
            settings.secretId,
            settings.secretKey,
            300,
        )
        val serviceConfig = CosXmlServiceConfig.Builder()
            .setRegion(settings.region.trim())
            .isHttps(true)
            .builder()
        return CosXmlService(context.applicationContext, serviceConfig, credentialProvider)
    }
}
