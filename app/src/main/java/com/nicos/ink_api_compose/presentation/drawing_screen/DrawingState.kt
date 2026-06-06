package com.nicos.ink_api_compose.presentation.drawing_screen

import android.graphics.Bitmap
import androidx.ink.strokes.Stroke

data class DrawingState(
    val finishedStrokesState: Set<Stroke> = emptySet<Stroke>(),
    var bitmap: Bitmap? = null,
)
