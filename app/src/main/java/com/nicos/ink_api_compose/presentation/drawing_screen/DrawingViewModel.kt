package com.nicos.ink_api_compose.presentation.drawing_screen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nicos.ink_api_compose.data.database.entities.StrokeEntity
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

    init {
        loadDrawing()
    }

    private fun loadDrawing() {
        viewModelScope.launch(Dispatchers.Main) {
            viewModelScope.async(
                Dispatchers.IO
            ) {
                val strokeEntity = strokeRepository.getStroke()
                state.finishedStrokesState.value = strokeEntity?.finishedStrokes ?: emptySet()

            }.await()
            state = state.copy(
                finishedStrokesState = state.finishedStrokesState
            )
        }
    }

    fun saveDrawing() {
        viewModelScope.launch(Dispatchers.IO) {
            var stroke = state.finishedStrokesState.value.lastOrNull()
            if (stroke == null) return@launch
            val strokeEntity = strokeConverters.serializeStrokeToEntity(stroke)
            strokeRepository.insertStroke(
                strokeEntity.copy(
                    id = 1,
                    finishedStrokes = state.finishedStrokesState.value
                )
            )
        }
    }
}