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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.calculatorpascaline.feature.calculator.electrical.drawLabel
import io.github.calculatorpascaline.feature.calculator.physics.rememberClock
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/** Where the drawing of placements goes on the canvas: [origin] of the layout and [scale] of a module. */
internal class Frame(val origin: Offset, val scale: Float) {
  fun at(x: Double, y: Double) = origin + Offset(x.toFloat(), y.toFloat()) * scale

  fun layout(point: Offset): Pair<Double, Double> =
    ((point.x - origin.x) / scale).toDouble() to ((point.y - origin.y) / scale).toDouble()
}

/** The frame that fits all of [placements] in [size], centered. */
internal fun frameOf(placements: List<Placement>, size: Size): Frame? {
  if (placements.isEmpty()) return null
  val extents = placements.map { it.extent() }
  val left = extents.minOf { it[0] }
  val right = extents.maxOf { it[1] }
  val top = extents.minOf { it[2] }
  val bottom = extents.maxOf { it[3] }
  val scale = min(size.width / (right - left), size.height / (bottom - top)).toFloat()
  return Frame(
    Offset(
      (size.width - (right - left).toFloat() * scale) / 2 - left.toFloat() * scale,
      (size.height - (bottom - top).toFloat() * scale) / 2 - top.toFloat() * scale,
    ),
    scale,
  )
}

/**
 * The gears to scale, turning: the input a turn every few seconds and the others at their own speed, the way they
 * go. Gears further back are lighter. Each center has the numbers of its gears and the way they turn. A tap on a gear
 * gives its number to [onGearTap], from 0 for the input.
 */
@Composable
internal fun GearDrawing(states: List<GearState>, onGearTap: (Int) -> Unit) {
  val clock = rememberClock()
  val measurer = rememberTextMeasurer()
  val scheme = MaterialTheme.colorScheme
  val colors = GearColors(listOf(scheme.primary, scheme.tertiary, scheme.secondary), scheme.onSurface, scheme.surfaceContainerHighest)
  val style = MaterialTheme.typography.labelMedium.copy(color = scheme.onSurface, fontWeight = FontWeight.Bold)
  val direction = states.first().speed.takeIf { it.isFinite() }?.sign ?: 0.0
  val labels = remember(states) { centerLabels(states) }
  val currentStates = rememberUpdatedState(states)
  fun turn() = clock.value * TURNS_PER_SECOND * 2 * PI * direction

  Canvas(
    Modifier.fillMaxWidth().height(DRAWING_HEIGHT).pointerInput(Unit) {
      detectTapGestures { tap ->
        val placements = layoutGears(currentStates.value, turn())
        val frame = frameOf(placements, Size(size.width.toFloat(), size.height.toFloat())) ?: return@detectTapGestures
        val (x, y) = frame.layout(tap)
        // The gear in front is the one tapped
        placements.lastOrNull { hypot(it.x - x, it.y - y) <= it.radius + ADDENDUM }?.let { onGearTap(it.state) }
      }
    }
  ) {
    val placements = layoutGears(states, turn())
    val frame = frameOf(placements, size) ?: return@Canvas
    val levels = placements.maxOf { it.level } + 1
    placements.forEach { placement ->
      val fill = colors.fill(placement.state, placement.level, levels)
      placement.beltFrom?.let { drawBelt(placements[it], placement, frame, colors.outline) }
      drawPlacement(placement, frame, fill, colors.outline)
    }
    labels.forEach { (index, text) ->
      val placement = placements.firstOrNull { it.state == index && it.shape != Shape.WORM } ?: return@forEach
      val center = if (placement.shape == Shape.RACK) frame.at(placement.x, placement.y + RACK_DEPTH / 2) else frame.at(placement.x, placement.y)
      drawCircle(colors.hub, HUB_RADIUS.dp.toPx(), center)
      drawLabel(measurer, text, style, center)
    }
  }
}

/** Colors of the gears, one after the other, lighter further back. */
internal class GearColors(private val fills: List<Color>, val outline: Color, val hub: Color) {
  fun fill(index: Int, level: Int, levels: Int): Color =
    fills[index % fills.size].copy(alpha = BACK_ALPHA + (FRONT_ALPHA - BACK_ALPHA) * (level + 1) / levels)
}

