package com.weightagent.app.data.mediastore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPathFiltersTest {

    @Test
    fun skipsFeishuPath() {
        assertTrue(
            MediaPathFilters.shouldSkipMediaPath(
                "Download/feishu_recordings/",
                null,
                "foo.m4a",
            ),
        )
    }

    @Test
    fun keepsMiuiRecorderPath() {
        assertFalse(
            MediaPathFilters.shouldSkipMediaPath(
                "MIUI/sound_recorder/",
                null,
                "20240101.m4a",
            ),
        )
    }
}
