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

package com.sadellie.unitto.feature.calculator.electrical

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

private const val KILO = 1000.0
private const val SMALLEST_PREFIX = -4
private const val LARGEST_PREFIX = 3
private const val THOUSANDS = 3
private val PREFIXES = listOf("p", "n", "µ", "m", "", "k", "M", "G")

/** 4700 Ω ==> 4.7 kΩ, 0.000022 F ==> 22 µF. */
internal fun engineering(value: Double, unit: String, format: (Double) -> String): String {
  if (!value.isFinite()) return "—"
  if (value == 0.0) return "${format(0.0)} $unit"
  val thousands = floor(log10(abs(value)) / THOUSANDS).toInt().coerceIn(SMALLEST_PREFIX, LARGEST_PREFIX)
  return "${format(value / KILO.pow(thousands))} ${PREFIXES[thousands - SMALLEST_PREFIX]}$unit"
}

/** What a filter keeps. */
internal enum class FilterKind {
  LOW_PASS,
  HIGH_PASS,
  BAND_PASS,
  BAND_STOP;

  /** The ways it can be built: a band needs a coil and a capacitor. */
  val builds: List<FilterBuild>
    get() = if (this == LOW_PASS || this == HIGH_PASS) FilterBuild.entries else listOf(FilterBuild.RLC)

  val isBand: Boolean
    get() = this == BAND_PASS || this == BAND_STOP
}

/**
 * The parts of a filter. The order is how fast it cuts: 20 dB less for every ten times the
 * frequency for the first order, 40 dB for the second.
 *
 * @property label The same in every language.
 */
internal enum class FilterBuild(val label: String, val order: Int, val usesCapacitor: Boolean, val usesCoil: Boolean) {
  RC("RC", 1, true, false),
  RL("RL", 1, false, true),
  TWO_RC("RC + RC", 2, true, false),
  RLC("RLC", 2, true, true),
}

/**
 * A passive filter on a voltage, in ohms, farads and henries. Two RC stages use the same R and C,
 * the second one loading the first. RLC parts are in series, with the output across one or two.
 */
internal class Filter(
  val kind: FilterKind,
  val build: FilterBuild,
  val resistance: Double,
  val capacitance: Double,
  val inductance: Double,
) {
  /** Where a first order filter is at −3 dB, or the natural frequency f₀ of a second order one. */
  val cornerFrequency =
    when (build) {
      FilterBuild.RC,
      FilterBuild.TWO_RC -> 1 / (2 * PI * resistance * capacitance)
      FilterBuild.RL -> resistance / (2 * PI * inductance)
      FilterBuild.RLC -> 1 / (2 * PI * sqrt(inductance * capacitance))
    }

  /** How sharp a second order filter is, null for the first order. */
  val quality: Double? =
    when (build) {
      FilterBuild.RLC -> sqrt(inductance / capacitance) / resistance
      FilterBuild.TWO_RC -> ONE_THIRD
      else -> null
    }

  val bandwidth = cornerFrequency / (quality ?: 1.0)

  val isValid: Boolean
    get() =
      resistance > 0 && (!build.usesCapacitor || capacitance > 0) && (!build.usesCoil || inductance > 0) && build in kind.builds

  /** Output / input at [frequency], with the input side as 1 − x² + j · x / Q (1 + j · x for the first order). */
  private class Transfer(val topReal: Double, val topImaginary: Double, val bottomReal: Double, val bottomImaginary: Double)

  private fun transfer(frequency: Double): Transfer {
    val x = frequency / cornerFrequency
    val q = quality
    val square = if (q == null) 0.0 else x * x
    val damping = if (q == null) x else x / q
    return when (kind) {
      FilterKind.LOW_PASS -> Transfer(1.0, 0.0, 1 - square, damping)
      FilterKind.HIGH_PASS -> if (q == null) Transfer(0.0, x, 1.0, x) else Transfer(-square, 0.0, 1 - square, damping)
      FilterKind.BAND_PASS -> Transfer(0.0, damping, 1 - square, damping)
      FilterKind.BAND_STOP -> Transfer(1 - square, 0.0, 1 - square, damping)
    }
  }

  fun gain(frequency: Double): Double {
    val h = transfer(frequency)
    return hypot(h.topReal, h.topImaginary) / hypot(h.bottomReal, h.bottomImaginary)
  }

  fun decibels(frequency: Double): Double = voltageDecibels(gain(frequency))

  /** Degrees the output is ahead of the input, from −180 to 180. */
  fun phase(frequency: Double): Double {
    val h = transfer(frequency)
    val radians = atan2(h.topImaginary, h.topReal) - atan2(h.bottomImaginary, h.bottomReal)
    val degrees = radians * HALF_TURN / PI
    return when {
      degrees > HALF_TURN -> degrees - 2 * HALF_TURN
      degrees <= -HALF_TURN -> degrees + 2 * HALF_TURN
      else -> degrees
    }
  }
}

private const val ONE_THIRD = 1.0 / 3

/** A color of the bands on a resistor. */
internal enum class BandColor {
  BLACK,
  BROWN,
  RED,
  ORANGE,
  YELLOW,
  GREEN,
  BLUE,
  VIOLET,
  GREY,
  WHITE,
  GOLD,
  SILVER;

  /** The digit of a band, null for gold and silver. */
  val digit: Int?
    get() = ordinal.takeIf { it < DIGITS }

  /** Power of 10 of the multiplier band. */
  val exponent: Int
    get() =
      when (this) {
        GOLD -> -1
        SILVER -> -2
        else -> ordinal
      }

