package com.weightagent.app.data.mediastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreAudioScannerSyntheticIdTest {

    @Test
    fun syntheticId_isNegative_and_reversible() {
        val fileId = 42_001L
        val syn = MediaStoreAudioScanner.syntheticIdForFilesRow(fileId)
        assertTrue(syn < 0)
        assertTrue(MediaStoreAudioScanner.isSyntheticFilesId(syn))
        val back = -(syn) - 10_000_000_000L
        assertEquals(fileId, back)
    }
}
