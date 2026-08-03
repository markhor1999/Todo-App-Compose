package com.codingwithsalman.voicenotes.core.media.di

import com.tricodestudio.voicekit.AudioDecoder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Adapts VoiceKit types into Murmur's Hilt graph.
 *
 * [AudioDecoder] used to live in this module with an `@Inject` constructor. It now ships in
 * `:voicekit`, which carries no DI annotations on purpose — an SDK that forces a container on its
 * consumers is one nobody can adopt. So the app provides it, exactly as any third-party integrator
 * would. This file *is* the integration surface, and it is four lines.
 */
@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideAudioDecoder(): AudioDecoder = AudioDecoder()
}
