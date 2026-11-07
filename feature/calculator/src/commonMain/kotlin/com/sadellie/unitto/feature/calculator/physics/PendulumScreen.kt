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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import com.sadellie.unitto.feature.calculator.projectile.GRAVITY
import com.sadellie.unitto.feature.calculator.projectile.PlanetChips
import com.sadellie.unitto.feature.calculator.projectile.Step
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.pendulum_damping
import unitto.core.common.generated.resources.pendulum_explain_energy
import unitto.core.common.generated.resources.pendulum_explain_energy_text
import unitto.core.common.generated.resources.pendulum_explain_real
import unitto.core.common.generated.resources.pendulum_explain_real_text
import unitto.core.common.generated.resources.pendulum_explain_small
import unitto.core.common.generated.resources.pendulum_explain_small_text
import unitto.core.common.generated.resources.pendulum_frequency
import unitto.core.common.generated.resources.pendulum_hint
import unitto.core.common.generated.resources.pendulum_kinetic
import unitto.core.common.generated.resources.pendulum_length
import unitto.core.common.generated.resources.pendulum_period
import unitto.core.common.generated.resources.pendulum_potential
import unitto.core.common.generated.resources.pendulum_start_angle
import unitto.core.common.generated.resources.pendulum_title
import unitto.core.common.generated.resources.pendulum_top_speed
import unitto.core.common.generated.resources.projectile_explain_title
import unitto.core.common.generated.resources.projectile_initial

private val LENGTH = Condition("L", "m", Res.string.pendulum_length, 0.1..10.0, 0.05)
private val ANGLE = Condition("θ", "°", Res.string.pendulum_start_angle, -MAX_ANGLE..MAX_ANGLE, 1.0)
private val DAMPING = Condition("c", "/s", Res.string.pendulum_damping, 0.0..2.0, 0.01)

/** A swing that never stops being worked out: drag the weight, watch the energy change form. */
@Composable
internal fun PendulumRoute(openDrawer: () -> Unit) {
  val fractional = rememberFractional()
  val format = numberFormat(fractional)
  var length by rememberSaveable { mutableStateOf("1") }
  var angle by rememberSaveable { mutableStateOf("40") }
  var gravity by rememberSaveable { mutableStateOf("9.81") }
  var damping by rememberSaveable { mutableStateOf("0") }
  val pendulum = Pendulum(length.number(), angle.number(), gravity.number(), damping.number())

  PhysicsScreen(
    title = Res.string.pendulum_title,
    openDrawer = openDrawer,
    live = {
      if (pendulum.isValid) PendulumCard(pendulum, format) { angle = it.rounded(0) } else InvalidValues()
    },
    settings = {
      Card(stringResource(Res.string.projectile_initial)) {
        ConditionRow(LENGTH, length, fractional) { length = it }
        ConditionRow(ANGLE, angle, fractional) { angle = it }
        ConditionRow(DAMPING, damping, fractional) { damping = it }
        ConditionRow(GRAVITY, gravity, fractional) { gravity = it }
        PlanetChips(gravity) { gravity = it.gravity.replace(".", fractional) }
      }
    },
    explanation = { if (pendulum.isValid) PendulumExplanation(pendulum, format) },
  )
}

@Composable
private fun PendulumCard(pendulum: Pendulum, format: (Double) -> String, onAngleChange: (Double) -> Unit) {
  val motion = remember(pendulum) { PendulumMotion(pendulum) }
  OnEveryFrame(motion, motion::advance)
  val onDrag by rememberUpdatedState(onAngleChange)
  val rod = MaterialTheme.colorScheme.onSurface
  val weight = MaterialTheme.colorScheme.primary
  val path = MaterialTheme.colorScheme.outline
  val kinetic = MaterialTheme.colorScheme.tertiary
  LiveCard {
    Canvas(
      modifier =
        Modifier.fillMaxWidth().height(SCENE_HEIGHT).pointerInput(Unit) {
          detectDragGestures { change, _ ->
            val x = change.position.x - size.width / 2
            val y = change.position.y - PIVOT_TOP.toPx()
            onDrag((atan2(x, y) * HALF_TURN / PI).coerceIn(-MAX_ANGLE, MAX_ANGLE))
          }
        }
    ) {
      drawSwing(pendulum, motion.angle, rod, weight, path)
    }
    Text(
      stringResource(Res.string.pendulum_hint),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) { drawHistory(motion.angle, motion.history, pendulum.radians, weight, path) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      EnergyBar(stringResource(Res.string.pendulum_potential), weight) { pendulum.potentialShare(motion.angle) }
      EnergyBar(stringResource(Res.string.pendulum_kinetic), kinetic) { pendulum.kineticShare(motion.speed) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.pendulum_period, "${format(pendulum.period)} s")
      Answer(Modifier.weight(1f), Res.string.pendulum_frequency, "${format(1 / pendulum.period)} Hz")
      Answer(Modifier.weight(1f), Res.string.pendulum_top_speed, "${format(pendulum.topSpeed)} m/s")
    }
  }
}

