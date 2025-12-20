package com.nicos.ink_api_compose.data.stroke_converter

import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.storage.decode
import androidx.ink.storage.encode
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInputBatch
import com.nicos.ink_api_compose.data.database.entities.StrokeEntity
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class StrokeConverters {

    companion object {
        private val stockBrushToEnumValues =
            mapOf(
                StockBrushes.marker() to SerializedStockBrush.MARKER_V1,
                StockBrushes.pressurePen() to SerializedStockBrush.PRESSURE_PEN_V1,
                StockBrushes.highlighter() to SerializedStockBrush.HIGHLIGHTER_V1,
            )

        private val enumToStockBrush =
            stockBrushToEnumValues.entries.associate { (key, value) -> value to key }
    }

    fun serializeStrokeToEntity(stroke: List<Stroke>): StrokeEntity {
        val serializedBrush = serializeBrush(stroke.last().brush)
        val encodedSerializedInputs = stroke.map {
            ByteArrayOutputStream().use { outputStream ->
                it.inputs.encode(outputStream)
                outputStream.toByteArray()
            }
        }
        return StrokeEntity(
            brushSize = serializedBrush.size,
            brushColor = serializedBrush.color,
            brushEpsilon = serializedBrush.epsilon,
            stockBrush = serializedBrush.stockBrush,
            strokeInputs = Json.encodeToString(encodedSerializedInputs),
        )
    }


    fun deserializeEntityToStroke(entity: StrokeEntity): Set<Stroke> {
        val serializedBrush =
            SerializedBrush(
                size = entity.brushSize,
                color = entity.brushColor,
                epsilon = entity.brushEpsilon,
                stockBrush = entity.stockBrush,
            )

        val decodedSerializedInputs = Json.decodeFromString<List<ByteArray>>(entity.strokeInputs)
        val inputsStroke = decodedSerializedInputs.map {
            ByteArrayInputStream(it).use { inputStream ->
                StrokeInputBatch.decode(inputStream)
            }
        }

        val brush = deserializeBrush(serializedBrush)

        return inputsStroke.map { Stroke(brush = brush, inputs = it) }.toSet()
    }

    private fun serializeBrush(brush: Brush): SerializedBrush {
        return SerializedBrush(
            size = brush.size,
            color = brush.colorLong,
            epsilon = brush.epsilon,
            stockBrush = stockBrushToEnumValues[brush.family] ?: SerializedStockBrush.MARKER_V1,
        )
    }

    private fun deserializeBrush(serializedBrush: SerializedBrush): Brush {
        val stockBrushFamily = enumToStockBrush[serializedBrush.stockBrush] ?: StockBrushes.marker()

        return Brush.createWithColorLong(
            family = stockBrushFamily,
            colorLong = serializedBrush.color,
            size = serializedBrush.size,
            epsilon = serializedBrush.epsilon,
        )
    }
}
