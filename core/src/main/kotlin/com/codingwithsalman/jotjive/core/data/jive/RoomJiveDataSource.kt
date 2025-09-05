package com.codingwithsalman.jotjive.core.data.jive

import com.codingwithsalman.jotjive.core.database.jive.JiveDao
import com.codingwithsalman.jotjive.core.domain.jive.Jive
import com.codingwithsalman.jotjive.core.domain.jive.JiveDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomJiveDataSource(
    private val jiveDao: JiveDao
) : JiveDataSource {
    override fun observeJives(): Flow<List<Jive>> {
        return jiveDao
            .observeJives()
            .map { jiveWithTopics ->
                jiveWithTopics.map { jiveWithTopic ->
                    jiveWithTopic.toJive()
                }
            }
    }

    override fun observeTopics(): Flow<List<String>> {
        return jiveDao
            .observeTopics()
            .map { topicEntities ->
                topicEntities.map { it.topic }
            }
    }

    override fun searchTopics(query: String): Flow<List<String>> {
        return jiveDao
            .searchTopics(query)
            .map { topicEntities ->
                topicEntities.map { it.topic }
            }
    }

    override suspend fun insertJive(jive: Jive) {
        jiveDao.insertJiveWithTopics(jive.toJiveWithTopics())
    }

}