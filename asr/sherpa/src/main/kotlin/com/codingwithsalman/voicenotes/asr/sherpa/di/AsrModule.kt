package com.codingwithsalman.voicenotes.asr.sherpa.di

import com.codingwithsalman.voicenotes.asr.api.LiveTranscriptionManager
import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
import com.codingwithsalman.voicenotes.asr.sherpa.LiveTranscriptionManagerImpl
import com.codingwithsalman.voicenotes.asr.sherpa.TranscriptionCoordinatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AsrModule {

    @Binds
    @Singleton
    abstract fun bindsTranscriptionCoordinator(
        impl: TranscriptionCoordinatorImpl,
    ): TranscriptionCoordinator

    @Binds
    @Singleton
    abstract fun bindsLiveTranscriptionManager(
        impl: LiveTranscriptionManagerImpl,
    ): LiveTranscriptionManager
}

/**
 * Provides the VoiceKit engine internals into Murmur's graph.
 *
 * These used to be `@Inject`-annotated classes in `:asr:sherpa`. They now ship in `:voicekit`, which
 * carries no DI annotations, so the app constructs them — the same three lines any integrator would
 * write. When the migration behind [com.tricodestudio.voicekit.VoiceKit] finishes, this module goes
 * away entirely and the app calls `VoiceKit.transcribe()` instead.
 */
@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
object VoiceKitEngineModule {

    @dagger.Provides
    @javax.inject.Singleton
    @OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)
    fun provideModelStore(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
    ): com.tricodestudio.voicekit.ModelStore = com.tricodestudio.voicekit.ModelStore(context)

    /**
     * What this hardware can survive. Probed once — see
     * `brain/apps/voicenotes/2026-09-01-crash-diagnosis-lowend-devices.md`.
     */
    @dagger.Provides
    @javax.inject.Singleton
    @OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)
    fun provideDeviceCapabilities(
        @dagger.hilt.android.qualifiers.ApplicationContext context: android.content.Context,
    ): com.tricodestudio.voicekit.DeviceCapabilities =
        com.tricodestudio.voicekit.DeviceCapabilities.from(context)

    @dagger.Provides
    @javax.inject.Singleton
    @OptIn(com.tricodestudio.voicekit.VoiceKitInternalApi::class)
    fun provideTranscriptionEngine(
        modelStore: com.tricodestudio.voicekit.ModelStore,
        audioDecoder: com.tricodestudio.voicekit.AudioDecoder,
        capabilities: com.tricodestudio.voicekit.DeviceCapabilities,
    ): com.tricodestudio.voicekit.SherpaTranscriptionEngine =
        com.tricodestudio.voicekit.SherpaTranscriptionEngine(
            modelStore = modelStore,
            audioDecoder = audioDecoder,
            capabilities = capabilities,
        )
}
