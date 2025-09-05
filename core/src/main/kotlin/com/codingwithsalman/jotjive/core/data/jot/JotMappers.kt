package com.codingwithsalman.jotjive.core.data.jot

import com.codingwithsalman.jotjive.core.database.jot.JotEntity
import com.codingwithsalman.jotjive.core.domain.jot.Jot

internal fun JotEntity.toJot() = Jot(
    id = id,
    title = title,
    content = content,
    timestamp = timestamp,
    color = color
)

internal fun Jot.toJotEntity() = JotEntity(
    id = id ?: 0,
    title = title,
    content = content,
    timestamp = timestamp,
    color = color
)