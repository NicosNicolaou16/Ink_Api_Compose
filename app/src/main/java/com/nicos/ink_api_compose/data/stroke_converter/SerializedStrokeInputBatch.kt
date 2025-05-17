package com.nicos.ink_api_compose.data.stroke_converter


data class SerializedStrokeInputBatch(
    val toolType: SerializedToolType,
    val strokeUnitLengthCm: Float,
    val inputs: List<SerializedStrokeInput>
)
