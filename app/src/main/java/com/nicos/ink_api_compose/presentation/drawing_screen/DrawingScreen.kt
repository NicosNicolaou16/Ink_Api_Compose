package com.nicos.ink_api_compose.presentation.drawing_screen

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.ink.authoring.compose.InProgressStrokes
import androidx.ink.brush.Brush
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.color.Color
import androidx.ink.brush.color.toArgb
import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.nicos.ink_api_compose.R
import com.nicos.ink_api_compose.ui.theme.Blue
import com.nicos.ink_api_compose.ui.theme.Green
import com.nicos.ink_api_compose.ui.theme.Pink
import com.nicos.ink_api_compose.ui.theme.Red
import com.nicos.ink_api_compose.utils.MyLifecycle
import kotlinx.coroutines.launch

@Composable
fun DrawingSurfaceRoot(
    innerPadding: PaddingValues,
) {
    Scaffold { padding ->
        DrawingSurface(
            innerPadding = padding,
        )
    }
}

@OptIn(InkInternalOnlyApi::class)
@SuppressLint("ClickableViewAccessibility", "RestrictedApi")
@Composable
fun DrawingSurface(
    innerPadding: PaddingValues,
    drawingViewModel: DrawingViewModel = hiltViewModel(),
) {
    val state = drawingViewModel.state
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    val selectedColor = remember { mutableIntStateOf(Color.Red.toArgb()) }
    val canvasStrokeRenderer = remember { CanvasStrokeRenderer.create() }
    var isEraseModeEnable by remember { mutableStateOf(false) }
    val defaultBrush = Brush.createWithColorIntArgb(
        family = StockBrushes.pressurePen(),
        colorIntArgb = Color.Red.toArgb(),
        size = 15F,
        epsilon = 0.1F
    )
    val strokes = remember { mutableStateListOf<Stroke>() }

    MyLifecycle(
        onStop = {
            drawingViewModel.saveDrawing()
        }
    )

    LaunchedEffect(key1 = state.finishedStrokesState) {
        strokes.clear()
        strokes.addAll(state.finishedStrokesState)
    }

    showDialog = state.bitmap != null
    ShowBitmapDialog(
        bitmap = state.bitmap,
        showDialog = showDialog,
        onDismissRequest = {
            drawingViewModel.setBitmapAsNull()
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Drawing Area
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
            if (!isEraseModeEnable) {
                InProgressStrokes(
                    defaultBrush = defaultBrush,
                    nextBrush = {
                        defaultBrush.copyWithColorIntArgb(colorIntArgb = selectedColor.intValue)
                    },
                    onStrokesFinished = { newStrokes ->
                        strokes.addAll(newStrokes)
                        drawingViewModel.updateFinishedStrokesState(newStrokes = newStrokes.toSet())
                    }
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                drawRect(color = androidx.compose.ui.graphics.Color.Transparent)
                val canvasTransform = Matrix()
                drawContext.canvas.nativeCanvas.concat(canvasTransform)
                val canvas = drawContext.canvas.nativeCanvas

                strokes.forEach { stroke ->
                    canvasStrokeRenderer.draw(
                        stroke = stroke,
                        canvas = canvas,
                        strokeToScreenTransform = canvasTransform
                    )
                }
            }
        }

        // Modern Bottom Tool Palette
        BottomView(
            isEraseMode = isEraseModeEnable,
            selectedColor = selectedColor,
            onDrawingEnable = { isEraseModeEnable = false },
            onPartiallyEraseEnable = { isEraseModeEnable = true },
            onEraseDrawer = { drawingViewModel.eraseWholeStrokes() },
            onCreateBitmap = {
                scope.launch {
                    if (state.finishedStrokesState.isNotEmpty()) {
                        drawingViewModel.recordCanvasToBitmap(
                            canvasStrokeRenderer = canvasStrokeRenderer,
                            canvasTransform = Matrix(),
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun BottomView(
    isEraseMode: Boolean,
    selectedColor: MutableIntState,
    onDrawingEnable: () -> Unit,
    onPartiallyEraseEnable: () -> Unit,
    onEraseDrawer: () -> Unit,
    onCreateBitmap: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .safeDrawingPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Row: Toolbar Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drawing / Erasing Toggle Group
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolIconButton(
                        iconRes = R.drawable.outline_draw_24,
                        contentDescription = "Draw",
                        isActive = !isEraseMode,
                        onClick = onDrawingEnable
                    )
                    ToolIconButton(
                        iconRes = R.drawable.outline_delete_24,
                        contentDescription = "Eraser",
                        isActive = isEraseMode,
                        onClick = onPartiallyEraseEnable
                    )
                }

                // Global Actions Group
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolIconButton(
                        iconRes = R.drawable.baseline_delete_forever_24,
                        contentDescription = "Clear All",
                        isActive = false,
                        onClick = onEraseDrawer,
                        isDestructive = true
                    )
                    ToolIconButton(
                        iconRes = R.drawable.baseline_image_24,
                        contentDescription = "Create Bitmap",
                        isActive = false,
                        onClick = onCreateBitmap
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
            )

            // Bottom Row: Color Selection Palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom // Anchors the pencils to the bottom so they grow up
            ) {
                SelectedColor(selectedColor = selectedColor, color = Red)
                SelectedColor(selectedColor = selectedColor, color = Blue)
                SelectedColor(selectedColor = selectedColor, color = Pink)
                SelectedColor(selectedColor = selectedColor, color = Green)
            }
        }
    }
}

@Composable
private fun ToolIconButton(
    iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val backgroundColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
    val iconTint = when {
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        isDestructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .background(color = backgroundColor, shape = CircleShape)
            .clip(CircleShape)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = iconTint
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
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
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

@SuppressLint("RestrictedApi")
@Composable
private fun SelectedColor(
    selectedColor: MutableIntState,
    color: androidx.compose.ui.graphics.Color,
) {
    val isSelected = selectedColor.intValue == color.toArgb()

    // Smoothly animate the height instead of snapping instantly
    val animatedHeight by animateDpAsState(
        targetValue = if (isSelected) 100.dp else 65.dp,
        animationSpec = tween(durationMillis = 300),
        label = "pencilHeight"
    )

    Image(
        painter = painterResource(id = R.drawable.ic_pencil),
        contentDescription = "Select Color",
        colorFilter = ColorFilter.tint(color = color),
        modifier = Modifier
            .size(width = 50.dp, height = animatedHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Removes the ripple effect for a cleaner "tool selection" feel
            ) {
                selectedColor.intValue = color.toArgb()
            }
    )
}