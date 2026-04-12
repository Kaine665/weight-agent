package com.weightagent.app.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CosSettingsTest {

    @Test
    fun normalizedPrefix_trimsAndEnsuresTrailingSlash() {
        val s = CosSettings("a", "b", "c", "d", "  recordings  ")
        assertEquals("recordings/", s.normalizedPrefix)
    }

    @Test
    fun isComplete_requiresCoreFields() {
        assertTrue(
            CosSettings("id", "key", "ap-guangzhou", "bkt-125", "p/").isComplete(),
        )
    }
}
