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

import io.github.calculatorpascaline.feature.calculator.electrical.engineering
import kotlin.math.PI
import kotlin.math.abs

/** How turning speeds are written: [perRpm] of the unit in one rpm. */
internal enum class SpeedUnit(val symbol: String, val perRpm: Double) {
  RPM("rpm", 1.0),
  RAD_PER_SECOND("rad/s", 2 * PI / SECONDS_PER_MINUTE),
  HERTZ("Hz", 1.0 / SECONDS_PER_MINUTE),
}

/** Torque, power, force and straight speeds in metric (N·m, kW, N, m/s) or imperial (lbf·ft, hp, lbf, ft/s) units. */
internal enum class ForceUnits(
  val torque: String,
  val torquePerNewtonMeter: Double,
  val power: String,
  val powerPerWatt: Double,
  val force: String,
  val forcePerNewton: Double,
) {
  METRIC("N·m", 1.0, "kW", 1.0 / WATTS_PER_KILOWATT, "N", 1.0),
  IMPERIAL("lbf·ft", LBF_FT_PER_NEWTON_METER, "hp", 1.0 / WATTS_PER_HORSEPOWER, "lbf", LBF_PER_NEWTON);

  /** Unit of straight speeds. */
  val speed: String
    get() = if (this == METRIC) "m/s" else "ft/s"

  /** Units of straight speed in one m/s. */
  val speedPerMeterPerSecond: Double
    get() = if (this == METRIC) 1.0 else FEET_PER_METER
}

/** How numbers are written: speed with the way it turns, ratio as 6 : 1, sizes in mm or inches, in the chosen units. */
internal class GearTexts(
  val format: (Double) -> String,
  private val size: String,
  val speedUnit: SpeedUnit,
  val units: ForceUnits,
) {
  /** A turning speed in rpm, or the straight speed of a rack in m/s when [rack]. */
  fun speed(rpm: Double, rightAngle: Boolean = false, rack: Boolean = false): String {
    if (!rpm.isFinite()) return DASH
    if (rack) return "${format(abs(rpm) * units.speedPerMeterPerSecond)} ${units.speed}${slide(rpm)}"
    val arrow =
      when {
        rightAngle -> " ⟂"
        rpm > 0 -> " ↻"
        rpm < 0 -> " ↺"
        else -> ""
      }
    return "${format(abs(rpm) * speedUnit.perRpm)} ${speedUnit.symbol}$arrow"
  }

  /** A torque in N·m, or the force on a rack in N when [rack]. */
  fun torque(value: Double, rack: Boolean = false): String =
    when {
      !value.isFinite() -> DASH
      rack -> "${format(value * units.forcePerNewton)} ${units.force}"
      else -> "${format(value * units.torquePerNewtonMeter)} ${units.torque}"
    }

  fun ratio(value: Double): String =
    when {
      !value.isFinite() || value == 0.0 -> DASH
      value < 0 -> "−" + ratio(-value)
      value >= 1 -> "${format(value)} : 1"
      else -> "1 : ${format(1 / value)}"
    }

  fun size(value: Double) = if (value.isFinite()) "${format(value)} $size" else DASH

  fun power(watts: Double) =
    if (units == ForceUnits.METRIC) engineering(watts, "W", format)
    else if (watts.isFinite()) "${format(watts * units.powerPerWatt)} hp" else DASH

  fun percent(share: Double) = if (share.isFinite()) "${format(share * PERCENT)} %" else DASH

  fun plain(value: Double) = if (value.isFinite()) format(value) else DASH

  fun meterSpeed(value: Double) =
    if (value.isFinite()) "${format(value * units.speedPerMeterPerSecond)} ${units.speed}" else DASH

  fun stress(megapascals: Double) = if (megapascals.isFinite()) "${format(megapascals)} MPa" else DASH

  /** The way a rack goes: it is under the gear, so left when the gear turns clockwise. */
  private fun slide(speed: Double) =
    when {
      speed > 0 -> " ←"
      speed < 0 -> " →"
      else -> ""
    }
}

internal const val DASH = "—"
internal const val PERCENT = 100.0
internal const val WATTS_PER_KILOWATT = 1000.0
private const val WATTS_PER_HORSEPOWER = 745.6998715822702
private const val LBF_FT_PER_NEWTON_METER = 0.7375621492772654
private const val LBF_PER_NEWTON = 0.2248089430997105
private const val FEET_PER_METER = 3.280839895013123
