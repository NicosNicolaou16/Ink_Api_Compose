package com.nicos.ink_api_compose.data.stroke_converter

import kotlinx.serialization.Serializable

@Serializable
data class SerializedBrush(
    val size: Float,
    val color: Long,
    val epsilon: Float,
    val stockBrush: SerializedStockBrush
)
