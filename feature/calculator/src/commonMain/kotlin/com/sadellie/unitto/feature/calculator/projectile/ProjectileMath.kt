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
internal class Flight(val launch: Launch, val vx: Double, val vy: Double) {
  private val gravity = launch.gravity

  /** Time until it is back on the ground: the positive root of y(t) = 0. */
  val time = (vy + sqrt(vy * vy + 2 * gravity * launch.height)) / gravity

  val range = vx * time

  /** The top of the flight, at the start when it is thrown downwards. */
  val apexTime = max(0.0, vy / gravity)

  val apexHeight = y(apexTime)

  val impactVy = verticalSpeed(time)

  val impactSpeed = hypot(vx, impactVy)

  /** Degrees below the horizon. */
  val impactAngle = atan2(-impactVy, vx) * HALF_TURN / PI

  fun x(t: Double) = vx * t

  fun y(t: Double) = launch.height + vy * t - gravity * t * t / 2

  fun verticalSpeed(t: Double) = vy - gravity * t

  fun speed(t: Double) = hypot(vx, verticalSpeed(t))
}

private const val HALF_TURN = 180.0
private const val RIGHT_ANGLE = 90.0
