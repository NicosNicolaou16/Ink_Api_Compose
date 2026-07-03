package com.nicos.ink_api_compose.data.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.nicos.ink_api_compose.data.stroke_converter.SerializedStockBrush

@Entity
data class StrokeEntity(
    @PrimaryKey(autoGenerate = false) val id: Int = 0,
    val brushSize: Float,
    val brushColor: Long,
    val brushEpsilon: Float,
    val stockBrush: SerializedStockBrush,
    val strokeInputs: String,
)
