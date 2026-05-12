package com.codingwithsalman.jotjive.core.data.jot

import com.codingwithsalman.jotjive.core.database.jot.JotDao
import com.codingwithsalman.jotjive.core.domain.jot.Jot
import com.codingwithsalman.jotjive.core.domain.jot.JotDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.collections.map

internal class RoomJotDataSource(
    private val jotDao: JotDao
) : JotDataSource {
    override fun observeJots(): Flow<List<Jot>> {
        return jotDao
            .observeJots()
            .map { jotWithTopics ->
                jotWithTopics.map { jotEntity ->
                    jotEntity.toJot()
                }
            }
    }

    override fun observeTopics(): Flow<List<String>> {
        return jotDao
            .observeTopics()
            .map { topicEntities ->
                topicEntities.map { it.topic }
            }
    }

    override suspend fun insertJot(jot: Jot) {
        jotDao.insertJotWithTopics(jot.toJotWithTopics())
    }

    override suspend fun updateJot(jot: Jot) {
        jotDao.updateJot(jot.toJotEntity())
    }

    override suspend fun getJot(id: Int): Jot? {
        return jotDao.getJotById(id)?.toJot()
    }

    override suspend fun deleteJot(id: Int) {
        jotDao.deleteJot(id)
    }
}