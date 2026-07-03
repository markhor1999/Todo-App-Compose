package com.codingwithsalman.voicenotes.asr.sherpa.di

import com.codingwithsalman.voicenotes.asr.api.TranscriptionCoordinator
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
}
