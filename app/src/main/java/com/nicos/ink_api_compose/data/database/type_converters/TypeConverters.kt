package com.nicos.ink_api_compose.data.database.type_converters

import androidx.ink.strokes.Stroke
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nicos.ink_api_compose.data.stroke_converter.StrokeConverters
import com.nicos.ink_api_compose.data.database.entities.StrokeEntity

class StrokeConverter {
    private val gson = Gson()
    private val converters = StrokeConverters()
    private val entityType = object : TypeToken<StrokeEntity>() {}.type

    /**
     * Converts a single [Stroke] object into a JSON String.
     */
    @TypeConverter
    fun fromStroke(stroke: Stroke?): String? {
        if (stroke == null) return null
        val entity = converters.serializeStrokeToEntity(stroke)
        return gson.toJson(entity, entityType)
    }

    /**
     * Converts a JSON String back into a single [Stroke] object.
     */
    @TypeConverter
    fun toStroke(json: String?): Stroke? {
        if (json.isNullOrEmpty()) return null
        val entity: StrokeEntity = gson.fromJson(json, entityType)
        return converters.deserializeEntityToStroke(entity)
    }
}