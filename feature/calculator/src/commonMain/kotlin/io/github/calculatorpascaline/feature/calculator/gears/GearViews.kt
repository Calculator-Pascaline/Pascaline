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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.calculatorpascaline.feature.calculator.physics.rememberClock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/**
 * The train seen from the side: a row for each level, back at the top, with every gear as wide as it is across, and
 * the shafts through them. Gears on the same row turn in the same plane; those on different rows can overlap.
 */
@Composable
internal fun GearSideView(states: List<GearState>) {
  val placements =
    remember(states) { layoutGears(states, 0.0).filter { it.shape == Shape.GEAR || it.shape == Shape.RING } }
  if (placements.isEmpty()) return
  val scheme = MaterialTheme.colorScheme
  val colors = GearColors(listOf(scheme.primary, scheme.tertiary, scheme.secondary), scheme.onSurface, scheme.surfaceContainerHighest)
  val levels = placements.maxOf { it.level } + 1
  Canvas(Modifier.fillMaxWidth().height(ROW_HEIGHT * levels + ROW_HEIGHT / 2)) {
    val left = placements.minOf { it.x - it.radius - ADDENDUM }
    val right = placements.maxOf { it.x + it.radius + ADDENDUM }
    val scale = (size.width / (right - left)).toFloat()
    val row = ROW_HEIGHT.toPx()
    fun x(value: Double) = ((value - left) * scale).toFloat()
    fun middle(level: Int) = row * (level + ROW_MIDDLE)
    // Each shaft from its first gear to its last
    placements.groupBy { it.x to it.y }.values.forEach { onShaft ->
      val top = middle(onShaft.minOf { it.level }) - row / 2
      val bottom = middle(onShaft.maxOf { it.level }) + row / 2
      drawLine(colors.outline, Offset(x(onShaft.first().x), top), Offset(x(onShaft.first().x), bottom), SHAFT_WIDTH.dp.toPx())
    }
    placements.forEach { placement ->
      val topLeft = Offset(x(placement.x - placement.radius), middle(placement.level) - row * FACE_SHARE / 2)
      val size = Size(x(placement.x + placement.radius) - topLeft.x, row * FACE_SHARE)
      drawRect(colors.fill(placement.state, placement.level, levels), topLeft, size)
      drawRect(colors.outline, topLeft, size, style = Stroke(width = 1.dp.toPx()))
    }
  }
}

/**
 * A planetary set turning: the sun in the middle, the planets on the carrier's arms and the ring around them. The part
 * held still is gray.
 */
@Composable
internal fun PlanetaryDrawing(set: Planetary, result: PlanetaryResult) {
  val clock = rememberClock()
  val scheme = MaterialTheme.colorScheme
  val held = scheme.outline.copy(alpha = HELD_ALPHA)
  val outline = scheme.onSurface
  val sunColor = if (set.fixed == Member.SUN) held else scheme.primary.copy(alpha = PLANETARY_ALPHA)
  val planetColor = scheme.tertiary.copy(alpha = PLANETARY_ALPHA)
  val ringColor = if (set.fixed == Member.RING) held else scheme.secondary.copy(alpha = PLANETARY_ALPHA)
  val carrierColor = if (set.fixed == Member.CARRIER) held else scheme.onSurfaceVariant.copy(alpha = CARRIER_ALPHA)
  val planetTeeth = result.planetTeeth
  val count = set.planets
  if (!set.drawable(planetTeeth)) return
  Canvas(Modifier.fillMaxWidth().height(PLANETARY_HEIGHT)) {
    val inputTurn = clock.value * TURNS_PER_SECOND * 2 * PI * set.speed.sign
    fun angle(member: Member) =
      if (set.speed == 0.0) 0.0 else inputTurn * (result.speeds.getValue(member) / abs(set.speed))
    val sunAngle = angle(Member.SUN)
    val carrierAngle = angle(Member.CARRIER)
    val reach = set.ring / 2 + RIM
    val scale = (min(size.width, size.height) / (reach * 2)).toFloat()
    val frame = Frame(Offset(size.width / 2, size.height / 2), scale)
    val arm = (set.sun + planetTeeth) / 2
    val planets =
      (0 until count.toInt()).map { index ->
        val toward = carrierAngle + index * 2 * PI / count
        Placement(
          arm * cos(toward),
          arm * sin(toward),
          planetTeeth / 2,
          planetTeeth.toInt(),
          toward + PI - PI / planetTeeth - (sunAngle - toward) * set.sun / planetTeeth,
        )
      }
    // The ring meshes with the first planet on its inside, so it follows it
    val first = planets.first()
    val touch = carrierAngle
    val ring =
      Placement(0.0, 0.0, set.ring / 2, set.ring.toInt(), touch + PI / set.ring + (first.angle - touch) * planetTeeth / set.ring, Shape.RING)
    drawPlacement(ring, frame, ringColor, outline)
    planets.forEach { planet ->
      drawLine(carrierColor, frame.at(0.0, 0.0), frame.at(planet.x, planet.y), ARM_WIDTH.dp.toPx())
    }
    drawPlacement(Placement(0.0, 0.0, set.sun / 2, set.sun.toInt(), sunAngle), frame, sunColor, outline)
    planets.forEach { planet ->
      drawPlacement(planet, frame, planetColor, outline)
      drawCircle(carrierColor, PIN_RADIUS.dp.toPx(), frame.at(planet.x, planet.y))
    }
  }
}

/** Whether there are whole planets to draw. */
private fun Planetary.drawable(planetTeeth: Double): Boolean {
  val whole = listOf(planetTeeth, planets, sun).all { it >= 1 && it == floor(it) }
  val bigEnough = planetTeeth >= MIN_TEETH && sun >= MIN_TEETH
  return whole && bigEnough && planets <= MAX_DRAWN_PLANETS
}

private const val TURNS_PER_SECOND = 0.15
private const val FACE_SHARE = 0.5f
// Rows start under half a row of space, and their gears sit in their middle
private const val ROW_MIDDLE = 0.75f
private const val SHAFT_WIDTH = 2
private const val ARM_WIDTH = 10
private const val PIN_RADIUS = 5
private const val HELD_ALPHA = 0.45f
private const val PLANETARY_ALPHA = 0.7f
private const val CARRIER_ALPHA = 0.5f
private const val MAX_DRAWN_PLANETS = 12
private val ROW_HEIGHT = 28.dp
private val PLANETARY_HEIGHT = 260.dp
