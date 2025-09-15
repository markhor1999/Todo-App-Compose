package com.codingwithsalman.jotjive.core.data.jot

import com.codingwithsalman.jotjive.core.database.jot.JotEntity
import com.codingwithsalman.jotjive.core.database.jot_topic_relation.JotWithTopics
import com.codingwithsalman.jotjive.core.database.topic.TopicEntity
import com.codingwithsalman.jotjive.core.domain.jot.Jot
import java.time.Instant

internal fun JotWithTopics.toJot() = Jot(
    id = jot.jotId,
    title = jot.title,
    note = jot.note,
    addedAt = Instant.ofEpochMilli(jot.addedAt),
    mood = jot.mood,
    topics = topics.map { it.topic },
    isTodo = jot.isTodo,
    isDone = jot.isDone
)

internal fun Jot.toJotEntity() = JotEntity(
    jotId = id ?: 0,
    title = title,
    note = note,
    addedAt = addedAt.toEpochMilli(),
    mood = mood,
    isTodo = isTodo,
    isDone = isDone
)

internal fun Jot.toJotWithTopics(): JotWithTopics {
    return JotWithTopics(
        jot = JotEntity(
            jotId = id ?: 0,
            title = title,
            mood = mood,
            addedAt = addedAt.toEpochMilli(),
            note = note,
            isTodo = isTodo,
            isDone = isDone
        ),
        topics = topics.map { TopicEntity(it) }
    )
}