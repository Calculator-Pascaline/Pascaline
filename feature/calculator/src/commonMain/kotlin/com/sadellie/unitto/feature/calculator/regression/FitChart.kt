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

package com.sadellie.unitto.feature.calculator.regression

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import kotlin.math.abs

private class ChartColors(val point: Color, val curve: Color, val gap: Color, val grid: Color, val text: Color, val ring: Color)

private fun FitWindow.xOf(value: Double, width: Float) = ((value - left) / (right - left) * width).toFloat()

private fun FitWindow.yOf(value: Double, height: Float) = ((top - value) / (top - bottom) * height).toFloat()

private fun FitWindow.valueAt(position: Offset, size: Size) =
  DataPoint(
    x = left + (position.x / size.width).coerceIn(0f, 1f) * (right - left),
    y = top - (position.y / size.height).coerceIn(0f, 1f) * (top - bottom),
  )

private fun FitWindow.nearest(points: List<DataPoint>, position: Offset, size: Size, reach: Float): Int? =
  points.indices
    .map { it to (Offset(xOf(points[it].x, size.width), yOf(points[it].y, size.height)) - position).getDistance() }
    .filter { (_, distance) -> distance <= reach }
    .minByOrNull { (_, distance) -> distance }
    ?.first

/**
 * The points, the curve and how far every point is from it. A point follows the finger, a tap adds
 * one and holding removes one. [window] should stay the same while a point is moved.
 */
@Composable
internal fun FitChart(
  points: List<DataPoint>,
  result: RegressionResult?,
  window: FitWindow,
  prediction: DataPoint?,
  actions: ChartActions,
  format: (Double) -> String,
) {
  val measurer = rememberTextMeasurer()
  val currentPoints by rememberUpdatedState(points)
  val currentActions by rememberUpdatedState(actions)
  val scheme = MaterialTheme.colorScheme
  val colors = ChartColors(scheme.primary, scheme.tertiary, scheme.error, scheme.outlineVariant, scheme.onSurfaceVariant, scheme.surfaceContainer)
  val label = MaterialTheme.typography.labelSmall.copy(color = colors.text)
  Canvas(
    modifier =
      Modifier.fillMaxWidth()
        .height(CHART_HEIGHT)
        .pointerInput(window) {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val area = size.toSize()
            val index = window.nearest(currentPoints, down.position, area, TOUCH_RADIUS.toPx()) ?: return@awaitEachGesture
            val start = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() } ?: return@awaitEachGesture
            currentActions.onMove(index, window.valueAt(start.position, area))
            drag(start.id) { change ->
              change.consume()
              currentActions.onMove(index, window.valueAt(change.position, area))
            }
            currentActions.onDrop()
          }
        }
        .pointerInput(window) {
          val reach = TOUCH_RADIUS.toPx()
          detectTapGestures(
            onTap = {
              val area = size.toSize()
              if (window.nearest(currentPoints, it, area, reach) == null) currentActions.onAdd(window.valueAt(it, area))
            },
            onLongPress = { position ->
              window.nearest(currentPoints, position, size.toSize(), reach)?.let { currentActions.onRemove(it) }
            },
          )
        }
  ) {
    clipRect {
      drawGrid(window, colors, measurer, label, format)
      result?.let {
        drawGaps(window, points, it, colors.gap)
        drawCurve(window, it, colors.curve)
      }
      prediction?.let { drawPrediction(window, it, colors.curve) }
      drawPoints(window, points, colors)
    }
  }
}

private fun DrawScope.drawGrid(
  window: FitWindow,
  colors: ChartColors,
  measurer: TextMeasurer,
  label: TextStyle,
  format: (Double) -> String,
) {
  window.lines(window.left, window.right).forEach { value ->
    val x = window.xOf(value, size.width)
    drawLine(colors.grid, Offset(x, 0f), Offset(x, size.height))
    drawLabel(measurer, format(value), label, Offset(x + LABEL_GAP.toPx(), size.height - LABEL_RISE.toPx()))
  }
  window.lines(window.bottom, window.top).forEach { value ->
    val y = window.yOf(value, size.height)
    drawLine(colors.grid, Offset(0f, y), Offset(size.width, y))
    drawLabel(measurer, format(value), label, Offset(LABEL_GAP.toPx(), y))
  }
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, style: TextStyle, at: Offset) =
  drawText(measurer, text, at, style)

/** A curve can leave the chart by a lot, and lines to very far away are not drawn well. */
private fun FitWindow.isNear(y: Double) = y.isFinite() && abs(y - (top + bottom) / 2) < (top - bottom) * NEAR

private fun DrawScope.drawCurve(window: FitWindow, result: RegressionResult, color: Color) {
  val path = Path()
  var isDrawing = false
  repeat(SAMPLES + 1) { index ->
    val x = window.left + (window.right - window.left) * index / SAMPLES
    val y = result.valueAt(x)
    val isNear = window.isNear(y)
    if (isNear) {
      val px = window.xOf(x, size.width)
      val py = window.yOf(y, size.height)
      if (isDrawing) path.lineTo(px, py) else path.moveTo(px, py)
    }
    isDrawing = isNear
  }
  drawPath(path, color, style = Stroke(width = CURVE_WIDTH.dp.toPx()))
}

/** From every point straight to the curve. */
private fun DrawScope.drawGaps(window: FitWindow, points: List<DataPoint>, result: RegressionResult, color: Color) {
  points.forEach { point ->
    val y = result.valueAt(point.x)
    if (window.isNear(y)) {
      val x = window.xOf(point.x, size.width)
      drawLine(
        color,
        Offset(x, window.yOf(point.y, size.height)),
        Offset(x, window.yOf(y, size.height)),
        strokeWidth = GAP_WIDTH.dp.toPx(),
      )
    }
  }
}

private fun DrawScope.drawPrediction(window: FitWindow, prediction: DataPoint, color: Color) {
  val x = window.xOf(prediction.x, size.width)
  val y = window.yOf(prediction.y, size.height)
  val dashes = PathEffect.dashPathEffect(floatArrayOf(DASH.dp.toPx(), DASH.dp.toPx()))
  drawLine(color, Offset(x, size.height), Offset(x, y), pathEffect = dashes)
  drawLine(color, Offset(0f, y), Offset(x, y), pathEffect = dashes)
  drawCircle(color, POINT_RADIUS.toPx(), Offset(x, y), style = Stroke(width = GAP_WIDTH.dp.toPx()))
}

private fun DrawScope.drawPoints(window: FitWindow, points: List<DataPoint>, colors: ChartColors) {
  points.forEach { point ->
    val center = Offset(window.xOf(point.x, size.width), window.yOf(point.y, size.height))
    drawCircle(colors.ring, POINT_RADIUS.toPx() + GAP_WIDTH.dp.toPx(), center)
    drawCircle(colors.point, POINT_RADIUS.toPx(), center)
  }
}

private const val SAMPLES = 240
private const val NEAR = 3
private const val CURVE_WIDTH = 3
private const val GAP_WIDTH = 2
private const val DASH = 4
private val CHART_HEIGHT = 300.dp
private val POINT_RADIUS = 6.dp
private val TOUCH_RADIUS = 28.dp
private val LABEL_GAP = 3.dp
private val LABEL_RISE = 18.dp
