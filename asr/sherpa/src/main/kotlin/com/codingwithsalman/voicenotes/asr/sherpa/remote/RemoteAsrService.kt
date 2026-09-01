@file:OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)

package com.codingwithsalman.voicenotes.asr.sherpa.remote

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.util.Log
import com.tricodestudio.voicekit.ModelCatalog
import com.tricodestudio.voicekit.SherpaTranscriptionEngine
import com.tricodestudio.voicekit.UnsupportedDeviceException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject

/**
 * Runs sherpa-onnx in its own process (`:asr`, declared in this module's manifest).
 *
 * The point is containment, not performance. sherpa can fail with a `SIGBUS` — a signal, not a
 * throwable — which no `try`/`catch` can survive; and on a 2 GB phone the recogniser can exhaust
 * memory outright. In-process, either one killed Murmur. Out here, the worst case is that this
 * process dies, the client's binding drops, and the note is marked FAILED while the app carries on.
 * See brain/apps/voicenotes/2026-09-01-crash-diagnosis-lowend-devices.md.
 *
 * Everything injected here is Context-only ([SherpaTranscriptionEngine], its `ModelStore`,
 * `AudioDecoder` and `DeviceCapabilities`); the database is deliberately never touched from this
 * process, so Room stays single-process and needs no multi-instance invalidation.
 */
@AndroidEntryPoint
class RemoteAsrService : Service() {

    @Inject
    lateinit var engine: SherpaTranscriptionEngine

    private lateinit var worker: HandlerThread
    private lateinit var messenger: Messenger

    override fun onCreate() {
        super.onCreate()
        // Inference is long and blocking; it must not run on this process's main looper.
        worker = HandlerThread("asr-remote").apply { start() }
        messenger = Messenger(IncomingHandler(worker))
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onDestroy() {
        worker.quitSafely()
        super.onDestroy()
    }

    private inner class IncomingHandler(thread: HandlerThread) : Handler(thread.looper) {
        override fun handleMessage(msg: Message) {
            if (msg.what != AsrProtocol.MSG_TRANSCRIBE) {
                super.handleMessage(msg)
                return
            }
            val reply = msg.replyTo ?: return
            val data = msg.data ?: Bundle()
            val path = data.getString(AsrProtocol.KEY_AUDIO_PATH)
            val modelId = data.getString(AsrProtocol.KEY_MODEL_ID)
            if (path == null) {
                reply.sendQuietly(errorMessage("no audio path in request", unsupported = false))
                return
            }
            val spec = ModelCatalog.byId(modelId)

            try {
                var lastSent = -1
                val result = runBlocking {
                    engine.transcribe(File(path), spec) { p ->
                        // One update per percent: Binder traffic, not a progress bar refresh rate.
                        val percent = (p * 100).toInt()
                        if (percent > lastSent) {
                            lastSent = percent
                            reply.sendQuietly(
                                Message.obtain(null, AsrProtocol.MSG_PROGRESS).apply {
                                    this.data = Bundle().apply {
                                        putFloat(AsrProtocol.KEY_PROGRESS, p)
                                    }
                                }
                            )
                        }
                    }
                }
                val out = Message.obtain(null, AsrProtocol.MSG_RESULT).apply {
                    this.data = AsrProtocol.encodeSegments(result.segments).apply {
                        putLong(AsrProtocol.KEY_DURATION_MS, result.durationMs)
                        putString(AsrProtocol.KEY_LANGUAGE, result.language)
                    }
                }
                reply.sendQuietly(out)
            } catch (e: UnsupportedDeviceException) {
                Log.w(TAG, "refused: ${e.message}")
                reply.sendQuietly(errorMessage(e.message ?: "unsupported device", unsupported = true))
            } catch (e: OutOfMemoryError) {
                // Catching an Error on purpose. Out here it is survivable: report it and let the
                // note fail, rather than letting the signal take a process down with the app in it.
                Log.e(TAG, "out of memory while transcribing", e)
                reply.sendQuietly(errorMessage("out of memory", unsupported = false))
            } catch (e: Throwable) {
                Log.e(TAG, "transcription failed", e)
                reply.sendQuietly(errorMessage(e.message ?: e::class.java.simpleName, unsupported = false))
            }
        }
    }

    private fun errorMessage(text: String, unsupported: Boolean): Message =
        Message.obtain(null, AsrProtocol.MSG_ERROR).apply {
            data = Bundle().apply {
                putString(AsrProtocol.KEY_ERROR, text)
                putBoolean(AsrProtocol.KEY_UNSUPPORTED, unsupported)
            }
        }

    /**
     * The client can go away at any moment — it is a WorkManager job, and the system may stop it
     * mid-run. A dead peer is a normal outcome here, not something to crash this process over.
     */
    private fun Messenger.sendQuietly(message: Message) {
        try {
            send(message)
        } catch (e: android.os.RemoteException) {
            Log.i(TAG, "client went away before the reply landed")
        }
    }

    private companion object {
        const val TAG = "VnAsrRemote"
    }
}
