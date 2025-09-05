package com.codingwithsalman.jotjive.core.database.jot

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JotDao {
    @Query("SELECT * FROM jotentity")
    fun getJots(): Flow<List<JotEntity>>

    @Query("SELECT * FROM jotentity WHERE id = :id")
    suspend fun getJotById(id: Int): JotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJot(jot: JotEntity)

    @Delete
    suspend fun deleteJot(jotEntity: JotEntity)

    @Query("DELETE FROM jotentity WHERE id = :id")
    suspend fun deleteJot(id: Int)
}