internal fun DrawScope.drawPlacement(placement: Placement, frame: Frame, fill: Color, outline: Color) {
  when (placement.shape) {
    Shape.GEAR -> drawGear(placement, frame, fill, outline)
    Shape.RING -> drawRing(placement, frame, fill, outline)
    Shape.WORM -> drawWorm(placement, frame, fill, outline)
    Shape.WHEEL -> drawWheel(placement, frame, fill, outline)
    Shape.RACK -> drawRack(placement, frame, fill, outline)
  }
}

/** The first gear on each shaft with the numbers of all its gears, like 2·3 ↺. */
private fun centerLabels(states: List<GearState>): List<Pair<Int, String>> {
  val labels = mutableListOf<Pair<Int, MutableList<Int>>>()
  states.forEachIndexed { index, state ->
    if (state.link == Link.SHAFT && labels.isNotEmpty()) labels.last().second += index + 1 else labels += index to mutableListOf(index + 1)
  }
  return labels.map { (index, numbers) ->
    val state = states[index]
    val arrow =
      when {
        state.link == Link.RACK -> if (state.speed > 0) " ←" else " →"
        state.rightAngle -> " ⟂"
        state.speed > 0 -> " ↻"
        state.speed < 0 -> " ↺"
        else -> ""
      }
    index to numbers.joinToString("·") + arrow
  }
}

/** Teeth as trapezoids around the root circle, from the angle of the first one, pointing out or in for a ring. */
private fun toothPath(placement: Placement, frame: Frame, inside: Boolean): Path {
  val path = Path()
  val half = PI / placement.teeth
  val root = if (inside) placement.radius + DEDENDUM else placement.radius - DEDENDUM
  val tip = if (inside) placement.radius - ADDENDUM else placement.radius + ADDENDUM
  repeat(placement.teeth) { tooth ->
    val middle = placement.angle + tooth * 2 * half
    listOf(root to -ROOT_WIDTH, tip to -TIP_WIDTH, tip to TIP_WIDTH, root to ROOT_WIDTH).forEachIndexed { corner, (radius, width) ->
      val angle = middle + width * half
      val at = frame.at(placement.x + radius * cos(angle), placement.y + radius * sin(angle))
      if (tooth == 0 && corner == 0) path.moveTo(at.x, at.y) else path.lineTo(at.x, at.y)
    }
  }
  path.close()
  return path
}

private fun DrawScope.drawGear(placement: Placement, frame: Frame, fill: Color, outline: Color) {
  val path = toothPath(placement, frame, inside = false)
  drawPath(path, fill)
  drawPath(path, outline, style = Stroke(width = OUTLINE.dp.toPx()))
}

/** A ring: its outside is round, its teeth point to the middle, where the gear it turns with sits. */
internal fun DrawScope.drawRing(placement: Placement, frame: Frame, fill: Color, outline: Color) {
  val teeth = toothPath(placement, frame, inside = true)
  val outside = Path()
  val center = frame.at(placement.x, placement.y)
  val radius = ((placement.radius + RIM) * frame.scale).toFloat()
  outside.addOval(Rect(center, radius))
  val ring = Path().apply {
    fillType = PathFillType.EvenOdd
    addPath(outside)
    addPath(teeth)
  }
  drawPath(ring, fill)
  drawPath(outside, outline, style = Stroke(width = OUTLINE.dp.toPx()))
  drawPath(teeth, outline, style = Stroke(width = OUTLINE.dp.toPx()))
}

/** A worm from its end: a disc with its thread winding out, turning with its shaft. */
private fun DrawScope.drawWorm(placement: Placement, frame: Frame, fill: Color, outline: Color) {
  val center = frame.at(placement.x, placement.y)
  val radius = (placement.radius * frame.scale).toFloat()
  drawCircle(fill, radius, center)
  drawCircle(outline, radius, center, style = Stroke(width = OUTLINE.dp.toPx()))
  val thread = Path()
  repeat(SPIRAL_POINTS + 1) { step ->
    val share = step.toDouble() / SPIRAL_POINTS
    val angle = placement.angle + share * 2 * PI
    val at = frame.at(placement.x + share * placement.radius * cos(angle), placement.y + share * placement.radius * sin(angle))
    if (step == 0) thread.moveTo(at.x, at.y) else thread.lineTo(at.x, at.y)
  }
  drawPath(thread, outline, style = Stroke(width = (OUTLINE * 2).dp.toPx()))
}

