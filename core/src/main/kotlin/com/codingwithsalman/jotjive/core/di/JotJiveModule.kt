package com.codingwithsalman.jotjive.core.di

import com.codingwithsalman.jotjive.core.data.audio.AndroidAudioPlayer
import com.codingwithsalman.jotjive.core.data.jive.RoomJiveDataSource
import com.codingwithsalman.jotjive.core.data.recording.AndroidVoiceRecorder
import com.codingwithsalman.jotjive.core.data.recording.InternalRecordingStorage
import com.codingwithsalman.jotjive.core.data.settings.DataStoreSettings
import com.codingwithsalman.jotjive.core.domain.audio.AudioPlayer
import com.codingwithsalman.jotjive.core.domain.jive.JiveDataSource
import com.codingwithsalman.jotjive.core.domain.recording.RecordingStorage
import com.codingwithsalman.jotjive.core.domain.recording.VoiceRecorder
import com.codingwithsalman.jotjive.core.domain.settings.SettingsPreferences
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val jotJiveModule = module {
    singleOf(::AndroidVoiceRecorder) bind VoiceRecorder::class
    singleOf(::InternalRecordingStorage) bind RecordingStorage::class
    singleOf(::AndroidAudioPlayer) bind AudioPlayer::class
    singleOf(::RoomJiveDataSource) bind JiveDataSource::class
    singleOf(::DataStoreSettings) bind SettingsPreferences::class
}