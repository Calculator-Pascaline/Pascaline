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

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** What is on the string. */
internal enum class WaveMode {
  /** One wave going right. */
  TRAVELLING,

  /** The same wave going both ways, like on a guitar string. */
  STANDING,

  /** Two close frequencies that go in and out of step. */
  BEATS,
}

/** y(x, t) = A sin(kx − ωt), in meters and seconds. */
internal data class Wave(val amplitude: Double, val wavelength: Double, val frequency: Double) {
  val isValid: Boolean
    get() = amplitude >= 0 && wavelength > 0 && frequency > 0

  /** v = λ · f */
  val speed: Double
    get() = wavelength * frequency

  /** T = 1 / f */
  val period: Double
    get() = 1 / frequency

  /** ω = 2π · f */
  val angularFrequency: Double
    get() = 2 * PI * frequency

  /** k = 2π / λ */
  val waveNumber: Double
    get() = 2 * PI / wavelength

  fun right(x: Double, t: Double) = amplitude * sin(waveNumber * x - angularFrequency * t)

  fun left(x: Double, t: Double) = amplitude * sin(waveNumber * x + angularFrequency * t)

  /** The same string carrying another [frequency]: the speed stays, the wavelength doesn't. */
  fun atFrequency(frequency: Double) = copy(frequency = frequency, wavelength = speed / frequency)

  /** Beats per second with [other]. */
  fun beats(other: Wave) = abs(frequency - other.frequency)

  /** The string at [x] and [t], with the waves of [mode] added up. */
  fun height(mode: WaveMode, other: Wave, x: Double, t: Double) =
    when (mode) {
      WaveMode.TRAVELLING -> right(x, t)
      WaveMode.STANDING -> right(x, t) + left(x, t)
      WaveMode.BEATS -> right(x, t) + other.right(x, t)
    }
}
