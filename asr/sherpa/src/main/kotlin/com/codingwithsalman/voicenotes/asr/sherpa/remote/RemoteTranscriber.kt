@file:OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)

package com.codingwithsalman.voicenotes.asr.sherpa.remote

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.tricodestudio.voicekit.AsrModelSpec
import com.tricodestudio.voicekit.SherpaTranscriptionEngine
import com.tricodestudio.voicekit.TranscriptionResult
import com.tricodestudio.voicekit.UnsupportedDeviceException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Runs a transcription in the isolated `:asr` process and brings the answer back.
 *
 * **Failure policy, which is the whole reason this class exists:**
 * - The ASR process *dies* mid-run (SIGBUS, native OOM, LMK) → [AsrProcessDiedException]. The note
 *   fails and we do **not** retry in this process, because whatever killed that one would kill this
 *   one — and this one is the app.
 * - The service cannot be *bound* at all (manifest or system problem, not a crash) → fall back to
 *   in-process transcription. Losing isolation is bad; losing the feature outright is worse, and a
 *   bind failure carries none of the evidence that inference is dangerous on this device.
 *
 * See brain/apps/voicenotes/2026-09-01-crash-diagnosis-lowend-devices.md.
 */
@Singleton
class RemoteTranscriber @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: SherpaTranscriptionEngine,
) {

    suspend fun transcribe(
        audioFile: File,
        spec: AsrModelSpec,
        onProgress: (Float) -> Unit,
    ): TranscriptionResult {
        // Cheap local refusal first: no reason to spin up a process to be told no.
        engine.unsupportedReason(spec)?.let { throw UnsupportedDeviceException(it) }
        // Claim the local inference slot even though the work runs elsewhere. The live preview
        // holds this same slot in this process, and without it a live session and a remote
        // transcription would each allocate ~1 GB of native heap at the same time.
        return try {
            engine.withInferenceSlot { transcribeRemote(audioFile, spec, onProgress) }
        } catch (e: BindFailedException) {
            // engine.transcribe() takes the slot itself, so this is outside withInferenceSlot to
            // avoid deadlocking on a non-reentrant mutex.
            Log.w(TAG, "ASR process unavailable (${e.message}); running in-process this time")
            engine.transcribe(audioFile, spec, onProgress)
        }
    }

    private suspend fun transcribeRemote(
        audioFile: File,
        spec: AsrModelSpec,
        onProgress: (Float) -> Unit,
    ): TranscriptionResult = suspendCancellableCoroutine { cont ->
        val callbackThread = HandlerThread("asr-client").apply { start() }
        val done = AtomicBoolean(false)

        // Every exit path runs through here exactly once: resume the caller, unbind, stop the
        // thread. Getting this wrong leaks a binding per note, so it is deliberately one funnel.
        var connection: ServiceConnection? = null
        fun finish(block: () -> Unit) {
            if (!done.compareAndSet(false, true)) return
            connection?.let { runCatching { context.unbindService(it) } }
            callbackThread.quitSafely()
            block()
        }

        val incoming = Messenger(object : Handler(callbackThread.looper) {
            override fun handleMessage(msg: Message) {
                val data: Bundle = msg.data ?: Bundle()
                when (msg.what) {
                    AsrProtocol.MSG_PROGRESS ->
                        onProgress(data.getFloat(AsrProtocol.KEY_PROGRESS, 0f))

                    AsrProtocol.MSG_RESULT -> finish {
                        cont.resume(
                            TranscriptionResult(
                                segments = AsrProtocol.decodeSegments(data),
                                language = data.getString(AsrProtocol.KEY_LANGUAGE),
                                durationMs = data.getLong(AsrProtocol.KEY_DURATION_MS),
                            )
                        )
                    }

                    AsrProtocol.MSG_ERROR -> finish {
                        val text = data.getString(AsrProtocol.KEY_ERROR) ?: "transcription failed"
                        cont.resumeWithException(
                            if (data.getBoolean(AsrProtocol.KEY_UNSUPPORTED)) {
                                UnsupportedDeviceException(text)
                            } else {
                                IllegalStateException(text)
                            }
                        )
                    }

                    else -> super.handleMessage(msg)
                }
            }
        })

        val conn = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val remote = binder?.let(::Messenger) ?: return finish {
                    cont.resumeWithException(BindFailedException("null binder"))
                }
                val request = Message.obtain(null, AsrProtocol.MSG_TRANSCRIBE).apply {
                    replyTo = incoming
                    data = Bundle().apply {
                        putString(AsrProtocol.KEY_AUDIO_PATH, audioFile.absolutePath)
                        putString(AsrProtocol.KEY_MODEL_ID, spec.id)
                    }
                }
                try {
                    remote.send(request)
                } catch (e: android.os.RemoteException) {
                    finish { cont.resumeWithException(AsrProcessDiedException("send failed: ${e.message}")) }
                }
            }

            /** Called when the remote process goes away — including when it is killed by a signal. */
            override fun onServiceDisconnected(name: ComponentName?) = finish {
                cont.resumeWithException(
                    AsrProcessDiedException("the ASR process died while transcribing")
                )
            }

            override fun onBindingDied(name: ComponentName?) = finish {
                cont.resumeWithException(AsrProcessDiedException("ASR binding died"))
            }

            override fun onNullBinding(name: ComponentName?) = finish {
                cont.resumeWithException(BindFailedException("service returned no binder"))
            }
        }
        connection = conn

        cont.invokeOnCancellation { finish {} }

        val intent = Intent(context, RemoteAsrService::class.java)
        val bound = try {
            context.bindService(intent, conn, Context.BIND_AUTO_CREATE)
        } catch (e: SecurityException) {
            false
        }
        if (!bound) {
            finish { cont.resumeWithException(BindFailedException("bindService returned false")) }
        }
    }

    private class BindFailedException(message: String) : RuntimeException(message)

    private companion object {
        const val TAG = "VnAsrClient"
    }
}
