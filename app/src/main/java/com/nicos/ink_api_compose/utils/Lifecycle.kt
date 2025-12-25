package com.nicos.ink_api_compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun MyLifecycle(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onDestroy: () -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {}
) {
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnStop by rememberUpdatedState(onStop)
    val currentOnDestroy by rememberUpdatedState(onDestroy)
    val currentOnPause by rememberUpdatedState(onPause)
    val currentOnResume by rememberUpdatedState(onResume)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    currentOnStart()
                }
                Lifecycle.Event.ON_STOP -> {
                    currentOnStop()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    currentOnDestroy()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    currentOnPause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    currentOnResume()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}