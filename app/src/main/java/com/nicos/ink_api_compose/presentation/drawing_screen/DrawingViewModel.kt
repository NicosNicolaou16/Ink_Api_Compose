package com.nicos.ink_api_compose.presentation.drawing_screen

import android.annotation.SuppressLint
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.Intersection.intersects
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import androidx.ink.strokes.Stroke
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicos.ink_api_compose.data.stroke_converter.StrokeConverters
import com.nicos.ink_api_compose.domain.StrokeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val strokeRepository: StrokeRepository,
    private val strokeConverters: StrokeConverters,
) : ViewModel() {

    var state by mutableStateOf(DrawingState())
        private set

    private var previousPoint: MutableVec? = null
    private val eraserPadding = 50f

    init {
        loadDrawing()
    }

    private fun loadDrawing() {
        viewModelScope.launch(Dispatchers.Main) {
            viewModelScope.async(
                Dispatchers.IO
            ) {
                val strokeEntity = strokeRepository.getStroke() ?: return@async
                // Convert the strokeEntity to a stroke
                val strokes = strokeConverters.deserializeEntityToStroke(strokeEntity)
                // Add the stroke to the finishedStrokesState
                state.finishedStrokesState.value = strokes

            }.await()
            state = state.copy(
                finishedStrokesState = state.finishedStrokesState
            )
        }
    }

    fun saveDrawing() {
        viewModelScope.launch(Dispatchers.IO) {
            if (state.finishedStrokesState.value.isEmpty()) return@launch
            // Convert the stroke to a strokeEntity
            val strokeEntity =
                strokeConverters.serializeStrokeToEntity(state.finishedStrokesState.value)
            // Update the stroke in the database
            strokeRepository.insertStroke(
                strokeEntity.copy(
                    id = 1,
                )
            )
        }
    }

    fun startErase() {
        previousPoint = null
    }

    fun endErase() {
        previousPoint = null
        viewModelScope.launch { saveDrawing() }
    }

    fun erase(x: Float, y: Float) {
        val strokesBeforeErase = state.finishedStrokesState.value
        val strokesAfterErase = eraseIntersectingStrokes(
            x, y, strokesBeforeErase
        )
        state.finishedStrokesState.value = strokesAfterErase
        if (strokesAfterErase.size != strokesBeforeErase.size) {
            state = state.copy(
                finishedStrokesState = state.finishedStrokesState
            )
        }
    }

    @SuppressLint("RestrictedApi")
    private fun eraseIntersectingStrokes(
        currentX: Float,
        currentY: Float,
        currentStrokes: Set<Stroke>,
    ): Set<Stroke> {
        val prev = previousPoint
        previousPoint = MutableVec(currentX, currentY)

        if (prev == null) return currentStrokes

        val segment = MutableSegment(prev, MutableVec(currentX, currentY))
        val parallelogram = MutableParallelogram()
            .populateFromSegmentAndPadding(segment, eraserPadding)

        val strokesToRemove = currentStrokes.filter { stroke ->
            stroke.shape.intersects(parallelogram, AffineTransform.IDENTITY)
        }

        return if (strokesToRemove.isNotEmpty()) {
            currentStrokes - strokesToRemove.toSet()
        } else {
            currentStrokes.toSet()
        }
    }
}