package com.nicos.ink_api_compose.presentation.drawing_screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Picture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.color.Color
import androidx.ink.brush.color.toArgb
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.Vec
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.nicos.ink_api_compose.R
import com.nicos.ink_api_compose.ui.theme.Blue
import com.nicos.ink_api_compose.ui.theme.Green
import com.nicos.ink_api_compose.ui.theme.Pink
import com.nicos.ink_api_compose.ui.theme.Red
import com.nicos.ink_api_compose.utils.MyLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.plus

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
    var showDialog by remember { mutableStateOf(false) }
    val selectedColor = remember { mutableIntStateOf(Color.Red.toArgb()) }
    val canvasStrokeRenderer = CanvasStrokeRenderer.create()
    var isEraseMode by remember { mutableStateOf(false) }
    val defaultBrush = Brush.createWithColorIntArgb(
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

    BottomView(
        innerPadding = innerPadding,
        drawingViewModel = drawingViewModel,
        state = state,
        isEraseMode = isEraseMode,
        defaultBrush = defaultBrush,
        selectedColor = selectedColor,
        canvasStrokeRenderer,
        partiallyErase = {
            isEraseMode = !isEraseMode
        },
        bitmap = {
            bitmapRe = it
            showDialog = true
        },
    )
}

@Composable
private fun BottomView(
    innerPadding: PaddingValues,
    drawingViewModel: DrawingViewModel,
    state: DrawingState,
    isEraseMode: Boolean,
    defaultBrush: Brush,
    selectedColor: MutableIntState,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    partiallyErase: () -> Unit,
    bitmap: (Bitmap) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { drawingViewModel.startErase() },
                        onDragEnd = { drawingViewModel.endErase() }
                    ) { change, _ ->
                        drawingViewModel.erase(change.position.x, change.position.y)
                        change.consume()
                    }
                }
        ) {
            if (!isEraseMode)
                InProgressStrokes(
                    defaultBrush = defaultBrush,
                    nextBrush = {
                        defaultBrush.copyWithColorIntArgb(colorIntArgb = selectedColor.intValue)
                    },
                    onStrokesFinished = { strokes -> state.finishedStrokesState.value += strokes }
                )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
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
                .safeDrawingPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Row {
                    EraseDrawerButton(
                        eraseDrawer = {
                            eraseWholeStrokes(
                                finishedStrokesState = state.finishedStrokesState
                            )
                        }
                    )
                    Spacer(modifier = Modifier.padding(end = 5.dp))
                    EraserPartiallyToggleButton(
                        isEraseMode = isEraseMode,
                        onClick = partiallyErase
                    )
                }
                CreateBitmapFromStrokeButton(
                    bitmap = {
                        scope.launch {
                            if (state.finishedStrokesState.value.isNotEmpty()) {
                                recordCanvasToBitmap(
                                    strokes = state.finishedStrokesState.value.toList(),
                                    canvasStrokeRenderer = canvasStrokeRenderer,
                                    canvasTransform = Matrix(),
                                    onBitmap = {
                                        bitmap(it)
                                    }
                                )
                            }
                        }
                    })
            }
            SelectedColor(
                selectedColor = selectedColor,
                color = Red
            )
            SelectedColor(
                selectedColor = selectedColor,
                color = Blue
            )
            SelectedColor(
                selectedColor = selectedColor,
                color = Pink
            )
            SelectedColor(
                selectedColor = selectedColor,
                color = Green
            )
        }
    }
}

@Composable
private fun EraserPartiallyToggleButton(
    isEraseMode: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isEraseMode) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color.Gray
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.outline_delete_24), // Replace with your eraser icon
            contentDescription = "Toggle Eraser",
        )
    }
}


@Composable
fun ShowBitmapDialog(
    bitmap: Bitmap?,
    showDialog: Boolean,
    onDismissRequest: () -> Unit
) {
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
        modifier = Modifier.width(width = 150.dp),
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