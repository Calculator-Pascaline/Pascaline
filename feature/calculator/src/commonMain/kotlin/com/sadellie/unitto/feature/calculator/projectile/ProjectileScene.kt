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

package com.sadellie.unitto.feature.calculator.projectile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Meters to pixels, with the same scale both ways so that the parabola has its true shape. */
private class SceneScale(flights: List<Trajectory>, size: Size, pad: Float, bottom: Float) {
  private val worldWidth = max(flights.maxOf { it.range }, MIN_WORLD)
  private val worldHeight = max(flights.maxOf { it.apexHeight }, MIN_WORLD)
  private val scale = min((size.width - 2 * pad) / worldWidth, (size.height - 2 * pad - bottom) / worldHeight)
  private val left = (size.width - worldWidth * scale).toFloat() / 2
  val ground = size.height - bottom

  fun at(x: Double, y: Double) = Offset((left + x * scale).toFloat(), (ground - y * scale).toFloat())
}

/** Colors and sizes that the drawing needs from the theme. */
private class SceneStyle(
  val curve: Color,
  val ball: Color,
  val split: Color,
  val ground: Color,
  val text: TextStyle,
  val measurer: TextMeasurer,
)

/**
 * The flight at [progress], from 0 to 1 and a bit more for the ring where it lands. The arrow at the
 * start is the launch itself: dragging it changes the speed and the angle.
 *
 * @param format Number with the separator of the settings.
 */
@Composable
internal fun ProjectileScene(
  modifier: Modifier,
  flight: Trajectory,
  withoutAir: Trajectory?,
  progress: Float,
  format: (Double) -> String,
  onAimChange: (speed: Double, angle: Double) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val measurer = rememberTextMeasurer()
  val text = MaterialTheme.typography.labelMedium.copy(color = colors.onSurfaceVariant)
  val style = remember(colors, measurer, text) {
    SceneStyle(colors.primary, Color(BALL_COLOR), colors.tertiary, colors.outline, text, measurer)
  }
  val currentFlight by rememberUpdatedState(flight)
  val currentFlights by rememberUpdatedState(listOfNotNull(flight, withoutAir))
  val currentOnAim by rememberUpdatedState(onAimChange)
  Canvas(
    modifier =
      modifier.pointerInput(Unit) {
        var isAiming = false
        // The arrow, kept here: the whole scene changes its scale during the drag
        var aim = Offset.Zero
        detectDragGestures(
          onDragStart = { start ->
            val launch = currentFlight.launch
            val origin = SceneScale(currentFlights, size.toSizeF(), PADDING.toPx(), GROUND_SPACE.toPx()).at(0.0, launch.height)
            aim = aimOf(launch, ARROW_PER_SPEED.toPx())
            isAiming = (start - origin).getDistance() < AIM_TOUCH.toPx() || (start - origin - aim).getDistance() < AIM_TOUCH.toPx()
          },
          onDrag = { change, amount ->
            if (isAiming) {
              change.consume()
              aim += amount
              val angle = atan2(-aim.y, aim.x) * HALF_TURN / PI
              currentOnAim(aim.getDistance() / ARROW_PER_SPEED.toPx().toDouble(), angle)
            }
          },
        )
      }
  ) {
    val scale = SceneScale(listOfNotNull(flight, withoutAir), size, PADDING.toPx(), GROUND_SPACE.toPx())
    val time = flight.time * progress.coerceAtMost(1f)
    drawGround(flight, scale, style, format)
    drawApex(flight, scale, style, format)
    if (withoutAir != null) drawGhost(withoutAir, scale, style)
    drawPaths(flight, time, scale, style)
    drawAim(flight, scale, style)
    drawBall(flight, time, scale, style)
    if (progress > 1f) drawLanding(flight, (progress - 1f) / LANDING_PART, scale, style)
  }
}

private fun androidx.compose.ui.unit.IntSize.toSizeF() = Size(width.toFloat(), height.toFloat())

/** The launch as an arrow on the screen. */
private fun aimOf(launch: Launch, perSpeed: Float): Offset {
  val radians = launch.angle * PI / HALF_TURN
  val length = launch.speed * perSpeed
  return Offset((cos(radians) * length).toFloat(), (-sin(radians) * length).toFloat())
}

private fun DrawScope.drawLabel(style: SceneStyle, text: String, center: Offset) {
  val layout = style.measurer.measure(text, style.text)
  val x = (center.x - layout.size.width / 2).coerceIn(0f, max(0f, size.width - layout.size.width))
  drawText(layout, topLeft = Offset(x, center.y - layout.size.height / 2))
}

/** The ground, with a mark at every landing distance and how far it is. */
private fun DrawScope.drawGround(flight: Trajectory, scale: SceneScale, style: SceneStyle, format: (Double) -> String) {
  drawLine(style.ground, Offset(0f, scale.ground), Offset(size.width, scale.ground), LINE_WIDTH.toPx())
  val start = scale.at(0.0, 0.0)
  val end = scale.at(flight.range, 0.0)
  val y = scale.ground + GROUND_LABEL.toPx()
  drawLine(style.ground, start, Offset(start.x, y), 1.dp.toPx())
  drawLine(style.ground, end, Offset(end.x, y), 1.dp.toPx())
  drawLabel(style, "${format(flight.range)} m", Offset((start.x + end.x) / 2, y))
}

