package com.tricodestudio.voicekit

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@VoiceKitInternalApi
public class ModelStore constructor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val modelsRoot: File
        get() = File(context.filesDir, "models").apply { mkdirs() }

    fun modelDir(spec: AsrModelSpec): File =
        File(modelsRoot, spec.id).apply { mkdirs() }

    fun localFile(spec: AsrModelSpec, file: ModelFileSpec): File =
        File(modelDir(spec), file.fileName)

    fun vadLocalFile(): File =
        File(File(modelsRoot, "vad").apply { mkdirs() }, ModelCatalog.vadFile.fileName)

    fun isInstalled(spec: AsrModelSpec): Boolean =
        spec.files.all { localFile(spec, it).length() == it.sizeBytes } &&
            vadLocalFile().length() == ModelCatalog.vadFile.sizeBytes

    /**
     * Downloads any missing files of [spec] (+ the shared VAD model), emitting overall
     * progress 0..1 by bytes. Files land via a .part temp + rename, so a killed
     * download never leaves a half-written file that passes the size check.
     */
    fun download(spec: AsrModelSpec): Flow<Float> = flow {
        val work: List<Pair<ModelFileSpec, File>> = buildList {
            spec.files.forEach { f ->
                if (localFile(spec, f).length() != f.sizeBytes) add(f to localFile(spec, f))
            }
            if (vadLocalFile().length() != ModelCatalog.vadFile.sizeBytes) {
                add(ModelCatalog.vadFile to vadLocalFile())
            }
        }
        val totalBytes = work.sumOf { it.first.sizeBytes }
        if (totalBytes == 0L) {
            emit(1f)
            return@flow
        }
        var doneBytes = 0L
        for ((fileSpec, destination) in work) {
            val part = File(destination.parentFile, destination.name + ".part")
            downloadFile(fileSpec.url, part) { written ->
                emit(((doneBytes + written).toFloat() / totalBytes).coerceIn(0f, 1f))
            }
            check(part.length() == fileSpec.sizeBytes) {
                "Size mismatch for ${fileSpec.fileName}: ${part.length()} vs ${fileSpec.sizeBytes}"
            }
            if (destination.exists()) destination.delete()
            check(part.renameTo(destination)) { "Could not move ${part.name} into place" }
            doneBytes += fileSpec.sizeBytes
            emit((doneBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
        }
    }.flowOn(ioDispatcher)

    private suspend fun downloadFile(
        url: String,
        destination: File,
        emitWritten: suspend (Long) -> Unit,
    ) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            check(connection.responseCode in 200..299) {
                "HTTP ${connection.responseCode} for $url"
            }
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    var sinceEmit = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        sinceEmit += read
                        if (sinceEmit >= 1_048_576L) { // once per MB keeps the Flow cheap
                            emitWritten(written)
                            sinceEmit = 0L
                        }
                    }
                    emitWritten(written)
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
