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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What a throw starts with, in meters and seconds.
 *
 * @property angle Degrees above the horizon, negative when thrown downwards.
 */
internal data class Launch(val speed: Double, val angle: Double, val height: Double, val gravity: Double) {
  val isValid: Boolean
    get() = speed >= 0 && abs(angle) <= RIGHT_ANGLE && height >= 0 && gravity > 0
}

/**
 * A throw from start to landing on flat ground.
 *
 * @property apexTime When it is highest, at the start when it is thrown downwards.
 * @property impactAngle Degrees below the horizon.
 */
internal interface Trajectory {
  val launch: Launch
  val time: Double
  val range: Double
  val apexTime: Double
  val apexHeight: Double
  val impactSpeed: Double
  val impactAngle: Double

  fun x(t: Double): Double

  fun y(t: Double): Double

  fun horizontalSpeed(t: Double): Double

  fun verticalSpeed(t: Double): Double

  fun speed(t: Double) = hypot(horizontalSpeed(t), verticalSpeed(t))
}

/**
 * The air a ball flies through, in kilograms and meters.
 *
 * @property density Of the air, 1.225 kg/m³ at sea level.
 */
internal data class Air(val mass: Double, val diameter: Double, val density: Double) {
  val isValid: Boolean
    get() = mass > 0 && diameter > 0 && density >= 0

  /** A = π d² / 4, the circle the ball shows to the air. */
  val area: Double
    get() = PI * diameter * diameter / 4

  /** Deceleration divided by speed squared: ½ ρ Cd A / m. */
  val drag: Double
    get() = density * SPHERE_DRAG * area / (2 * mass)

  /** Speed where air stops the fall from getting faster: √(g / k). */
  fun terminalSpeed(gravity: Double) = sqrt(gravity / drag)
}

/** With [air] a simulated flight, a parabola without it. Null when something isn't valid. */
internal fun Launch.trajectory(air: Air?): Trajectory? =
  when {
    air == null || air.density == 0.0 -> flight()
    !isValid || !air.isValid -> null
    else -> simulate(air, TIME_STEP)
  }

/** The angle that goes furthest through [air], to the degree, and how far that is. */
internal fun Launch.bestAngle(air: Air): Pair<Double, Double> {
  fun range(angle: Double) = copy(angle = angle).simulate(air, ROUGH_TIME_STEP).range
  val rough = (0..ANGLE_STEPS).map { it * ANGLE_STEP }.maxBy(::range)
  val best =
    ((rough - ANGLE_STEP).coerceAtLeast(0.0)..(rough + ANGLE_STEP).coerceAtMost(RIGHT_ANGLE)).let { span ->
      generateSequence(span.start) { it + 1 }.takeWhile { it <= span.endInclusive }.maxBy(::range)
    }
  return best to copy(angle = best).simulate(air, TIME_STEP).range
}

/** Runge-Kutta: gravity down and air against the motion, [step] seconds at a time. */
private fun Launch.simulate(air: Air, step: Double): Trajectory {
  val radians = angle * PI / HALF_TURN
  val k = air.drag
  fun accelerationX(vx: Double, vy: Double) = -k * hypot(vx, vy) * vx
  fun accelerationY(vx: Double, vy: Double) = -gravity - k * hypot(vx, vy) * vy
  val states = mutableListOf(doubleArrayOf(0.0, height, speed * cos(radians), speed * sin(radians)))
  while (states.last()[Y] >= 0 && states.size < MAX_STEPS) {
    val last = states.last()
    val vx = last[VX]
    val vy = last[VY]
    val ax1 = accelerationX(vx, vy)
    val ay1 = accelerationY(vx, vy)
    val vx2 = vx + ax1 * step / 2
    val vy2 = vy + ay1 * step / 2
    val ax2 = accelerationX(vx2, vy2)
    val ay2 = accelerationY(vx2, vy2)
    val vx3 = vx + ax2 * step / 2
    val vy3 = vy + ay2 * step / 2
    val ax3 = accelerationX(vx3, vy3)
    val ay3 = accelerationY(vx3, vy3)
    val vx4 = vx + ax3 * step
    val vy4 = vy + ay3 * step
    val ax4 = accelerationX(vx4, vy4)
    val ay4 = accelerationY(vx4, vy4)
    states +=
      doubleArrayOf(
        last[X] + step * (vx + 2 * vx2 + 2 * vx3 + vx4) / SIX,
        last[Y] + step * (vy + 2 * vy2 + 2 * vy3 + vy4) / SIX,
        vx + step * (ax1 + 2 * ax2 + 2 * ax3 + ax4) / SIX,
        vy + step * (ay1 + 2 * ay2 + 2 * ay3 + ay4) / SIX,
      )
  }
  return SimulatedFlight(this, states, step)
}

