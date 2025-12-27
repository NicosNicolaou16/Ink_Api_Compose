package com.nicos.ink_api_compose.data.stroke_converter

import androidx.ink.brush.Brush
import androidx.ink.strokes.Stroke

data class StrokesAndSelectedLastBrushesDeserialize(
    val strokes: Set<Stroke>,
    val lastBrushes: Brush,
)