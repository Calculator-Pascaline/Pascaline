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

package com.sadellie.unitto.feature.calculator.elements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Bohr's picture of an atom: the nucleus with the electrons going around it on their shells, the
 * inner ones faster. It grows from the nucleus when [shells] change.
 *
 * @param shells Electrons of each shell, the innermost first.
 */
@Composable
internal fun AtomView(modifier: Modifier, shells: List<Int>, nucleus: Color, line: Color, electron: Color) {
  val growth = remember { Animatable(0f) }
  LaunchedEffect(shells) {
    growth.snapTo(0f)
    growth.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
  }
  val time by
    rememberInfiniteTransition(label = "Electrons")
      .animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ORBIT_MILLIS, easing = LinearEasing), RepeatMode.Restart),
        label = "Orbits",
      )
  Canvas(modifier = modifier) {
    val outer = min(size.width, size.height) / 2 - ELECTRON_RADIUS.toPx()
    val inner = outer * NUCLEUS_SIZE
    drawCircle(nucleus, radius = inner * growth.value.coerceIn(0f, 1f) * NUCLEUS_DOT, center = size.center)
    shells.forEachIndexed { index, electrons ->
      val part = if (shells.size == 1) 1f else index.toFloat() / (shells.size - 1)
      val radius = (inner + (outer - inner) * part) * growth.value
      drawShell(radius, electrons, time * (shells.size - index) * FULL_TURN + index * SHELL_SHIFT, line, electron)
    }
  }
}

private fun DrawScope.drawShell(radius: Float, electrons: Int, turn: Float, line: Color, electron: Color) {
  drawCircle(line, radius = radius, center = size.center, style = Stroke(SHELL_WIDTH.toPx()))
  val dot = if (electrons > MANY_ELECTRONS) SMALL_ELECTRON_RADIUS.toPx() else ELECTRON_RADIUS.toPx()
  repeat(electrons) { index ->
    val angle = turn + index.toFloat() / electrons * FULL_TURN
    val center = size.center + Offset(cos(angle) * radius, sin(angle) * radius)
    drawCircle(electron, radius = dot, center = center)
  }
}

private const val FULL_TURN = (2 * PI).toFloat()
private const val ORBIT_MILLIS = 16_000
private const val NUCLEUS_SIZE = 0.3f
private const val NUCLEUS_DOT = 0.7f
private const val SHELL_SHIFT = 0.7f
private const val MANY_ELECTRONS = 14
private val SHELL_WIDTH = 1.dp
private val ELECTRON_RADIUS = 3.dp
private val SMALL_ELECTRON_RADIUS = 2.dp