/** The steps of a simulation, values between two steps are read on the line between them. */
private class SimulatedFlight(override val launch: Launch, private val states: List<DoubleArray>, private val step: Double) :
  Trajectory {
  private val lastPart = (states.size - 1).let { last ->
    if (last == 0 || states[last][Y] > 0) 1.0
    else states[last - 1][Y] / (states[last - 1][Y] - states[last][Y])
  }
  override val time = (states.size - 2).coerceAtLeast(0) * step + lastPart * step
  override val range = at(time, X)
  override val apexTime = states.indices.maxBy { states[it][Y] } * step
  override val apexHeight = at(apexTime, Y)
  override val impactSpeed = speed(time)
  override val impactAngle = atan2(-verticalSpeed(time), horizontalSpeed(time)) * HALF_TURN / PI

  private fun at(t: Double, index: Int): Double {
    val position = (t / step).coerceAtLeast(0.0)
    val before = position.toInt().coerceAtMost(states.size - 2).coerceAtLeast(0)
    val after = (before + 1).coerceAtMost(states.size - 1)
    return states[before][index] + (states[after][index] - states[before][index]) * (position - before)
  }

  override fun x(t: Double) = at(t, X)

  override fun y(t: Double) = at(t, Y).coerceAtLeast(0.0)

  override fun horizontalSpeed(t: Double) = at(t, VX)

  override fun verticalSpeed(t: Double) = at(t, VY)
}

/** Motion of a [Launch] without air, on flat ground: a parabola. Null when the launch isn't valid. */
internal fun Launch.flight(): Flight? {
  if (!isValid) return null
  val radians = angle * PI / HALF_TURN
  return Flight(this, speed * cos(radians), speed * sin(radians))
}

/**
 * @property vx Speed along the ground, it never changes.
 * @property vy Speed upwards at the start, gravity then takes it down.
 */
internal class Flight(override val launch: Launch, val vx: Double, val vy: Double) : Trajectory {
  private val gravity = launch.gravity

  /** Time until it is back on the ground: the positive root of y(t) = 0. */
  override val time = (vy + sqrt(vy * vy + 2 * gravity * launch.height)) / gravity

  override val range = vx * time

  /** The top of the flight, at the start when it is thrown downwards. */
  override val apexTime = max(0.0, vy / gravity)

  override val apexHeight = y(apexTime)

  override val impactSpeed = speed(time)

  override val impactAngle = atan2(-verticalSpeed(time), vx) * HALF_TURN / PI

  override fun x(t: Double) = vx * t

  override fun y(t: Double) = launch.height + vy * t - gravity * t * t / 2

  override fun horizontalSpeed(t: Double) = vx

  override fun verticalSpeed(t: Double) = vy - gravity * t
}

/** Drag coefficient of a smooth sphere in the air, around 0.47 at the speeds of a throw. */
internal const val SPHERE_DRAG = 0.47

/** Seconds between two steps of the simulation. */
internal const val TIME_STEP = 0.005
private const val ROUGH_TIME_STEP = 0.02
private const val MAX_STEPS = 40_000
private const val ANGLE_STEP = 5.0
private const val ANGLE_STEPS = 18
private const val SIX = 6
private const val X = 0
private const val Y = 1
private const val VX = 2
private const val VY = 3
private const val HALF_TURN = 180.0
private const val RIGHT_ANGLE = 90.0
