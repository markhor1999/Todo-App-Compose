package com.codingwithsalman.jotjive.core.domain.jive

import kotlinx.coroutines.flow.Flow

interface JiveDataSource {
    fun observeJives(): Flow<List<Jive>>
    fun observeTopics(): Flow<List<String>>
    fun searchTopics(query: String): Flow<List<String>>
    suspend fun insertJive(jive: Jive)
}