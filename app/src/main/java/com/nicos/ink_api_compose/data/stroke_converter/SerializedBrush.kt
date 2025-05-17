package com.nicos.ink_api_compose.data.stroke_converter

data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush
)
