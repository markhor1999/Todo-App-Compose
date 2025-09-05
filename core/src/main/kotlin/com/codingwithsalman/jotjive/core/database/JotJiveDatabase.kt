package com.codingwithsalman.jotjive.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.codingwithsalman.jotjive.core.database.jive.FloatListTypeConverter
import com.codingwithsalman.jotjive.core.database.jive.JiveDao
import com.codingwithsalman.jotjive.core.database.jive.JiveEntity
import com.codingwithsalman.jotjive.core.database.jive.MoodTypeConverter
import com.codingwithsalman.jotjive.core.database.jive_topic_relation.JiveTopicCrossRef
import com.codingwithsalman.jotjive.core.database.topic.TopicEntity

@Database(
    entities = [JiveEntity::class, TopicEntity::class, JiveTopicCrossRef::class],
    version = 1,
)
@TypeConverters(
    MoodTypeConverter::class,
    FloatListTypeConverter::class
)
abstract class JotJiveDatabase : RoomDatabase() {
    abstract val jiveDao: JiveDao
}