private fun DrawScope.drawSwing(pendulum: Pendulum, angle: Double, rod: Color, weight: Color, path: Color) {
  val pivot = Offset(size.width / 2, PIVOT_TOP.toPx())
  val longest = size.height - pivot.y - WEIGHT_RADIUS.toPx() - SCENE_MARGIN.toPx()
  val length = longest * (pendulum.length / LONG_ROD).coerceIn(SHORTEST_ROD, 1.0).toFloat()
  val reach = Offset(length, length)
  val degrees = pendulum.angle.toFloat()
  drawArc(
    color = path,
    startAngle = DOWN - degrees,
    sweepAngle = 2 * degrees,
    useCenter = false,
    topLeft = pivot - reach,
    size = Size(length * 2, length * 2),
    style = Stroke(width = PATH_WIDTH.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(DASH, DASH))),
  )
  val end = pivot + Offset(sin(angle).toFloat() * length, cos(angle).toFloat() * length)
  drawLine(rod, pivot, end, strokeWidth = ROD_WIDTH.toPx())
  drawCircle(rod, radius = PIVOT_RADIUS.toPx(), center = pivot)
  drawCircle(weight, radius = WEIGHT_RADIUS.toPx(), center = end)
}

/** The angle over the last seconds, the newest on the right. */
private fun DrawScope.drawHistory(now: Double, history: List<Float>, start: Double, line: Color, axis: Color) {
  val middle = size.height / 2
  drawLine(axis, Offset(0f, middle), Offset(size.width, middle))
  if (history.size < 2 || start == 0.0) return
  val height = (middle - CHART_MARGIN.toPx()) / kotlin.math.abs(start).toFloat()
  val path = Path()
  history.forEachIndexed { index, value ->
    val x = size.width * (index + 1 + HISTORY_LENGTH - history.size) / HISTORY_LENGTH
    val y = middle - value * height
    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
  }
  drawPath(path, line, style = Stroke(width = ROD_WIDTH.toPx()))
  drawCircle(line, radius = PIVOT_RADIUS.toPx(), center = Offset(size.width, middle - now.toFloat() * height))
}

@Composable
private fun EnergyBar(label: String, color: Color, share: () -> Double) {
  val track = MaterialTheme.colorScheme.surfaceContainerHighest
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(label, modifier = Modifier.width(LABEL_WIDTH), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    Canvas(Modifier.weight(1f).height(BAR_HEIGHT)) {
      val corner = CornerRadius(size.height / 2)
      drawRoundRect(track, cornerRadius = corner)
      val width = size.width * share().toFloat().coerceIn(0f, 1f)
      drawRoundRect(color, size = Size(width, size.height), cornerRadius = corner)
    }
  }
}

@Composable
private fun PendulumExplanation(pendulum: Pendulum, format: (Double) -> String) {
  val l = format(pendulum.length)
  val g = format(pendulum.gravity)
  val t0 = format(pendulum.smallAnglePeriod)
  val t = format(pendulum.period)
  val h = format(pendulum.drop)
  Card(stringResource(Res.string.projectile_explain_title)) {
    Step(
      Res.string.pendulum_explain_small,
      Res.string.pendulum_explain_small_text,
      "T₀ = 2π · √(L / g) = 2π · √($l / $g) = $t0 s",
    )
    Step(
      Res.string.pendulum_explain_real,
      Res.string.pendulum_explain_real_text,
      "θ″ = −(g / L) · sin θ − c · θ′",
      "T = T₀ / agm(1, cos(θ / 2)) = $t s",
    )
    Step(
      Res.string.pendulum_explain_energy,
      Res.string.pendulum_explain_energy_text,
      "h = L · (1 − cos θ) = $h m",
      "v = √(2 · g · h) = √(2 · $g · $h) = ${format(pendulum.topSpeed)} m/s",
    )
  }
}

private const val HALF_TURN = 180.0
private const val DOWN = 90f
private const val DASH = 12f
private const val LONG_ROD = 1.5
private const val SHORTEST_ROD = 0.3
private const val HISTORY_LENGTH = 240
private val SCENE_HEIGHT = 240.dp
private val CHART_HEIGHT = 56.dp
private val CHART_MARGIN = 4.dp
private val PIVOT_TOP = 16.dp
private val PIVOT_RADIUS = 4.dp
private val SCENE_MARGIN = 4.dp
private val WEIGHT_RADIUS = 16.dp
private val ROD_WIDTH = 2.dp
private val PATH_WIDTH = 1.5.dp
private val LABEL_WIDTH = 96.dp
private val BAR_HEIGHT = 10.dp
