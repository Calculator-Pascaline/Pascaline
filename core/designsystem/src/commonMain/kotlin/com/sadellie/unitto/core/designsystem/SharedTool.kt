/*
 * Pascaline is a calculator for Android
 * Copyright (c) 2026 Calculator-Pascaline
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sadellie.unitto.core.designsystem

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.RemeasureToBounds
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/** Lets a tile on Home grow into the screen of its tool. Null where nothing is shared. */
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** What a tile and the screen of its tool share, named after the tool. */
fun sharedToolKey(routeId: String) = "tool/$routeId"

/**
 * A tile and the screen of its tool are the same thing at two sizes: when the screen changes, one
 * grows or shrinks into the other. The tile is laid out again at each size, the screen keeps its
 * own and is scaled, so the tool never looks squeezed.
 */
@Composable
fun Modifier.sharedTool(key: String, shape: Shape, isScreen: Boolean): Modifier {
  val shared = LocalSharedTransitionScope.current ?: return this
  val animated = LocalNavAnimatedContentScope.current
  return with(shared) {
    this@sharedTool.sharedBounds(
      sharedContentState = rememberSharedContentState(key),
      animatedVisibilityScope = animated,
      // The tile and the screen cross-fade while they grow, so one of them always shows
      enter = fadeIn(tween(FADE)),
      exit = fadeOut(tween(FADE)),
      boundsTransform = { _, _ -> spring(BOUNDS_DAMPING, BOUNDS_STIFFNESS, Rect.VisibilityThreshold) },
      resizeMode =
        if (isScreen) SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop, Alignment.TopCenter)
        else RemeasureToBounds,
      clipInOverlayDuringTransition = OverlayClip(shape),
    )
  }
}

private const val FADE = 180
private const val BOUNDS_DAMPING = 0.85f
private const val BOUNDS_STIFFNESS = 380f
