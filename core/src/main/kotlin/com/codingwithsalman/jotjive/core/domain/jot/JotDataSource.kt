package com.codingwithsalman.jotjive.core.domain.jot

import kotlinx.coroutines.flow.Flow

interface JotDataSource {
    fun observeJots(): Flow<List<Jot>>
    fun observeTopics(): Flow<List<String>>

    suspend fun insertJot(jot: Jot)
    suspend fun updateJot(jot: Jot)
    suspend fun getJot(id: Int): Jot?
    suspend fun deleteJot(id: Int)
}