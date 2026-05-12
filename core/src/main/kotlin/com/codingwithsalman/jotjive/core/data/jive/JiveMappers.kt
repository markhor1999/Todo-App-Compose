package com.codingwithsalman.jotjive.core.data.jive

import com.codingwithsalman.jotjive.core.database.jive.JiveEntity
import com.codingwithsalman.jotjive.core.database.jive_topic_relation.JiveWithTopics
import com.codingwithsalman.jotjive.core.database.topic.TopicEntity
import com.codingwithsalman.jotjive.core.domain.jive.Jive
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

internal fun JiveWithTopics.toJive(): Jive {
    return Jive(
        mood = jive.mood,
        title = jive.title,
        note = jive.note,
        topics = topics.map { it.topic },
        audioFilePath = jive.audioFilePath,
        audioPlaybackLength = jive.audioPlaybackLength.milliseconds,
        audioAmplitudes = jive.audioAmplitudes,
        recordedAt = Instant.ofEpochMilli(jive.recordedAt),
        id = jive.jiveId
    )
}

internal fun Jive.toJiveWithTopics(): JiveWithTopics {
    return JiveWithTopics(
        jive = JiveEntity(
            jiveId = id ?: 0,
            title = title,
            mood = mood,
            recordedAt = recordedAt.toEpochMilli(),
            note = note,
            audioAmplitudes = audioAmplitudes,
            audioFilePath = audioFilePath,
            audioPlaybackLength = audioPlaybackLength.inWholeMilliseconds
        ),
        topics = topics.map { TopicEntity(it) }
    )
}