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

package io.github.calculatorpascaline.feature.calculator.gears

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.calculatorpascaline.feature.calculator.electrical.drawLabel
import io.github.calculatorpascaline.feature.calculator.physics.rememberClock
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/**
 * The gears to scale, turning: the input a turn every few seconds and the others at their own speed, the way they
 * go. Each center has the numbers of its gears and the way they turn.
 */
@Composable
internal fun GearDrawing(states: List<GearState>) {
  val clock = rememberClock()
  val measurer = rememberTextMeasurer()
  val scheme = MaterialTheme.colorScheme
  val fills = listOf(scheme.primary, scheme.tertiary, scheme.secondary)
  val outline = scheme.onSurface
  val hub = scheme.surfaceContainerHighest
  val style = MaterialTheme.typography.labelMedium.copy(color = scheme.onSurface, fontWeight = FontWeight.Bold)
  val direction = states.first().speed.takeIf { it.isFinite() }?.sign ?: 0.0
  val labels = remember(states) { centerLabels(states) }

  Canvas(Modifier.fillMaxWidth().height(DRAWING_HEIGHT)) {
    val placements = layoutGears(states, clock.value * TURNS_PER_SECOND * 2 * PI * direction)
    if (placements.isEmpty()) return@Canvas
    val left = placements.minOf { it.x - it.radius - ADDENDUM }
    val right = placements.maxOf { it.x + it.radius + ADDENDUM }
    val top = placements.minOf { it.y - it.radius - ADDENDUM }
    val bottom = placements.maxOf { it.y + it.radius + ADDENDUM }
    val scale = min(size.width / (right - left), size.height / (bottom - top)).toFloat()
    val origin =
      Offset(
        (size.width - (right - left).toFloat() * scale) / 2 - left.toFloat() * scale,
        (size.height - (bottom - top).toFloat() * scale) / 2 - top.toFloat() * scale,
      )
    placements.forEachIndexed { index, placement ->
      val fill = fills[index % fills.size].copy(alpha = if (states[index].sameShaft) SHAFT_ALPHA else GEAR_ALPHA)
      drawGear(placement, origin, scale, fill, outline)
    }
    labels.take(placements.size).forEach { (index, text) ->
      val center = origin + Offset(placements[index].x.toFloat(), placements[index].y.toFloat()) * scale
      drawCircle(hub, HUB_RADIUS.dp.toPx(), center)
      drawLabel(measurer, text, style, center)
    }
  }
}

/** The first gear on each shaft with the numbers of all its gears, like 2·3 ↺. */
private fun centerLabels(states: List<GearState>): List<Pair<Int, String>> {
  val labels = mutableListOf<Pair<Int, MutableList<Int>>>()
  states.forEachIndexed { index, state ->
    if (state.sameShaft && labels.isNotEmpty()) labels.last().second += index + 1 else labels += index to mutableListOf(index + 1)
  }
  return labels.map { (index, numbers) ->
    val speed = states[index].speed
    val arrow =
      when {
        speed > 0 -> " ↻"
        speed < 0 -> " ↺"
        else -> ""
      }
    index to numbers.joinToString("·") + arrow
  }
}

/** Teeth as trapezoids around the root circle, from the angle of the first one. */
private fun DrawScope.drawGear(placement: Placement, origin: Offset, scale: Float, fill: Color, outline: Color) {
  val path = Path()
  val half = PI / placement.teeth
  val root = placement.radius - DEDENDUM
  val tip = placement.radius + ADDENDUM
  fun point(radius: Double, angle: Double) =
    origin + Offset((placement.x + radius * cos(angle)).toFloat(), (placement.y + radius * sin(angle)).toFloat()) * scale
  repeat(placement.teeth) { tooth ->
    val middle = placement.angle + tooth * 2 * half
    listOf(root to -ROOT_WIDTH, tip to -TIP_WIDTH, tip to TIP_WIDTH, root to ROOT_WIDTH).forEachIndexed { corner, (radius, width) ->
      val at = point(radius, middle + width * half)
      if (tooth == 0 && corner == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
    }
  }
  path.close()
  drawPath(path, fill)
  drawPath(path, outline, style = Stroke(width = OUTLINE.dp.toPx()))
}

// Tooth sizes for a module of 1: the tip is 1 above the pitch circle and the root 1.25 below
private const val ADDENDUM = 1.0
private const val DEDENDUM = 1.25
private const val ROOT_WIDTH = 0.55
private const val TIP_WIDTH = 0.3
private const val TURNS_PER_SECOND = 0.15
private const val GEAR_ALPHA = 0.55f
private const val SHAFT_ALPHA = 0.8f
private const val HUB_RADIUS = 16
private const val OUTLINE = 1
private val DRAWING_HEIGHT = 240.dp
