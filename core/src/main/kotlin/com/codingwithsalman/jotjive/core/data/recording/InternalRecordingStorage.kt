package com.codingwithsalman.jotjive.core.data.recording

import android.content.Context
import com.codingwithsalman.jotjive.core.domain.recording.RecordingStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.temporal.ChronoUnit

class InternalRecordingStorage(
    private val context: Context
) : RecordingStorage {

    override suspend fun savePersistently(tempFilePath: String): String? {
        val tempFile = File(tempFilePath)
        if (!tempFile.exists()) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val savedFile = generateSavedFile()
                tempFile.copyTo(savedFile)

                savedFile.absolutePath
            } catch (e: IOException) {
                e.printStackTrace()
                null
            } finally {
                withContext(NonCancellable) {
                    cleanUpTemporaryFiles()
                }
            }
        }
    }

    override suspend fun cleanUpTemporaryFiles() {
        withContext(Dispatchers.IO) {
            context
                .cacheDir
                .listFiles()
                ?.filter { it.name.startsWith(RecordingStorage.TEMP_FILE_PREFIX) }
                ?.forEach { file ->
                    file.delete()
                }
        }
    }

    private fun generateSavedFile(): File {
        val timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()
        return File(
            context.filesDir,
            "${RecordingStorage.PERSISTENT_FILE_PREFIX}_$timestamp.${RecordingStorage.RECORDING_FILE_EXTENSION}"
        )
    }
}