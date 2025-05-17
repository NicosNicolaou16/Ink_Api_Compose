package com.nicos.ink_api_compose.data.database.entities

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StrokeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStroke(stroke: StrokeEntity)

    @Delete
    suspend fun deleteStroke(stroke: StrokeEntity)

    @Query("SELECT * FROM strokeentity WHERE id = 1")
    suspend fun getStroke(): StrokeEntity
}