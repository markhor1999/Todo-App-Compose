package com.codingwithsalman.voicenotes.core.media

import android.media.MediaMetadataRetriever
import java.io.File
import javax.inject.Inject

class AudioProbe @Inject constructor() {

    /** Duration in ms, or 0 when the container doesn't report one. */
    fun durationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }
}
