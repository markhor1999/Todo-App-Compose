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
import java.security.MessageDigest

@VoiceKitInternalApi
public class ModelStore constructor(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /** Overrides where model files land. Null uses the app's internal storage. */
    private val rootOverride: File? = null,
    /**
     * Where bytes come from. Defaults to the public mirrors; a licensed build passes a
     * [GatedModelSource] so downloads are authenticated. See `LICENSING.md`.
     */
    private val source: ModelSource = DirectModelSource,
) {
    private val modelsRoot: File
        get() = (rootOverride ?: File(context.filesDir, "models")).apply { mkdirs() }

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
            val resolved = source.resolve(spec, fileSpec)
            downloadFile(resolved, part) { written ->
                emit(((doneBytes + written).toFloat() / totalBytes).coerceIn(0f, 1f))
            }
            check(part.length() == fileSpec.sizeBytes) {
                "Size mismatch for ${fileSpec.fileName}: ${part.length()} vs ${fileSpec.sizeBytes}"
            }
            verifyDigest(part, fileSpec)
            if (destination.exists()) destination.delete()
            check(part.renameTo(destination)) { "Could not move ${part.name} into place" }
            doneBytes += fileSpec.sizeBytes
            emit((doneBytes.toFloat() / totalBytes).coerceIn(0f, 1f))
        }
    }.flowOn(ioDispatcher)

    /**
     * Verifies the content hash when the catalog carries one.
     *
     * Size alone is a weak integrity check: a truncated-then-padded file, a substituted model, or a
     * mirror serving the wrong revision all pass it. A wrong model does not fail loudly — it decodes
     * to nonsense, which is far more expensive to diagnose than a failed download.
     *
     * [ModelFileSpec.sha256] is null for entries still served from public mirrors, whose digests we
     * have not pinned. Those keep the size-only check they always had; this never weakens anything.
     */
    private fun verifyDigest(part: File, fileSpec: ModelFileSpec) {
        val expected = fileSpec.sha256 ?: return
        val digest = MessageDigest.getInstance("SHA-256")
        part.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        check(actual.equals(expected, ignoreCase = true)) {
            "Checksum mismatch for ${fileSpec.fileName}: got $actual, expected $expected"
        }
    }

    private suspend fun downloadFile(
        resolved: ResolvedDownload,
        destination: File,
        emitWritten: suspend (Long) -> Unit,
    ) {
        val url = resolved.url
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 30_000
            // The distribution endpoint 302s to a signed storage URL. Following it here is the whole
            // protocol — the signed URL carries its own authorization, so the header being dropped
            // across hosts (correct behaviour) costs nothing.
            instanceFollowRedirects = true
            resolved.headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        try {
            // 401/403 from the distribution endpoint means the licence was refused, and saying so
            // beats a generic HTTP error the developer has to go and look up.
            check(connection.responseCode !in listOf(401, 403)) {
                "Model download refused (HTTP ${connection.responseCode}) — the licence key was " +
                    "rejected for ${fileNameOf(url)}. Check VoiceKit.license."
            }
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

    private fun fileNameOf(url: String): String = url.substringAfterLast('/').substringBefore('?')

    /** Deletes [spec]'s files. The shared VAD model is left alone — other models need it. */
    fun remove(spec: AsrModelSpec) {
        modelDir(spec).deleteRecursively()
    }
}
