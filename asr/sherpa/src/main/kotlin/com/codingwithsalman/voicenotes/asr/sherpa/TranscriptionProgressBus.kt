package com.codingwithsalman.voicenotes.asr.sherpa

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Live per-note transcription progress, shared between workers and UI observers. */
@Singleton
class TranscriptionProgressBus @Inject constructor() {

    private val _progressByNote = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val progressByNote: StateFlow<Map<Long, Float>> = _progressByNote.asStateFlow()

    fun update(noteId: Long, progress: Float) {
        _progressByNote.value = _progressByNote.value + (noteId to progress)
    }

    fun clear(noteId: Long) {
        _progressByNote.value = _progressByNote.value - noteId
    }
}
