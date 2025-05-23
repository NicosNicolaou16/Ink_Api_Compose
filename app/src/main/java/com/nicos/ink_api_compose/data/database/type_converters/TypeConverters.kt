package com.nicos.ink_api_compose.data.database.type_converters

import androidx.ink.strokes.Stroke
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nicos.ink_api_compose.data.stroke_converter.StrokeConverters
import com.nicos.ink_api_compose.data.database.entities.StrokeEntity

class StrokeSetConverter {
    private val gson = Gson()
    private val converters = StrokeConverters()
    private val listType = object : TypeToken<List<StrokeEntity>>() {}.type

    @TypeConverter
    fun fromStrokeSet(strokes: Set<Stroke>?): String {
        val entities = strokes?.map { converters.serializeStrokeToEntity(it) } ?: emptyList()
        return gson.toJson(entities, listType)
    }

    @TypeConverter
    fun toStrokeSet(json: String?): Set<Stroke> {
        if (json.isNullOrEmpty()) return emptySet()
        val entities: List<StrokeEntity> = gson.fromJson(json, listType)
        return entities.mapNotNull { converters.deserializeEntityToStroke(it) }.toSet()
    }
}