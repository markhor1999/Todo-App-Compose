package com.codingwithsalman.voicenotes.core.media

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingsStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val recordingsDir: File
        get() = File(context.filesDir, "recordings").apply { mkdirs() }

    fun newRecordingFile(): File =
        File(recordingsDir, "rec_${System.currentTimeMillis()}.m4a")

    /** Destination for audio imported via SAF; extension preserved for the decoder. */
    fun newImportFile(extension: String): File =
        File(recordingsDir, "imp_${System.currentTimeMillis()}.${extension.trimStart('.')}")
}
