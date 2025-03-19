package com.nicos.ink_api_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.UiThread
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.ink.authoring.InProgressStrokeId
import androidx.ink.authoring.InProgressStrokesFinishedListener
import androidx.ink.authoring.InProgressStrokesView
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.Vec
import androidx.ink.strokes.Stroke
import androidx.navigation.compose.rememberNavController
import com.nicos.ink_api_compose.navigation.NavigationRoot
import com.nicos.ink_api_compose.ui.theme.Ink_Api_ComposeTheme

class MainActivity : ComponentActivity(), InProgressStrokesFinishedListener {
    private lateinit var inProgressStrokesView: InProgressStrokesView
    private val finishedStrokesState = mutableStateOf(emptySet<Stroke>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inProgressStrokesView = InProgressStrokesView(this)
        inProgressStrokesView.addFinishedStrokesListener(this)
        enableEdgeToEdge()
        setContent {
            Ink_Api_ComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val eraserBox = ImmutableBox.fromCenterAndDimensions(
                        Vec.ORIGIN,
                        Float.MAX_VALUE,
                        Float.MAX_VALUE
                    )
                    NavigationRoot(
                        navController = rememberNavController(),
                        innerPadding = innerPadding,
                        inProgressStrokesView = inProgressStrokesView,
                        finishedStrokesState = finishedStrokesState,
                        eraseDrawer = {
                            eraseWholeStrokes(
                                eraserBox = eraserBox,
                                finishedStrokesState = finishedStrokesState
                            )
                        }
                    )
                }
            }
        }
    }

    @UiThread
    override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {
        finishedStrokesState.value += strokes.values
        inProgressStrokesView.removeFinishedStrokes(strokes.keys)
    }

    private fun eraseWholeStrokes(
        eraserBox: ImmutableBox,
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
}
