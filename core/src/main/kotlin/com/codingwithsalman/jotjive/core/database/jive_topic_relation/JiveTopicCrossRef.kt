package com.codingwithsalman.jotjive.core.database.jive_topic_relation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.Relation
import com.codingwithsalman.jotjive.core.database.jive.JiveEntity
import com.codingwithsalman.jotjive.core.database.topic.TopicEntity

@Entity(
    primaryKeys = ["jiveId", "topic"],
)
internal data class JiveTopicCrossRef(
    val jiveId: Int,
    val topic: String
)

internal data class JiveWithTopics(
    @Embedded val jive: JiveEntity,
    @Relation(
        parentColumn = "jiveId",
        entityColumn = "topic",
        associateBy = Junction(JiveTopicCrossRef::class)
    )
    val topics: List<TopicEntity>
)