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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A weight on a rod, in meters and seconds.
 *
 * @property angle Degrees from hanging straight down, where it is let go.
 * @property damping Share of its speed the air takes away each second.
 */
internal data class Pendulum(val length: Double, val angle: Double, val gravity: Double, val damping: Double) {
  val isValid: Boolean
    get() = length > 0 && abs(angle) <= MAX_ANGLE && gravity > 0 && damping >= 0

  val radians: Double
    get() = angle * PI / HALF_TURN

  /** T₀ = 2π √(L / g), right for small angles. */
  val smallAnglePeriod: Double
    get() = 2 * PI * sqrt(length / gravity)

  /** The period at any angle: T₀ divided by the arithmetic-geometric mean of 1 and cos(θ / 2). */
  val period: Double
    get() = smallAnglePeriod / arithmeticGeometricMean(1.0, cos(radians / 2))

  /** How high it starts above the lowest point. */
  val drop: Double
    get() = length * (1 - cos(radians))

  /** All the height turned into speed: v = √(2 g h). */
  val topSpeed: Double
    get() = sqrt(2 * gravity * drop)

  /** Height energy at [position], 1 for the energy it starts with. */
  fun potentialShare(position: Double) = share(length * (1 - cos(position)))

  /** Speed energy at [speed] in radians per second, 1 for the energy it starts with. */
  fun kineticShare(speed: Double) = share(length * length * speed * speed / (2 * gravity))

  private fun share(height: Double) = if (drop > 0) height / drop else 0.0
}

private fun arithmeticGeometricMean(first: Double, second: Double): Double {
  var a = first
  var b = second
  repeat(MEAN_STEPS) {
    val next = (a + b) / 2
    b = sqrt(a * b)
    a = next
  }
  return a
}

/** The swing as it goes: θ″ = −(g / L) sin θ − c θ′, a few small steps each frame. */
internal class PendulumMotion(private val pendulum: Pendulum) {
  var angle by mutableDoubleStateOf(pendulum.radians)
    private set

  /** Radians per second. */
  var speed by mutableDoubleStateOf(0.0)
    private set

  /** The last angles, the oldest first. */
  val history = ArrayDeque<Float>()

  fun advance(seconds: Double) {
    var left = seconds.coerceAtMost(MAX_FRAME)
    var position = angle
    var velocity = speed
    while (left > 0) {
      val step = min(left, TIME_STEP)
      val a1 = acceleration(position, velocity)
      val v2 = velocity + a1 * step / 2
      val a2 = acceleration(position + velocity * step / 2, v2)
      val v3 = velocity + a2 * step / 2
      val a3 = acceleration(position + v2 * step / 2, v3)
      val v4 = velocity + a3 * step
      val a4 = acceleration(position + v3 * step, v4)
      position += step * (velocity + 2 * v2 + 2 * v3 + v4) / SIX
      velocity += step * (a1 + 2 * a2 + 2 * a3 + a4) / SIX
      left -= step
    }
    angle = position
    speed = velocity
    history.addLast(position.toFloat())
    if (history.size > HISTORY_SIZE) history.removeFirst()
  }

  private fun acceleration(position: Double, velocity: Double) =
    -pendulum.gravity / pendulum.length * sin(position) - pendulum.damping * velocity
}

/** Past this it would stand on its head. */
internal const val MAX_ANGLE = 170.0
private const val HALF_TURN = 180.0
private const val MEAN_STEPS = 12
private const val TIME_STEP = 0.002
private const val MAX_FRAME = 0.05
private const val HISTORY_SIZE = 240
private const val SIX = 6
