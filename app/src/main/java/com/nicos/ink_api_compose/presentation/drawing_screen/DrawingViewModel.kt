package com.nicos.ink_api_compose.presentation.drawing_screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Picture
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.Intersection.intersects
import androidx.ink.geometry.MutableParallelogram
import androidx.ink.geometry.MutableSegment
import androidx.ink.geometry.MutableVec
import androidx.ink.geometry.Vec
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
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
import kotlinx.coroutines.withContext
import kotlin.collections.minus

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val strokeRepository: StrokeRepository,
    private val strokeConverters: StrokeConverters,
) : ViewModel() {

    var state by mutableStateOf(DrawingState())
        private set

    private var previousPoint: MutableVec? = null
    private val eraserPadding = 50f

    private val eraserBox = ImmutableBox.fromCenterAndDimensions(
        Vec.ORIGIN,
        Float.MAX_VALUE,
        Float.MAX_VALUE
    )

    init {
        loadDrawing()
    }

    /**
     * Loads the drawing from the database.
     * */
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

    /**
     * Saves the drawing to the database.
     * */
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

    /**
     * Starts erasing the previous point.
     * */
    fun startErase() {
        previousPoint = null
    }

    /**
     * Erases the previous point from the finishedStrokesState.
     * */
    fun endErase() {
        previousPoint = null
        viewModelScope.launch { saveDrawing() }
    }

    /**
     * Erases a single point from the finishedStrokesState.
     * @param x: Float x coordinate of the point to erase
     * @param y: Float y coordinate of the point to erase
     * */
    fun erase(x: Float, y: Float) {
        val strokesBeforeErase = state.finishedStrokesState.value
        val strokesAfterErase = eraseIntersectingStrokes(
            x, y, strokesBeforeErase
        )
        if (strokesAfterErase.size != strokesBeforeErase.size) {
            Snapshot.withMutableSnapshot {
                state.finishedStrokesState.value = strokesAfterErase
            }
        }
    }

    /**
     * Erases all strokes that intersect with the current point.
     * @param currentX: Float x coordinate of the current point
     * @param currentY: Float y coordinate of the current point
     * @param currentStrokes: Set of strokes to erase from
     * */
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

    /**
     * Erases all strokes that intersect with the eraserBox.
     * @param finishedStrokesState: MutableState<Set<Stroke>> to erase from
     * */
    fun eraseWholeStrokes(
        finishedStrokesState: MutableState<Set<Stroke>>,
    ) {
        val threshold = 0.1f

        val strokesToErase = finishedStrokesState.value.filter { stroke ->
            stroke.shape.computeCoverageIsGreaterThan(
                box = eraserBox,
                coverageThreshold = threshold,
            )
        }
        if (strokesToErase.isNotEmpty()) {
            Snapshot.withMutableSnapshot {
                state.finishedStrokesState.value -= strokesToErase
                state = state.copy(finishedStrokesState = state.finishedStrokesState)
            }
        }
    }

    /**
     * Sets the bitmap state to null.
     * */
    fun setBitmapAsNull() {
        state = state.copy(
            bitmap = null
        )
    }

    /**
     * Records a canvas to a bitmap.
     * @param strokes: List of strokes to render
     * @param canvasStrokeRenderer: CanvasStrokeRenderer to use for rendering
     * @param canvasTransform: Optional transform to apply to the canvas
     * */
    suspend fun recordCanvasToBitmap(
        strokes: List<Stroke>,
        canvasStrokeRenderer: CanvasStrokeRenderer,
        canvasTransform: Matrix? = null, // Optional transform
    ) = withContext(Dispatchers.Default) {
        val picture = Picture()
        val canvas = picture.beginRecording(
            2000,
            2000
        )

        // Apply the transform before rendering
        if (canvasTransform != null) {
            canvas.concat(canvasTransform)
        }

        // Render each stroke into the recording canvas
        strokes.forEach { stroke ->
            canvasStrokeRenderer.draw(
                stroke = stroke,
                canvas = canvas,
                strokeToScreenTransform = canvasTransform ?: Matrix()
            )
        }

        picture.endRecording()
        val bitmap = Bitmap.createBitmap(picture)
        withContext(Dispatchers.Main) {
            state = state.copy(
                bitmap = bitmap
            )
        }
    }
}
