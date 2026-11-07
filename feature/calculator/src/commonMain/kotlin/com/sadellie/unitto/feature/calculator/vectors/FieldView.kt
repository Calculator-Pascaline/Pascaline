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

package com.sadellie.unitto.feature.calculator.vectors

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private class FieldColors(val positive: Color, val negative: Color, val arrow: Color, val axis: Color, val probe: Color)

/** Arrows on a map of colors, and the point that a finger moves. */
@Composable
internal fun FieldView(picture: FieldPicture, probe: Pair<Double, Double>, onProbe: (Double, Double) -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val colors = FieldColors(scheme.tertiary, scheme.primary, scheme.onSurface.copy(alpha = ARROW_ALPHA), scheme.outline, scheme.error)
  val move by rememberUpdatedState(onProbe)
  val reach by rememberUpdatedState(picture.reach)
  Canvas(
    Modifier.fillMaxWidth()
      .widthIn(max = MAX_SIZE)
      .aspectRatio(1f)
      .pointerInput(Unit) {
        detectTapGestures { move(toWorld(it.x, size.width, reach), -toWorld(it.y, size.height, reach)) }
      }
      .pointerInput(Unit) {
        detectDragGestures { change, _ ->
          change.consume()
          move(toWorld(change.position.x, size.width, reach), -toWorld(change.position.y, size.height, reach))
        }
      }
  ) {
    drawColors(picture, colors)
    drawAxes(picture.reach, colors.axis)
    drawArrows(picture, colors.arrow)
    drawProbe(picture, probe, colors.probe)
  }
}

private fun toWorld(pixel: Float, size: Int, reach: Double) = (pixel / size * 2 - 1) * reach

private fun DrawScope.toScreen(x: Double, y: Double, reach: Double) =
  Offset(((x / reach + 1) / 2 * size.width).toFloat(), ((1 - y / reach) / 2 * size.height).toFloat())

/** Squares colored by the sign of the value, darker where it is bigger. */
private fun DrawScope.drawColors(picture: FieldPicture, colors: FieldColors) {
  val cell = size.width / COLOR_CELLS
  val values =
    List(COLOR_CELLS * COLOR_CELLS) { index ->
      val x = ((index % COLOR_CELLS + HALF) / COLOR_CELLS * 2 - 1) * picture.reach
      val y = (1 - (index / COLOR_CELLS + HALF) / COLOR_CELLS * 2) * picture.reach
      picture.color(x, y)
    }
  val largest = values.filter { it.isFinite() }.maxOfOrNull { abs(it) }?.takeIf { it > 0 } ?: return
  values.forEachIndexed { index, value ->
    if (!value.isFinite()) return@forEachIndexed
    val color = if (value >= 0) colors.positive else colors.negative
    drawRect(
      color.copy(alpha = (abs(value) / largest * MAX_COLOR_ALPHA).toFloat()),
      Offset(index % COLOR_CELLS * cell, index / COLOR_CELLS * cell),
      Size(cell + 1, cell + 1),
    )
  }
}

private fun DrawScope.drawAxes(reach: Double, color: Color) {
  val origin = toScreen(0.0, 0.0, reach)
  drawLine(color, Offset(0f, origin.y), Offset(size.width, origin.y))
  drawLine(color, Offset(origin.x, 0f), Offset(origin.x, size.height))
}

/** All as long as the cells, but shorter where the field is weaker. */
private fun DrawScope.drawArrows(picture: FieldPicture, color: Color) {
  val cell = size.width / ARROWS
  val points =
    (0 until ARROWS).flatMap { i ->
      (0 until ARROWS).map { j ->
        val x = ((i + HALF) / ARROWS * 2 - 1) * picture.reach
        val y = (1 - (j + HALF) / ARROWS * 2) * picture.reach
        Triple(x, y, picture.arrow(x, y))
      }
    }
  val longest = points.map { hypot(it.third.first, it.third.second) }.filter { it.isFinite() }.maxOrNull()?.takeIf { it > 0 } ?: return
  points.forEach { (x, y, vector) ->
    val length = hypot(vector.first, vector.second)
    if (!length.isFinite() || length == 0.0) return@forEach
    val shown = (cell * ARROW_SHARE * (length / longest).coerceAtLeast(MIN_ARROW)).toFloat()
    drawArrow(toScreen(x, y, picture.reach), vector, shown, color, ARROW_WIDTH)
  }
}

private fun DrawScope.drawArrow(center: Offset, vector: Pair<Double, Double>, length: Float, color: Color, width: Float) {
  // y goes up in the field and down on the screen
  val angle = atan2(-vector.second, vector.first)
  val half = Offset((cos(angle) * length / 2).toFloat(), (sin(angle) * length / 2).toFloat())
  val tip = center + half
  drawLine(color, center - half, tip, strokeWidth = width.dp.toPx())
  val head = length * HEAD_SHARE
  listOf(HEAD_ANGLE, -HEAD_ANGLE).forEach { turn ->
    val back = Offset((cos(angle + PI - turn) * head).toFloat(), (sin(angle + PI - turn) * head).toFloat())
    drawLine(color, tip, tip + back, strokeWidth = width.dp.toPx())
  }
}

/** The point, with its own arrow, longer and in color. */
private fun DrawScope.drawProbe(picture: FieldPicture, probe: Pair<Double, Double>, color: Color) {
  val (x, y) = probe
  val center = toScreen(x, y, picture.reach)
  val vector = picture.arrow(x, y)
  if (hypot(vector.first, vector.second).let { it.isFinite() && it > 0 }) {
    drawArrow(center, vector, size.width / ARROWS * PROBE_ARROW, color, PROBE_WIDTH)
  }
  drawCircle(color, PROBE_RADIUS.dp.toPx(), center)
}

private const val COLOR_CELLS = 48
private const val ARROWS = 15
private const val HALF = 0.5
private const val MAX_COLOR_ALPHA = 0.55
private const val ARROW_ALPHA = 0.7f
private const val ARROW_SHARE = 0.85
private const val MIN_ARROW = 0.15
private const val ARROW_WIDTH = 1.5f
private const val PROBE_WIDTH = 3f
private const val PROBE_ARROW = 1.8f
private const val PROBE_RADIUS = 6
private const val HEAD_SHARE = 0.3f
private const val HEAD_ANGLE = 0.45
private val MAX_SIZE = 520.dp