/** A worm wheel from its edge: a long bar across its diameter, with its teeth at both ends. */
private fun DrawScope.drawWheel(placement: Placement, frame: Frame, fill: Color, outline: Color) {
  val center = frame.at(placement.x, placement.y)
  val length = (placement.radius * 2 * frame.scale).toFloat()
  val thickness = (FACE * frame.scale).toFloat()
  rotate((placement.direction * HALF_TURN_DEGREES / PI).toFloat(), center) {
    val topLeft = Offset(center.x - length / 2, center.y - thickness / 2)
    drawRect(fill, topLeft, Size(length, thickness))
    drawRect(outline, topLeft, Size(length, thickness), style = Stroke(width = OUTLINE.dp.toPx()))
    val tooth = (ADDENDUM * 2 * frame.scale).toFloat()
    listOf(topLeft.x, topLeft.x + length - tooth).forEach { x ->
      drawRect(outline.copy(alpha = TOOTH_ALPHA), Offset(x, topLeft.y), Size(tooth, thickness))
    }
  }
}

/** A rack: a bar with its teeth up, moved along with the gap at [Placement.angle]. */
private fun DrawScope.drawRack(placement: Placement, frame: Frame, fill: Color, outline: Color) {
  val left = placement.x - placement.radius
  val right = placement.x + placement.radius
  val path = Path()
  val start = frame.at(left, placement.y + RACK_DEPTH)
  path.moveTo(start.x, start.y)
  // Teeth one circular pitch (π for a module of 1) apart, from the first gap left of the bar
  val first = placement.angle - PI * floor((placement.angle - left) / PI) - PI
  var gap = first
  while (gap < right + PI) {
    val tooth = gap + PI / 2
    listOf(tooth - RACK_ROOT to DEDENDUM, tooth - RACK_TIP to -ADDENDUM, tooth + RACK_TIP to -ADDENDUM, tooth + RACK_ROOT to DEDENDUM)
      .forEach { (x, y) ->
        val at = frame.at(x.coerceIn(left, right), placement.y + y)
        path.lineTo(at.x, at.y)
      }
    gap += PI
  }
  val end = frame.at(right, placement.y + RACK_DEPTH)
  path.lineTo(end.x, end.y)
  path.close()
  drawPath(path, fill)
  drawPath(path, outline, style = Stroke(width = OUTLINE.dp.toPx()))
}

/** A belt or chain around two pulleys: the two straight runs that touch both pitch circles on the outside. */
private fun DrawScope.drawBelt(from: Placement, to: Placement, frame: Frame, color: Color) {
  val distance = hypot(to.x - from.x, to.y - from.y)
  val toward = atan2(to.y - from.y, to.x - from.x)
  val opening = acos(((from.radius - to.radius) / distance).coerceIn(-1.0, 1.0))
  listOf(toward + opening, toward - opening).forEach { angle ->
    drawLine(
      color,
      frame.at(from.x + from.radius * cos(angle), from.y + from.radius * sin(angle)),
      frame.at(to.x + to.radius * cos(angle), to.y + to.radius * sin(angle)),
      strokeWidth = BELT_WIDTH.dp.toPx(),
    )
  }
}

// Shape of the teeth of a gear, as shares of half a tooth's angle
private const val ROOT_WIDTH = 0.55
private const val TIP_WIDTH = 0.3
// Half the width of a rack tooth at its root and at its tip, for a module of 1 and 20° flanks
private const val RACK_ROOT = 1.24
private const val RACK_TIP = 0.42
private const val TURNS_PER_SECOND = 0.15
private const val BACK_ALPHA = 0.25f
private const val FRONT_ALPHA = 0.8f
private const val TOOTH_ALPHA = 0.35f
private const val HUB_RADIUS = 16
private const val OUTLINE = 1
private const val BELT_WIDTH = 3
private const val SPIRAL_POINTS = 24
private const val HALF_TURN_DEGREES = 180.0
private val DRAWING_HEIGHT = 240.dp
