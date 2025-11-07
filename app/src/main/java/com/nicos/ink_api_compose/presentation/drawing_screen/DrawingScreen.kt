package com.nicos.ink_api_compose.presentation.drawing_screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Picture
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
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
import kotlin.collections.plus
import androidx.core.graphics.createBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.Vec
import com.nicos.ink_api_compose.utils.MyLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val eraserBox = ImmutableBox.fromCenterAndDimensions(
    Vec.ORIGIN,
    Float.MAX_VALUE,
    Float.MAX_VALUE
)

@SuppressLint("ClickableViewAccessibility", "RestrictedApi")
@Composable
fun DrawingSurface(
    innerPadding: PaddingValues,
    drawingViewModel: DrawingViewModel = hiltViewModel(),
) {
    var bitmapRe by remember {
        mutableStateOf(
            createBitmap(1, 1)
        )
    }
    val state = drawingViewModel.state
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var inProgressStrokesView by remember { mutableStateOf<InProgressStrokesView?>(null) }
    val selectedColor = remember { mutableIntStateOf(Color.Red.toArgb()) }
    val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    val currentPointerId = remember { mutableStateOf<Int?>(null) }
    val currentStrokeId = remember { mutableStateOf<InProgressStrokeId?>(null) }
    var defaultBrush = Brush.createWithColorIntArgb(
        family = StockBrushes.pressurePen(),
        colorIntArgb = Color.Red.toArgb(),
        size = 15F,
        epsilon = 0.1F
    )

    MyLifecycle(
        onStop = {
            drawingViewModel.saveDrawing()
        }
    )

    ShowBitmapDialog(
        bitmap = bitmapRe,
        showDialog = showDialog,
        onDismissRequest = { showDialog = false })

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val rootView = FrameLayout(context)
                    InProgressStrokesView(context).apply {
                        eagerInit()
                        addFinishedStrokesListener(
                            object : InProgressStrokesFinishedListener {
                                override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
                                    state.finishedStrokesState.value += strokes.values
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

            Canvas(modifier = Modifier
                .fillMaxSize()
                .clipToBounds()) {
                drawRect(color = androidx.compose.ui.graphics.Color.Transparent)
                val canvasTransform = Matrix()
                drawContext.canvas.nativeCanvas.concat(canvasTransform)
                val canvas = drawContext.canvas.nativeCanvas

                state.finishedStrokesState.value.forEach { stroke ->
                    canvasStrokeRenderer.draw(
                        stroke = stroke,
                        canvas = canvas,
                        strokeToScreenTransform = canvasTransform
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .height(height = 200.dp)
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(fraction = 0.7f)
                .safeDrawingPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                EraseDrawerButton(
                    eraseDrawer = {
                        eraseWholeStrokes(
                            finishedStrokesState = state.finishedStrokesState
                        )
                    }
                )
                CreateBitmapFromStrokeButton(
                    bitmap = {
                        scope.launch {
                            if (state.finishedStrokesState.value.isNotEmpty()) {
                                recordCanvasToBitmap(
                                    strokes = state.finishedStrokesState.value.toList(),
                                    canvasStrokeRenderer = canvasStrokeRenderer,
                                    canvasTransform = Matrix(),
                                    onBitmap = {
                                        bitmapRe = it
                                        showDialog = true
                                    }
                                )
                            }
                        }
                    })
            }
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

@Composable
fun ShowBitmapDialog(bitmap: Bitmap?, showDialog: Boolean, onDismissRequest: () -> Unit) {
    if (bitmap != null && showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = {
                Text(text = "Bitmap from Stroke")
            },
            text = {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Displayed Bitmap",
                    modifier = Modifier
                )
            },
            confirmButton = {
                Button(onClick = onDismissRequest) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun CreateBitmapFromStrokeButton(bitmap: () -> Unit) {
    Button(
        onClick = {
            bitmap()
        },
    ) {
        Icon(
            painter = painterResource(id = R.drawable.baseline_image_24),
            contentDescription = "image from stroke",
        )
    }
}

@Composable
private fun EraseDrawerButton(eraseDrawer: () -> Unit) {
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

suspend fun recordCanvasToBitmap(
    strokes: List<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    canvasTransform: Matrix? = null, // Optional transform
    onBitmap: (Bitmap) -> Unit,
) = withContext(Dispatchers.Default) {
    val picture = Picture()
    val canvas = picture.beginRecording(
        2000,
        2000
    )

    // Apply the transform before rendering
    canvas.concat(canvasTransform)

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
    onBitmap(bitmap)
}

@SuppressLint("RestrictedApi")
@Composable
private fun SelectedColor(
    selectedColor: MutableIntState,
    color: androidx.compose.ui.graphics.Color,
    selectingColor: () -> Unit
) {

    Image(
        painter = painterResource(id = R.drawable.ic_pencil),
        contentDescription = "check",
        colorFilter = ColorFilter.tint(
            color = color
        ),
        modifier = Modifier
            .size(
                height = if (selectedColor.intValue == color.toArgb()) 100.dp else 70.dp,
                width = 50.dp
            )
            .clickable {
                selectedColor.intValue = color.toArgb()
                selectingColor()
            })
}

private fun eraseWholeStrokes(
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
            finishedStrokesState.value -= strokesToErase
        }
    }
}