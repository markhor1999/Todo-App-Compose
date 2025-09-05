package com.codingwithsalman.jotjive.core.domain.jot

import kotlinx.coroutines.flow.Flow

interface JotDataSource {
    fun observeJots(): Flow<List<Jot>>
    suspend fun insertJot(jot: Jot)
    suspend fun getJot(id: Int): Jot?
    suspend fun deleteJot(id: Int)
    suspend fun deleteJot(jot: Jot)
}