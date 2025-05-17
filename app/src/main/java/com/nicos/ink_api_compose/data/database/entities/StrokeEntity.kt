package com.nicos.ink_api_compose.data.database.entities

import androidx.ink.strokes.Stroke
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.nicos.ink_api_compose.data.stroke_converter.SerializedStockBrush
import com.nicos.ink_api_compose.data.database.type_converters.StrokeSetConverter

@Entity
data class StrokeEntity(
    @PrimaryKey(autoGenerate = false) val id: Int = 0,
    val brushSize: Float,
    val brushColor: Long,
    val brushEpsilon: Float,
    val stockBrush: SerializedStockBrush,
    val strokeInputs: String?,
    @TypeConverters(StrokeSetConverter::class)
    val finishedStrokes: Set<Stroke> = emptySet<Stroke>(),
)
