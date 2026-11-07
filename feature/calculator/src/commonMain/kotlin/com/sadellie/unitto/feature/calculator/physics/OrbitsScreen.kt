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

package com.sadellie.unitto.feature.calculator.physics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import com.sadellie.unitto.feature.calculator.projectile.Step
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.orbits_escape
import unitto.core.common.generated.resources.orbits_explain_area
import unitto.core.common.generated.resources.orbits_explain_area_text
import unitto.core.common.generated.resources.orbits_explain_period
import unitto.core.common.generated.resources.orbits_explain_period_text
import unitto.core.common.generated.resources.orbits_explain_shape
import unitto.core.common.generated.resources.orbits_explain_shape_text
import unitto.core.common.generated.resources.orbits_explain_speed
import unitto.core.common.generated.resources.orbits_explain_speed_text
import unitto.core.common.generated.resources.orbits_fastest
import unitto.core.common.generated.resources.orbits_geostationary
import unitto.core.common.generated.resources.orbits_high_point
import unitto.core.common.generated.resources.orbits_hint
import unitto.core.common.generated.resources.orbits_low_point
import unitto.core.common.generated.resources.orbits_period
import unitto.core.common.generated.resources.orbits_slowest
import unitto.core.common.generated.resources.orbits_title
import unitto.core.common.generated.resources.place_tab_moon
import unitto.core.common.generated.resources.projectile_earth
import unitto.core.common.generated.resources.projectile_explain_title
import unitto.core.common.generated.resources.projectile_initial
import unitto.core.common.generated.resources.projectile_jupiter
import unitto.core.common.generated.resources.projectile_mars

private val LOW_POINT = Condition("h₁", "km", Res.string.orbits_low_point, 100.0..60_000.0, 10.0)
private val HIGH_POINT = Condition("h₂", "km", Res.string.orbits_high_point, 100.0..60_000.0, 10.0)

/** Radius in meters and G · M. */
private class Body(val name: StringResource, val radius: Double, val mu: Double)

private val BODIES =
  listOf(
    Body(Res.string.projectile_earth, 6_371_000.0, 3.986004e14),
    Body(Res.string.place_tab_moon, 1_737_400.0, 4.9048e12),
    Body(Res.string.projectile_mars, 3_389_500.0, 4.2828e13),
    Body(Res.string.projectile_jupiter, 69_911_000.0, 1.26687e17),
  )

/** Heights in kilometers of a well known orbit of the Earth. */
private class Preset(val name: StringResource?, val label: String, val low: String, val high: String)

private val PRESETS =
  listOf(
    Preset(null, "ISS", "410", "420"),
    Preset(null, "GPS", "20180", "20200"),
    Preset(Res.string.orbits_geostationary, "", "35786", "35786"),
    Preset(null, "Molniya", "500", "39800"),
  )

/** A satellite that speeds up near the planet, with the area it sweeps. */
@Composable
internal fun OrbitsRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var bodyIndex by rememberSaveable { mutableIntStateOf(0) }
  var low by rememberSaveable { mutableStateOf("500") }
  var high by rememberSaveable { mutableStateOf("39800") }
  val body = BODIES[bodyIndex]
  val orbit = Orbit(body.radius, body.mu, low.number() * METERS_PER_KM, high.number() * METERS_PER_KM)

  PhysicsScreen(
    title = Res.string.orbits_title,
    openDrawer = openDrawer,
    live = { if (orbit.isValid) OrbitCard(orbit, format) else InvalidValues() },
    settings = {
      Card(stringResource(Res.string.projectile_initial)) {
        ChipRow {
          BODIES.forEachIndexed { index, item ->
            FilterChip(
              selected = index == bodyIndex,
              onClick = { bodyIndex = index },
              label = { Text(stringResource(item.name)) },
            )
          }
        }
        ChipRow {
          PRESETS.forEach { preset ->
            FilterChip(
              selected = bodyIndex == 0 && low == preset.low && high == preset.high,
              onClick = {
                bodyIndex = 0
                low = preset.low
                high = preset.high
              },
              label = { Text(preset.name?.let { stringResource(it) } ?: preset.label) },
            )
          }
        }
        ConditionRow(LOW_POINT, low, fractional) { low = it }
        ConditionRow(HIGH_POINT, high, fractional) { high = it }
      }
    },
    explanation = { if (orbit.isValid) OrbitExplanation(orbit, format) },
  )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    content()
  }
}

private fun time(seconds: Double, format: (Double) -> String) =
  if (seconds >= SECONDS_PER_HOUR) "${format(seconds / SECONDS_PER_HOUR)} h" else "${format(seconds / SECONDS_PER_MINUTE)} min"

private fun speed(metersPerSecond: Double, format: (Double) -> String) = "${format(metersPerSecond / METERS_PER_KM)} km/s"

