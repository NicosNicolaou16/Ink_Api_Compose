package com.nicos.ink_api_compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.ink.geometry.ImmutableBox
import androidx.ink.geometry.Vec
import com.nicos.ink_api_compose.presentation.drawing_screen.DrawingSurface
import com.nicos.ink_api_compose.ui.theme.Ink_Api_ComposeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Ink_Api_ComposeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val eraserBox = ImmutableBox.fromCenterAndDimensions(
                        Vec.ORIGIN,
                        Float.MAX_VALUE,
                        Float.MAX_VALUE
                    )
                    DrawingSurface(
                        innerPadding = innerPadding,
                    )
                }
            }
        }
    }
}