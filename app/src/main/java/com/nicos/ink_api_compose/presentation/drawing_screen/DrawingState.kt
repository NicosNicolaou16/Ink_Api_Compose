package com.nicos.ink_api_compose.presentation.drawing_screen

import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.ink.strokes.Stroke

data class DrawingState(
    val finishedStrokesState: MutableState<Set<Stroke>> = mutableStateOf(emptySet<Stroke>()),
    var bitmap: Bitmap? = null,
    val isLoading: Boolean = false
)
