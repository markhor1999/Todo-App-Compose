package com.codingwithsalman.jotjive.core.database.jot_topic_relation

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.Relation
import com.codingwithsalman.jotjive.core.database.jot.JotEntity
import com.codingwithsalman.jotjive.core.database.topic.TopicEntity


@Entity(
    primaryKeys = ["jotId", "topic"],
)
internal data class JotTopicCrossRef(
    val jotId: Int,
    val topic: String
)

internal data class JotWithTopics(
    @Embedded val jot: JotEntity,
    @Relation(
        parentColumn = "jotId",
        entityColumn = "topic",
        associateBy = Junction(JotTopicCrossRef::class)
    )
    val topics: List<TopicEntity>
)