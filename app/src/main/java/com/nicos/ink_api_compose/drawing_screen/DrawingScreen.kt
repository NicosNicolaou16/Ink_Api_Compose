package com.nicos.ink_api_compose.drawing_screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Picture
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.color.Color
import androidx.ink.brush.color.toArgb
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import androidx.input.motionprediction.MotionEventPredictor
import com.nicos.ink_api_compose.R
import com.nicos.ink_api_compose.ui.theme.Blue
import com.nicos.ink_api_compose.ui.theme.Green
import com.nicos.ink_api_compose.ui.theme.Pink
import com.nicos.ink_api_compose.ui.theme.Red
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.ByteArrayOutputStream
import kotlin.collections.plus

@SuppressLint("ClickableViewAccessibility", "RestrictedApi")
@Composable
fun DrawingSurface(
    innerPadding: PaddingValues,
    finishedStrokesState: MutableState<Set<Stroke>>,
    eraseDrawer: () -> Unit,
) {
    var inProgressStrokesView by remember { mutableStateOf<InProgressStrokesView?>(null) }
    val selectedColor = remember { mutableIntStateOf(Color.Red.toArgb()) }
    val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    val currentPointerId = remember { mutableStateOf<Int?>(null) }
    val currentStrokeId = remember { mutableStateOf<InProgressStrokeId?>(null) }
    var defaultBrush = Brush.createWithColorIntArgb(
        family = StockBrushes.pressurePenLatest,
        colorIntArgb = Color.Red.toArgb(),
        size = 15F,
        epsilon = 0.1F
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val rootView = FrameLayout(context)
                InProgressStrokesView(context).apply {
                    eagerInit()
                    addFinishedStrokesListener(
                        object : InProgressStrokesFinishedListener {
                            override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                                finishedStrokesState.value += strokes.values
                                inProgressStrokesView?.removeFinishedStrokes(strokes.keys)
                                // Caller must recompose from callback strokes, cannot wait until a later frame.
                                removeFinishedStrokes(strokes.keys)
                            }
                        }
                    )
                    inProgressStrokesView = this
                }
                val predictor = MotionEventPredictor.newInstance(rootView)
                val touchListener =
                    View.OnTouchListener { view, event ->
                        predictor.record(event)
                        val predictedEvent = predictor.predict()

                        try {
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    // First pointer - treat it as inking.
                                    view.requestUnbufferedDispatch(event)
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    currentPointerId.value = pointerId
                                    currentStrokeId.value =
                                        inProgressStrokesView?.startStroke(
                                            event = event,
                                            pointerId = pointerId,
                                            brush = defaultBrush
                                        )
                                    true
                                }

                                MotionEvent.ACTION_MOVE -> {
                                    val pointerId = checkNotNull(currentPointerId.value)
                                    val strokeId = checkNotNull(currentStrokeId.value)

                                    for (pointerIndex in 0 until event.pointerCount) {
                                        if (event.getPointerId(pointerIndex) != pointerId) continue
                                        inProgressStrokesView?.addToStroke(
                                            event,
                                            pointerId,
                                            strokeId,
                                            predictedEvent
                                        )
                                    }
                                    true
                                }

                                MotionEvent.ACTION_UP -> {
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    check(pointerId == currentPointerId.value)
                                    val currentStrokeId = checkNotNull(currentStrokeId.value)
                                    inProgressStrokesView?.finishStroke(
                                        event,
                                        pointerId,
                                        currentStrokeId
                                    )
                                    view.performClick()
                                    true
                                }

                                MotionEvent.ACTION_CANCEL -> {
                                    val pointerIndex = event.actionIndex
                                    val pointerId = event.getPointerId(pointerIndex)
                                    check(pointerId == currentPointerId.value)

                                    val currentStrokeId = checkNotNull(currentStrokeId.value)
                                    inProgressStrokesView?.cancelStroke(currentStrokeId, event)
                                    true
                                }

                                else -> false
                            }
                        } finally {
                            predictedEvent?.recycle()
                        }

                    }
                rootView.setOnTouchListener(touchListener)
                rootView.addView(inProgressStrokesView)
                rootView
            },
        )
        Canvas(modifier = Modifier) {
            val canvasTransform = Matrix()
            drawContext.canvas.nativeCanvas.concat(canvasTransform)
            val canvas = drawContext.canvas.nativeCanvas

            val bitmap = recordCanvasToBitmap(
                strokes = finishedStrokesState.value.toList(),
                canvasStrokeRenderer = canvasStrokeRenderer,
                canvasSize = size,
                canvasTransform = canvasTransform
            )

            Log.d("bitmap", bitmap.toBase64String())

            finishedStrokesState.value.forEach { stroke ->
                canvasStrokeRenderer.draw(
                    stroke = stroke,
                    canvas = canvas,
                    strokeToScreenTransform = canvasTransform
                )
            }
        }
        Row(
            modifier = Modifier
                .height(height = 150.dp)
                .align(Alignment.BottomCenter)
                .fillMaxWidth(fraction = 0.7f)
                .safeDrawingPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            EraseDrawer(
                eraseDrawer = eraseDrawer
            )
            SelectedColor(
                selectedColor = selectedColor,
                color = Red
            ) {
                defaultBrush =
                    defaultBrush.copyWithColorIntArgb(colorIntArgb = Color.Red.toArgb())
            }
            SelectedColor(
                selectedColor = selectedColor,
                color = Blue
            ) {
                defaultBrush =
                    defaultBrush.copyWithColorIntArgb(colorIntArgb = Color.Blue.toArgb())
            }
            SelectedColor(
                selectedColor = selectedColor,
                color = Pink
            ) {
                defaultBrush =
                    defaultBrush.copyWithColorIntArgb(colorIntArgb = Color(Pink.value).toArgb())
            }
            SelectedColor(
                selectedColor = selectedColor,
                color = Green
            ) {
                defaultBrush =
                    defaultBrush.copyWithColorIntArgb(colorIntArgb = Color.Green.toArgb())
            }
        }
    }
}

fun recordCanvasToBitmap(
    strokes: List<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    canvasSize: Size,
    canvasTransform: Matrix? = null // Optional transform
): Bitmap {
    val picture = Picture()
    val canvas = picture.beginRecording(
        if (canvasSize.width.toInt() != 0) canvasSize.width.toInt() else 1000,
        if (canvasSize.height.toInt() != 0) canvasSize.height.toInt() else 1000
    )

    canvas.concat(canvasTransform)

    strokes.forEach { stroke ->
        canvasStrokeRenderer.draw(
            stroke = stroke,
            canvas = canvas, // Use the Picture's canvas
            strokeToScreenTransform = canvasTransform ?: Matrix() //Handle null case
        )
    }
    picture.endRecording()
    val bitmap = Bitmap.createBitmap(picture)
    return bitmap
}

fun Bitmap.toBase64String(): String {
    ByteArrayOutputStream().use { outputStream ->
        this.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}

@Composable
private fun EraseDrawer(eraseDrawer: () -> Unit) {
    Button(
        onClick = {
            eraseDrawer()
        },
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_delete_forever_24),
            contentDescription = "Delete",
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun SelectedColor(
    selectedColor: MutableIntState,
    color: androidx.compose.ui.graphics.Color,
    selectingColor: () -> Unit
) {

    Box(
        modifier = Modifier
            .size(
                height = if (selectedColor.intValue == color.toArgb()) 100.dp else 70.dp,
                width = 50.dp
            )
            .background(color = color)
            .clickable {
                selectedColor.intValue = color.toArgb()
                selectingColor()
            })
}