/** How high the top is, on a dotted line down to the ground. */
private fun DrawScope.drawApex(flight: Trajectory, scale: SceneScale, style: SceneStyle, format: (Double) -> String) {
  val apex = scale.at(flight.x(flight.apexTime), flight.apexHeight)
  val foot = Offset(apex.x, scale.ground)
  val dots = PathEffect.dashPathEffect(floatArrayOf(DOT.toPx(), DOT.toPx() * 2))
  drawLine(style.ground, apex, foot, 1.dp.toPx(), pathEffect = dots)
  drawLabel(style, "${format(flight.apexHeight)} m", apex - Offset(0f, APEX_LABEL.toPx()))
}

/** The same throw without air, faint behind the real one. */
private fun DrawScope.drawGhost(flight: Trajectory, scale: SceneScale, style: SceneStyle) {
  val path = Path().apply {
    moveTo(scale.at(0.0, flight.y(0.0)))
    for (i in 1..PATH_STEPS) {
      val t = flight.time * i / PATH_STEPS
      lineTo(scale.at(flight.x(t), flight.y(t)))
    }
  }
  val dots = DOT.toPx()
  drawPath(path, style.ground, style = Stroke(LINE_WIDTH.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dots, dots * 2))))
}

/** The way ahead is dashed, what was flown is solid. */
private fun DrawScope.drawPaths(flight: Trajectory, time: Double, scale: SceneScale, style: SceneStyle) {
  fun path(from: Double, to: Double) = Path().apply {
    moveTo(scale.at(flight.x(from), flight.y(from)))
    for (i in 1..PATH_STEPS) {
      val t = from + (to - from) * i / PATH_STEPS
      lineTo(scale.at(flight.x(t), flight.y(t)))
    }
  }
  val dash = DASH.toPx()
  drawPath(
    path(0.0, flight.time),
    style.curve.copy(alpha = AHEAD_ALPHA),
    style = Stroke(LINE_WIDTH.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
  )
  if (time > 0) drawPath(path(0.0, time), style.curve, style = Stroke(TRAIL_WIDTH.toPx(), cap = StrokeCap.Round))
}

private fun Path.moveTo(point: Offset) = moveTo(point.x, point.y)

private fun Path.lineTo(point: Offset) = lineTo(point.x, point.y)

/** The arrow to drag, its length follows the speed. */
private fun DrawScope.drawAim(flight: Trajectory, scale: SceneScale, style: SceneStyle) {
  val start = scale.at(0.0, flight.launch.height)
  val tip = start + aimOf(flight.launch, ARROW_PER_SPEED.toPx())
  drawArrow(style.ball, start, tip)
  drawCircle(style.ball, AIM_RADIUS.toPx(), tip, alpha = AIM_ALPHA)
}

private fun DrawScope.drawArrow(color: Color, start: Offset, end: Offset) {
  val along = end - start
  val length = along.getDistance()
  if (length < 1f) return
  val head = HEAD.toPx()
  val unit = along / length
  val side = Offset(-unit.y, unit.x) * (head / 2)
  drawLine(color, start, end, ARROW_WIDTH.toPx(), cap = StrokeCap.Round)
  drawLine(color, end, end - unit * head + side, ARROW_WIDTH.toPx(), cap = StrokeCap.Round)
  drawLine(color, end, end - unit * head - side, ARROW_WIDTH.toPx(), cap = StrokeCap.Round)
}

/** The ball with its speed: along the ground it never changes, up and down gravity changes it. */
private fun DrawScope.drawBall(flight: Trajectory, time: Double, scale: SceneScale, style: SceneStyle) {
  val center = scale.at(flight.x(time), flight.y(time))
  val perSpeed = ARROW_PER_SPEED.toPx()
  val across = Offset(flight.horizontalSpeed(time).toFloat() * perSpeed, 0f)
  val upwards = Offset(0f, -flight.verticalSpeed(time).toFloat() * perSpeed)
  drawArrow(style.split, center, center + across)
  drawArrow(style.split, center, center + upwards)
  drawCircle(style.ball, BALL_RADIUS.toPx(), center)
}

/** A ring that grows and fades where the ball lands. */
private fun DrawScope.drawLanding(flight: Trajectory, part: Float, scale: SceneScale, style: SceneStyle) {
  val radius = BALL_RADIUS.toPx() + LANDING_RADIUS.toPx() * part
  drawCircle(
    style.ball.copy(alpha = (1 - part).coerceIn(0f, 1f)),
    radius,
    scale.at(flight.range, 0.0),
    style = Stroke(LINE_WIDTH.toPx()),
  )
}

/** The part of the animation after the landing, for the ring. */
internal const val LANDING_PART = 0.15f

private const val BALL_COLOR = 0xFFF08A24
private const val HALF_TURN = 180.0
private const val MIN_WORLD = 1.0
private const val PATH_STEPS = 60
private const val AHEAD_ALPHA = 0.5f
private const val AIM_ALPHA = 0.25f
private val PADDING = 28.dp
private val GROUND_SPACE = 24.dp
private val GROUND_LABEL = 12.dp
private val APEX_LABEL = 14.dp
private val LINE_WIDTH = 1.5.dp
private val TRAIL_WIDTH = 3.dp
private val ARROW_WIDTH = 2.dp
private val ARROW_PER_SPEED = 2.dp
private val HEAD = 8.dp
private val BALL_RADIUS = 7.dp
private val AIM_RADIUS = 12.dp
private val AIM_TOUCH = 48.dp
private val LANDING_RADIUS = 28.dp
private val DOT = 2.dp
private val DASH = 6.dp
