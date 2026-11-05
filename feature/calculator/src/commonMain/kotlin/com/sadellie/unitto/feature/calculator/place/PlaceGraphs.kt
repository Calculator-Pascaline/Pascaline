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

package com.sadellie.unitto.feature.calculator.place

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Curves, values over the graph's width with their color, with a dashed [baseline] (the horizon, mean sea level), and a marker at [selected] that a
 * tap or a drag moves.
 */
@Composable
internal fun CurveGraph(
  series: List<Pair<List<Double>, Color>>,
  selected: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
  baseline: Double? = null,
  height: Dp = GRAPH_HEIGHT,
) {
  val count = series.maxOf { it.first.size }
  val currentCount by rememberUpdatedState(count)
  val currentOnSelect by rememberUpdatedState(onSelect)
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val markerColor = MaterialTheme.colorScheme.onSurface
  fun indexAt(x: Float, width: Int) = ((x / width) * (currentCount - 1)).roundToInt().coerceIn(0, currentCount - 1)
  Canvas(
    modifier =
      modifier.fillMaxWidth()
        .height(height)
        .pointerInput(Unit) { detectTapGestures { currentOnSelect(indexAt(it.x, size.width)) } }
        .pointerInput(Unit) {
          detectHorizontalDragGestures { change, _ ->
            change.consume()
            currentOnSelect(indexAt(change.position.x, size.width))
          }
        }
  ) {
    val all = series.flatMap { it.first } + listOfNotNull(baseline)
    val low = all.min()
    val high = all.max().let { if (it - low < MIN_RANGE) low + MIN_RANGE else it }
    val pad = PADDING.dp.toPx()
    fun x(index: Int) = index.toFloat() / (count - 1).coerceAtLeast(1) * size.width
    fun y(value: Double) = (pad + (1 - (value - low) / (high - low)) * (size.height - 2 * pad)).toFloat()
    if (baseline != null) {
      drawLine(
        gridColor,
        Offset(0f, y(baseline)),
        Offset(size.width, y(baseline)),
        strokeWidth = 1.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
      )
    }
    for ((values, color) in series) {
      val path = Path()
      values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
      drawPath(path, color, style = Stroke(LINE_WIDTH.dp.toPx()))
    }
    val markerX = x(selected.coerceIn(0, count - 1))
    drawLine(markerColor, Offset(markerX, 0f), Offset(markerX, size.height), strokeWidth = 1.dp.toPx())
    for ((values, color) in series) {
      values.getOrNull(selected)?.let { drawCircle(color, MARKER_RADIUS.dp.toPx(), Offset(markerX, y(it))) }
    }
  }
}

/** One bar a month, the selected one stronger. A tap selects a month. */
@Composable
internal fun MonthBars(values: DoubleArray, labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
  val currentOnSelect by rememberUpdatedState(onSelect)
  val strong = MaterialTheme.colorScheme.primary
  val weak = MaterialTheme.colorScheme.primary.copy(alpha = WEAK_ALPHA)
  val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
  val measurer = rememberTextMeasurer()
  Canvas(
    modifier =
      Modifier.fillMaxWidth().height(GRAPH_HEIGHT).pointerInput(Unit) {
        detectTapGestures { currentOnSelect((it.x / size.width * values.size).toInt().coerceIn(0, values.size - 1)) }
      }
  ) {
    val labelHeight = LABEL_HEIGHT.dp.toPx()
    val slot = size.width / values.size
    val top = values.max().coerceAtLeast(1.0)
    values.forEachIndexed { i, value ->
      val barHeight = ((value / top) * (size.height - labelHeight)).toFloat()
      drawRoundRect(
        color = if (i == selected) strong else weak,
        topLeft = Offset(i * slot + slot * BAR_GAP, size.height - labelHeight - barHeight),
        size = Size(slot * (1 - 2 * BAR_GAP), barHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
      )
      val label = measurer.measure(labels.getOrNull(i).orEmpty(), labelStyle)
      drawText(label, topLeft = Offset(i * slot + (slot - label.size.width) / 2, size.height - label.size.height))
    }
  }
}

/** The Moon as seen from the north: lit on the right while waxing. */
@Composable
internal fun MoonDisc(illumination: Double, isWaxing: Boolean, modifier: Modifier = Modifier, diameter: Dp = 72.dp) {
  val lit = MaterialTheme.colorScheme.onSurface
  val dark = MaterialTheme.colorScheme.surfaceContainerHighest
  Canvas(modifier = modifier.size(diameter)) {
    val r = size.minDimension / 2
    val center = Offset(size.width / 2, size.height / 2)
    drawCircle(dark, r, center)
    val disc = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
    // Half of the disc on the lit side, then the terminator, an ellipse, added or taken away
    val half = Path().apply { arcTo(disc, if (isWaxing) -90f else 90f, 180f, true) }
    val terminatorWidth = (abs(1 - 2 * illumination) * r).toFloat()
    val ellipse = Path().apply { addOval(Rect(center.x - terminatorWidth, center.y - r, center.x + terminatorWidth, center.y + r)) }
    val shape = Path().apply {
      op(half, ellipse, if (illumination > 0.5) PathOperation.Union else PathOperation.Difference)
    }
    drawPath(shape, lit)
  }
}

private val GRAPH_HEIGHT = 140.dp
private const val PADDING = 8
private const val LINE_WIDTH = 2.5f
private const val MARKER_RADIUS = 5
private const val MIN_RANGE = 0.1
private const val WEAK_ALPHA = 0.35f
private const val LABEL_HEIGHT = 16
private const val BAR_GAP = 0.12f