@Composable
private fun OrbitCard(orbit: Orbit, format: (Double) -> String) {
  val clock = rememberClock()
  val planet = MaterialTheme.colorScheme.primary
  val path = MaterialTheme.colorScheme.outline
  val swept = MaterialTheme.colorScheme.tertiaryContainer
  val satellite = MaterialTheme.colorScheme.tertiary
  LiveCard {
    Canvas(Modifier.fillMaxWidth().height(SCENE_HEIGHT)) {
      val part = (clock.value / LAP_SECONDS).mod(1.0)
      val space = Space(orbit, size.width, size.height)
      drawSwept(orbit, space, part, swept)
      drawOval(path, topLeft = space.orbitBox.topLeft, size = space.orbitBox.size, style = Stroke(LINE_WIDTH.toPx()))
      drawCircle(planet, radius = (orbit.radius * space.scale).toFloat().coerceAtLeast(MIN_PLANET.toPx()), center = space.focus)
      drawSatellite(orbit, space, part, satellite)
    }
    Text(
      stringResource(Res.string.orbits_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Readout(orbit, clock, format)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.orbits_period, time(orbit.period, format))
      Answer(Modifier.weight(1f), Res.string.orbits_fastest, speed(orbit.fastest, format))
      Answer(Modifier.weight(1f), Res.string.orbits_slowest, speed(orbit.slowest, format))
    }
  }
}

/** Height and speed right now. Only this text changes on each frame. */
@Composable
private fun Readout(orbit: Orbit, clock: State<Double>, format: (Double) -> String) {
  val point = orbit.at((clock.value / LAP_SECONDS).mod(1.0))
  Text(
    "h = ${((point.distance - orbit.radius) / METERS_PER_KM).rounded(0)} km · v = ${speed(orbit.speedAt(point.distance), format)}",
    style = MaterialTheme.typography.labelMedium,
  )
}

/** The whole ellipse on the screen, the planet on the right so that the long side is drawn wide. */
private class Space(val orbit: Orbit, width: Float, height: Float) {
  val scale =
    min(
      width * FILL / (2 * orbit.semiMajorAxis),
      height * FILL / (2 * orbit.semiMinorAxis),
    )
  val middle = Offset(width / 2, height / 2)
  val focus = middle + Offset((orbit.semiMajorAxis * orbit.eccentricity * scale).toFloat(), 0f)
  val orbitBox: Rect =
    (orbit.semiMajorAxis * scale).toFloat().let { a ->
      val b = (orbit.semiMinorAxis * scale).toFloat()
      Rect(middle.x - a, middle.y - b, middle.x + a, middle.y + b)
    }

  fun place(point: OrbitPoint) = focus + Offset((point.x * scale).toFloat(), -(point.y * scale).toFloat())
}

/** The area swept in the same time is always the same, whatever the speed. */
private fun DrawScope.drawSwept(orbit: Orbit, space: Space, part: Double, color: Color) {
  val path = Path().apply { moveTo(space.focus.x, space.focus.y) }
  repeat(WEDGE_STEPS + 1) { index ->
    val point = space.place(orbit.at(part - WEDGE * (WEDGE_STEPS - index) / WEDGE_STEPS))
    path.lineTo(point.x, point.y)
  }
  path.close()
  drawPath(path, color)
}

/** The satellite with an arrow as long as it is fast. */
private fun DrawScope.drawSatellite(orbit: Orbit, space: Space, part: Double, color: Color) {
  val point = orbit.at(part)
  val place = space.place(point)
  val dx = -orbit.semiMajorAxis * sin(point.anomaly)
  val dy = orbit.semiMinorAxis * cos(point.anomaly)
  val length = ARROW.toPx() * orbit.speedAt(point.distance) / orbit.fastest / hypot(dx, dy)
  drawLine(color, place, place + Offset((dx * length).toFloat(), -(dy * length).toFloat()), strokeWidth = ARROW_WIDTH.toPx())
  drawCircle(color, radius = SATELLITE.toPx(), center = place)
}

@Composable
private fun OrbitExplanation(orbit: Orbit, format: (Double) -> String) {
  val r1 = (orbit.periapsis / METERS_PER_KM).rounded(0)
  val r2 = (orbit.apoapsis / METERS_PER_KM).rounded(0)
  val a = (orbit.semiMajorAxis / METERS_PER_KM).rounded(0)
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.orbits_explain_shape,
      Res.string.orbits_explain_shape_text,
      "a = (r₁ + r₂) / 2 = ($r1 + $r2) / 2 = $a km",
      "e = (r₂ − r₁) / (r₂ + r₁) = ${orbit.eccentricity.rounded(ECCENTRICITY_DECIMALS)}",
    )
    Step(
      Res.string.orbits_explain_period,
      Res.string.orbits_explain_period_text,
      "T = 2π · √(a³ / μ) = ${time(orbit.period, format)}",
    )
    Step(
      Res.string.orbits_explain_speed,
      Res.string.orbits_explain_speed_text,
      "v = √(μ · (2 / r − 1 / a))",
      "v₁ = ${speed(orbit.fastest, format)} · v₂ = ${speed(orbit.slowest, format)}",
    )
    Step(
      Res.string.orbits_explain_area,
      Res.string.orbits_explain_area_text,
      "r₁ · v₁ = r₂ · v₂",
      "${stringResource(Res.string.orbits_escape)} = √(2 · μ / r₁) = ${speed(orbit.escapeSpeed, format)}",
    )
  }
}

private const val METERS_PER_KM = 1000.0
private const val SECONDS_PER_HOUR = 3600.0
private const val SECONDS_PER_MINUTE = 60.0
private const val LAP_SECONDS = 10.0
private const val FILL = 0.92
private const val WEDGE = 1.0 / 12
private const val WEDGE_STEPS = 24
private const val ECCENTRICITY_DECIMALS = 3
private val SCENE_HEIGHT = 260.dp
private val MIN_PLANET = 3.dp
private val SATELLITE = 6.dp
private val ARROW = 44.dp
private val ARROW_WIDTH = 2.5.dp
private val LINE_WIDTH = 1.5.dp
