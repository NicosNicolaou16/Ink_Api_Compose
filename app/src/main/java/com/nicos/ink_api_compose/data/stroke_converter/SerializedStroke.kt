package com.nicos.ink_api_compose.data.stroke_converter

data class SerializedStroke(
    val inputs: SerializedStrokeInputBatch,
    val brush: SerializedBrush
)
