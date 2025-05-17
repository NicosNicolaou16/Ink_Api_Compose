package com.nicos.ink_api_compose.data.stroke_converter

data class SerializedStrokeInput(
    val x: Float,
    val y: Float,
    val timeMillis: Float,
    val pressure: Float,
    val tiltRadians: Float,
    val orientationRadians: Float,
    val strokeUnitLengthCm: Float
)
