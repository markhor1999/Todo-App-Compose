package com.codingwithsalman.jotjive.core.data.jot

import com.codingwithsalman.jotjive.core.database.jot.JotDao
import com.codingwithsalman.jotjive.core.domain.jot.Jot
import com.codingwithsalman.jotjive.core.domain.jot.JotDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomJotDataSource(
    private val jotDao: JotDao
) : JotDataSource {
    override fun observeJots(): Flow<List<Jot>> {
        return jotDao
            .getJots()
            .map { jotEntities ->
                jotEntities.map { jotEntity ->
                    jotEntity.toJot()
                }
            }
    }

    override suspend fun insertJot(jot: Jot) {
        jotDao.insertJot(jot.toJotEntity())
    }

    override suspend fun getJot(id: Int): Jot? {
        return jotDao.getJotById(id)?.toJot()
    }

    override suspend fun deleteJot(id: Int) {
        jotDao.deleteJot(id)
    }

    override suspend fun deleteJot(jot: Jot) {
        jotDao.deleteJot(jot.toJotEntity())
    }
}