  /** In percent, null for the colors that don't mean a tolerance. */
  val tolerance: Double?
    get() = TOLERANCES[this]
}

private const val DIGITS = 10
private val TOLERANCES =
  mapOf(
    BandColor.BROWN to 1.0,
    BandColor.RED to 2.0,
    BandColor.GREEN to 0.5,
    BandColor.BLUE to 0.25,
    BandColor.VIOLET to 0.1,
    BandColor.GREY to 0.05,
    BandColor.GOLD to 5.0,
    BandColor.SILVER to 10.0,
  )

/** Digit bands, then the multiplier, then the tolerance. */
internal data class ColorCode(val digits: List<BandColor>, val multiplier: BandColor, val tolerance: BandColor) {
  val bands: List<BandColor>
    get() = digits + multiplier + tolerance

  /** In ohms. */
  val resistance: Double
    get() = digits.fold(0.0) { value, band -> value * DIGITS + (band.digit ?: 0) } * 10.0.pow(multiplier.exponent)

  val share: Double
    get() = (tolerance.tolerance ?: 0.0) / PERCENT

  val lowest: Double
    get() = resistance * (1 - share)

  val highest: Double
    get() = resistance * (1 + share)
}

/** Bands of [ohms] with [digits] digit bands, null when the colors can't say that value. */
internal fun colorCode(ohms: Double, digits: Int, tolerance: BandColor): ColorCode? {
  if (ohms <= 0 || !ohms.isFinite()) return null
  var exponent = floor(log10(ohms)).toInt() - digits + 1
  var significant = round(ohms / 10.0.pow(exponent)).toInt()
  if (significant >= 10.0.pow(digits)) {
    significant /= DIGITS
    exponent++
  }
  val multiplier = BandColor.entries.firstOrNull { it.exponent == exponent } ?: return null
  val bands = significant.toString().map { BandColor.entries[it.digitToInt()] }
  return ColorCode(bands, multiplier, tolerance)
}

private val E12_TENTHS = listOf(10, 12, 15, 18, 22, 27, 33, 39, 47, 56, 68, 82)
private val E24_TENTHS = listOf(10, 11, 12, 13, 15, 16, 18, 20, 22, 24, 27, 30, 33, 36, 39, 43, 47, 51, 56, 62, 68, 75, 82, 91)

/** Values shops stock per decade, in tenths: 47 can be 4.7 Ω, 47 Ω, 470 Ω and so on. */
internal enum class ResistorSeries(val tenths: List<Int>) {
  E12(E12_TENTHS),
  E24(E24_TENTHS);

  private fun around(ohms: Double): List<Double> {
    val decade = floor(log10(ohms)).toInt()
    return (decade - 1..decade + 1).flatMap { power -> tenths.map { it * 10.0.pow(power - 1) } }
  }

  /** The closest value, in ratio: from 5 to 9 is as far as from 5 to 2.8. */
  fun nearest(ohms: Double): Double = around(ohms).minBy { abs(ln(it / ohms)) }
}

/** Share of the way to full, after [time] with this [timeConstant] (τ). */
internal fun chargedShare(time: Double, timeConstant: Double) = 1 - exp(-time / timeConstant)

/** The classic 555 chip running on its own, in ohms and farads. */
internal class Timer555(val first: Double, val second: Double, val capacitance: Double) {
  val highTime = ln(2.0) * (first + second) * capacitance
  val lowTime = ln(2.0) * second * capacitance
  val period = highTime + lowTime
  val frequency = 1 / period
  val duty = highTime / period

  val isValid: Boolean
    get() = first > 0 && second > 0 && capacitance > 0

  fun isHigh(time: Double) = time.mod(period) < highTime

  /** Voltage of the capacitor as a share of the supply: it goes between ⅓ and ⅔. */
  fun capacitorAt(time: Double): Double {
    val inCycle = time.mod(period)
    return if (inCycle < highTime) {
      1 - TWO_THIRDS * exp(-inCycle / ((first + second) * capacitance))
    } else {
      TWO_THIRDS * exp(-(inCycle - highTime) / (second * capacitance))
    }
  }
}

private const val TWO_THIRDS = 2.0 / 3

/**
 * A gate with one or two inputs.
 *
 * @property expression In symbols, the same in all languages.
 */
internal enum class Gate(val expression: String, val isInverted: Boolean) {
  AND("Y = A · B", false),
  OR("Y = A + B", false),
  XOR("Y = A ⊕ B", false),
  NOT("Y = ¬A", true),
  NAND("Y = ¬(A · B)", true),
  NOR("Y = ¬(A + B)", true),
  XNOR("Y = ¬(A ⊕ B)", true);

  val hasTwoInputs: Boolean
    get() = this != NOT

  fun output(a: Boolean, b: Boolean): Boolean =
    when (this) {
      AND -> a && b
      OR -> a || b
      XOR -> a != b
      NOT -> !a
      NAND -> !(a && b)
      NOR -> !(a || b)
      XNOR -> a == b
    }
}

/** Power ratio ==> decibels. */
internal fun powerDecibels(ratio: Double) = DECIBELS_PER_BEL * log10(ratio)

/** Voltage ratio ==> decibels. */
internal fun voltageDecibels(ratio: Double) = 2 * DECIBELS_PER_BEL * log10(ratio)

internal fun powerRatio(decibels: Double) = 10.0.pow(decibels / DECIBELS_PER_BEL)

internal fun voltageRatio(decibels: Double) = 10.0.pow(decibels / (2 * DECIBELS_PER_BEL))

private const val DECIBELS_PER_BEL = 10
private const val HALF_TURN = 180.0
private const val PERCENT = 100
