package com.codingwithsalman.jotjive.core.database.jot

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.codingwithsalman.jotjive.core.database.jot_topic_relation.JotTopicCrossRef
import com.codingwithsalman.jotjive.core.database.jot_topic_relation.JotWithTopics
import com.codingwithsalman.jotjive.core.database.topic.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface JotDao {
    @Query("SELECT * FROM jotentity")
    fun observeJots(): Flow<List<JotWithTopics>>

    @Query("SELECT * FROM topicentity ORDER BY topic ASC")
    fun observeTopics(): Flow<List<TopicEntity>>
    @Query("SELECT * FROM jotentity WHERE jotId = :id")
    suspend fun getJotById(id: Int): JotWithTopics?

    @Insert
    suspend fun insertJot(jot: JotEntity): Long

    @Update
    suspend fun updateJot(jot: JotEntity)

    @Upsert
    suspend fun upsertTopic(topicEntity: TopicEntity)

    @Insert
    suspend fun insertJotTopicCrossRef(crossRef: JotTopicCrossRef)

    @Transaction
    suspend fun insertJotWithTopics(jotWithTopics: JotWithTopics) {
        println("JOT_ISSUE = ${jotWithTopics.jot}")
        val jotId = insertJot(jotWithTopics.jot)
        println("JOT_ISSUE jotId = $jotId")

        jotWithTopics.topics.forEach { topic ->
            upsertTopic(topic)
            insertJotTopicCrossRef(
                crossRef = JotTopicCrossRef(
                    jotId = jotId.toInt(),
                    topic = topic.topic
                )
            )
        }
    }

    @Delete
    suspend fun deleteJot(jotEntity: JotEntity)

    @Query("DELETE FROM jotentity WHERE jotId = :id")
    suspend fun deleteJot(id: Int)


}