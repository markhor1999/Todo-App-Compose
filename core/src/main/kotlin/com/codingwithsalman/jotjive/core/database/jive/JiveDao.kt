package com.codingwithsalman.jotjive.core.database.jive

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.codingwithsalman.jotjive.core.database.jive_topic_relation.JiveTopicCrossRef
import com.codingwithsalman.jotjive.core.database.jive_topic_relation.JiveWithTopics
import com.codingwithsalman.jotjive.core.database.topic.TopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface JiveDao {
    @Query("SELECT * FROM jiveentity ORDER BY recordedAt DESC")
    fun observeJives(): Flow<List<JiveWithTopics>>

    @Query("SELECT * FROM topicentity ORDER BY topic ASC")
    fun observeTopics(): Flow<List<TopicEntity>>

    @Query(
        """
        SELECT *
        FROM topicentity
        WHERE topic LIKE "%" || :query || "%"
        ORDER BY topic ASC
    """
    )
    fun searchTopics(query: String): Flow<List<TopicEntity>>

    @Insert
    suspend fun insertJive(jiveEntity: JiveEntity): Long

    @Upsert
    suspend fun upsertTopic(topicEntity: TopicEntity)

    @Insert
    suspend fun insertJiveTopicCrossRef(crossRef: JiveTopicCrossRef)

    @Transaction
    suspend fun insertJiveWithTopics(jiveWithTopics: JiveWithTopics) {
        val jiveId = insertJive(jiveWithTopics.jive)

        jiveWithTopics.topics.forEach { topic ->
            upsertTopic(topic)
            insertJiveTopicCrossRef(
                crossRef = JiveTopicCrossRef(
                    jiveId = jiveId.toInt(),
                    topic = topic.topic
                )
            )
        }
    }
}