package com.nicos.ink_api_compose.data.entities

import androidx.ink.strokes.Stroke
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.nicos.ink_api_compose.data.type_converters.StrokeSetConverter

@Entity
data class StrokeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @TypeConverters(StrokeSetConverter::class)
    val finishedStrokes: Set<Stroke> = emptySet<Stroke>(),
)
