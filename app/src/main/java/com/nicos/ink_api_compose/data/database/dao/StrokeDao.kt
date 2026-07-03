package com.nicos.ink_api_compose.data.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
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