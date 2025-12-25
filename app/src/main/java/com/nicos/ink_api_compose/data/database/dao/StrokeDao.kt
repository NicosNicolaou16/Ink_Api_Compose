package com.nicos.ink_api_compose.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nicos.ink_api_compose.data.database.entities.StrokeEntity

@Dao
interface StrokeDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertStroke(stroke: StrokeEntity)

    @Delete
    suspend fun deleteStroke(stroke: StrokeEntity)

    @Transaction
    @Query("SELECT * FROM strokeentity WHERE id = 1")
    suspend fun getStroke(): StrokeEntity?
}