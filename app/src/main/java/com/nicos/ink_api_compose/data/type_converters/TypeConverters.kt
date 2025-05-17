package com.nicos.ink_api_compose.data.type_converters

import androidx.ink.strokes.Stroke
import androidx.room.TypeConverter
import com.nicos.ink_api_compose.data.entities.StrokeEntity

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