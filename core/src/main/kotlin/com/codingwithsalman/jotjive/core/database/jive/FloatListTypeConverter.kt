package com.codingwithsalman.jotjive.core.database.jive

import androidx.room.TypeConverter

internal class FloatListTypeConverter {
    @TypeConverter
    fun fromList(values: List<Float>): String {
        return values.joinToString(",")
    }

    @TypeConverter
    fun toList(value: String): List<Float> {
        return value.split(",").map { it.toFloat() }
    }
}