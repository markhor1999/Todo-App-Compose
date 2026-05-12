package com.codingwithsalman.jotjive.core.domain.settings

import com.codingwithsalman.jotjive.core.domain.jive.Mood
import kotlinx.coroutines.flow.Flow

interface SettingsPreferences {
    suspend fun saveDefaultTopics(topics: List<String>)
    fun observeDefaultTopics(): Flow<List<String>>

    suspend fun saveDefaultMood(mood: Mood)
    fun observeDefaultMood(): Flow<Mood>
}