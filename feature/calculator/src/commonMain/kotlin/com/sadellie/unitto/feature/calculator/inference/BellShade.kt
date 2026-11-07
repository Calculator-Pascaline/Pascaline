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

package com.sadellie.unitto.feature.calculator.inference

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.functions.formatTick
import com.sadellie.unitto.feature.calculator.functions.gridValues
import com.sadellie.unitto.feature.calculator.functions.niceStep
import kotlin.math.max
import kotlin.math.min

/** What is filled under the curves. */
internal sealed interface BellShade {
  /** The p-value: results at least as far as [value] from the center of the first curve. */
  data class Tails(val value: Double, val alternative: Alternative) : BellShade

  /** Where two groups can't be told apart. */
  data object Overlap : BellShade

  /** The confidence interval. */
  data class Middle(val low: Double, val high: Double) : BellShade
}

/** A bell curve, or two on the same axis, that slide to their new place. */
@Composable
internal fun BellChart(first: Bell, second: Bell?, shade: BellShade) {
  val firstCenter by animateFloatAsState(first.center.toFloat(), label = "First center")
  val firstSpread by animateFloatAsState(first.spread.toFloat(), label = "First spread")
  val secondCenter by animateFloatAsState((second ?: first).center.toFloat(), label = "Second center")
  val secondSpread by animateFloatAsState((second ?: first).spread.toFloat(), label = "Second spread")
  val firstColor = MaterialTheme.colorScheme.primary
  val secondColor = MaterialTheme.colorScheme.tertiary
  val axis = MaterialTheme.colorScheme.outline
  val labels = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
  val measurer = rememberTextMeasurer()
  Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
    val shown = Bell(firstCenter.toDouble(), firstSpread.toDouble())
    val other = Bell(secondCenter.toDouble(), secondSpread.toDouble()).takeIf { second != null }
    val area = PlotWindow(shown, other, size.width, size.height - AXIS_HEIGHT.toPx())
    when (shade) {
      is BellShade.Tails -> drawTails(area, shown, shade, secondColor)
      BellShade.Overlap -> other?.let { drawFilled(area, axis) { x -> min(shown.density(x), it.density(x)) } }
      is BellShade.Middle -> drawFilled(area, firstColor.copy(alpha = SHADE_ALPHA)) { x ->
        if (x in shade.low..shade.high) shown.density(x) else 0.0
      }
    }
    drawCurve(area, shown, firstColor)
    other?.let { drawCurve(area, it, secondColor) }
    val bottom = size.height - AXIS_HEIGHT.toPx()
    drawLine(axis, Offset(0f, bottom), Offset(size.width, bottom))
    val step = niceStep((area.to - area.from) / TICKS)
    gridValues(area.from, area.to, step).forEach { tick ->
      val layout = measurer.measure(formatTick(tick, step), labels)
      drawText(layout, topLeft = Offset(area.xOf(tick) - layout.size.width / 2, bottom + LABEL_GAP.toPx()))
    }
  }
}

/** The axis around the curves, the highest point of one reaches the top. */
private class PlotWindow(first: Bell, second: Bell?, val width: Float, val height: Float) {
  val from = min(first.center - first.spread * REACH, second?.let { it.center - it.spread * REACH } ?: Double.MAX_VALUE)
  val to = max(first.center + first.spread * REACH, second?.let { it.center + it.spread * REACH } ?: -Double.MAX_VALUE)
  private val top = max(first.density(first.center), second?.density(second.center) ?: 0.0)

  fun xOf(value: Double) = ((value - from) / (to - from) * width).toFloat()

  fun valueAt(index: Int) = from + (to - from) * index / SAMPLES

  fun yOf(density: Double) = height - (density / top * height * TOP_SHARE).toFloat()
}

private fun DrawScope.drawCurve(area: PlotWindow, bell: Bell, color: Color) {
  drawFilled(area, color.copy(alpha = FILL_ALPHA), bell::density)
  val path = Path()
  repeat(SAMPLES + 1) { index ->
    val x = area.valueAt(index)
    if (index == 0) path.moveTo(area.xOf(x), area.yOf(bell.density(x))) else path.lineTo(area.xOf(x), area.yOf(bell.density(x)))
  }
  drawPath(path, color, style = Stroke(width = LINE_WIDTH.toPx()))
  val center = area.xOf(bell.center)
  drawLine(color, Offset(center, area.yOf(bell.density(bell.center))), Offset(center, area.height), strokeWidth = MARK_WIDTH.toPx())
}

private fun DrawScope.drawFilled(area: PlotWindow, color: Color, height: (Double) -> Double) {
  val path = Path().apply { moveTo(0f, area.height) }
  repeat(SAMPLES + 1) { index ->
    val x = area.valueAt(index)
    path.lineTo(area.xOf(x), area.yOf(height(x)))
  }
  path.lineTo(area.width, area.height)
  path.close()
  drawPath(path, color)
}

private fun DrawScope.drawTails(area: PlotWindow, bell: Bell, tails: BellShade.Tails, color: Color) {
  val distance = kotlin.math.abs(tails.value - bell.center)
  val isShaded: (Double) -> Boolean =
    when (tails.alternative) {
      Alternative.LESS -> { x -> x <= tails.value }
      Alternative.GREATER -> { x -> x >= tails.value }
      Alternative.NOT_EQUAL -> { x -> kotlin.math.abs(x - bell.center) >= distance }
    }
  drawFilled(area, color) { x -> if (isShaded(x)) bell.density(x) else 0.0 }
}

/** Observed and expected counts next to each other. */
@Composable
internal fun CountBars(observed: List<Double>, expected: List<Double>) {
  val firstColor = MaterialTheme.colorScheme.primary
  val secondColor = MaterialTheme.colorScheme.tertiary
  Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
    val top = (observed + expected).maxOrNull()?.takeIf { it > 0 } ?: return@Canvas
    val group = size.width / observed.size
    val width = group * BAR_SHARE
    val corner = CornerRadius(BAR_CORNER.toPx())
    fun bar(value: Double, left: Float, color: Color) {
      val height = (value / top).toFloat() * size.height
      drawRoundRect(color, Offset(left, size.height - height), Size(width, height), corner)
    }
    observed.indices.forEach { index ->
      bar(observed[index], index * group + group / 2 - width, firstColor)
      bar(expected[index], index * group + group / 2, secondColor)
    }
  }
}

/** What each color means. */
@Composable
internal fun ChartLegend(first: String, second: String?) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    LegendItem(MaterialTheme.colorScheme.primary, first, MaterialTheme.typography.labelMedium)
    second?.let { LegendItem(MaterialTheme.colorScheme.tertiary, it, MaterialTheme.typography.labelMedium) }
  }
}

@Composable
private fun LegendItem(color: Color, name: String, style: TextStyle) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(Modifier.size(LEGEND_DOT).clip(CircleShape).background(color))
    Text(name, style = style)
  }
}

/** How many spreads of a curve are shown on each side. */
private const val REACH = 3.6
private const val TOP_SHARE = 0.94
private const val SAMPLES = 160
private const val TICKS = 5.0
private const val FILL_ALPHA = 0.18f
private const val SHADE_ALPHA = 0.45f
private const val BAR_SHARE = 0.32f
private val CHART_HEIGHT = 200.dp
private val AXIS_HEIGHT = 22.dp
private val LABEL_GAP = 4.dp
private val LINE_WIDTH = 2.5.dp
private val MARK_WIDTH = 1.5.dp
private val BAR_CORNER = 4.dp
private val LEGEND_DOT = 10.dp
