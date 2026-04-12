package com.weightagent.app.data.mediastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaomiPrivateRecorderScannerIdTest {

    @Test
    fun syntheticId_stableForPath() {
        val p = "/storage/emulated/0/Android/data/com.android.soundrecorder/files/foo.m4a"
        val a = XiaomiPrivateRecorderScanner.syntheticIdForRawPath(p)
        val b = XiaomiPrivateRecorderScanner.syntheticIdForRawPath(p)
        assertEquals(a, b)
        assertTrue(XiaomiPrivateRecorderScanner.isRawFileSyntheticId(a))
    }
}
