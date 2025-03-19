package com.nicos.ink_api_compose.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.ink.authoring.InProgressStrokesView
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.nicos.ink_api_compose.drawing_screen.DrawingSurface
import com.nicos.ink_api_compose.intro_screen.IntroScreen

@Composable
fun NavigationRoot(
    navController: NavHostController,
    innerPadding: PaddingValues,
    inProgressStrokesView: InProgressStrokesView,
    finishedStrokesState: MutableState<Set<androidx.ink.strokes.Stroke>>,
    eraseDrawer: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = "intro"
    ) {
        composable(route = "intro") {
            IntroScreen(
                navController = navController
            )
        }

        composable(route = "drawing") {
            DrawingSurface(
                innerPadding = innerPadding,
                inProgressStrokesView = inProgressStrokesView,
                finishedStrokesState = finishedStrokesState,
                eraseDrawer = eraseDrawer
            )
        }
    }
}