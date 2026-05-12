package com.codingwithsalman.jotjive.core.database.jive

import androidx.room.TypeConverter
import com.codingwithsalman.jotjive.core.domain.jive.Mood

internal class MoodTypeConverter {
    @TypeConverter
    fun fromMood(mood: Mood): String {
        return mood.name
    }

    @TypeConverter
    fun toMood(moodName: String): Mood {
        return Mood.valueOf(moodName)
    }
}