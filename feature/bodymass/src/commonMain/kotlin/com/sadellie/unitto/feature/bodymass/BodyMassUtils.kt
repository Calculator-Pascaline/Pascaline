/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2025 Elshan Agaev
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

package com.sadellie.unitto.feature.bodymass

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.MAX_SCALE
import com.sadellie.unitto.core.common.isEqualTo
import com.sadellie.unitto.core.common.isLessThan

/**
 * Calculates BMI value for metric system.
 *
 * @param heightCm Height in centimeters.
 * @param weightKg Weight in kilograms.
 * @return BMI Value with [MAX_SCALE].
 */
internal fun calculateMetric(heightCm: KBigDecimal, weightKg: KBigDecimal): KBigDecimal {
  if (heightCm.isLessThan(KBigDecimal.ZERO)) return KBigDecimal.ZERO
  if (weightKg.isLessThan(KBigDecimal.ZERO)) return KBigDecimal.ZERO
  if (heightCm.isEqualTo(KBigDecimal.ZERO)) return KBigDecimal.ZERO
  if (weightKg.isEqualTo(KBigDecimal.ZERO)) return KBigDecimal.ZERO

  val heightMeters = heightCm.divide(cmToMFactor, MAX_SCALE, KRoundingMode.HALF_EVEN)

  return weightKg.divide(heightMeters.pow(2), MAX_SCALE, KRoundingMode.HALF_EVEN)
}

/**
 * Calculates BMI value for imperial system.
 *
 * @param heightFt Height in feet.
 * @param heightIn Height in inches.
 * @param weightLbs Weight in pounds.
 * @return BMI Value with [MAX_SCALE] or [KBigDecimal.ZERO] if input is negative or zero.
 */
internal fun calculateImperial(
  heightFt: KBigDecimal,
  heightIn: KBigDecimal,
  weightLbs: KBigDecimal,
): KBigDecimal {
  if (heightFt.isLessThan(KBigDecimal.ZERO)) return KBigDecimal.ZERO
  if (heightIn.isLessThan(KBigDecimal.ZERO)) return KBigDecimal.ZERO
  if (weightLbs.isLessThan(KBigDecimal.ZERO)) return KBigDecimal.ZERO
  if (heightFt.isEqualTo(KBigDecimal.ZERO) && heightIn.isEqualTo(KBigDecimal.ZERO))
    return KBigDecimal.ZERO
  if (weightLbs.isEqualTo(KBigDecimal.ZERO)) return KBigDecimal.ZERO

  val heightInches = heightFt.multiply(footToInchFactor).plus(heightIn)

  return weightLbs
    .divide(heightInches.pow(2), MAX_SCALE, KRoundingMode.HALF_EVEN)
    .multiply(metricToImperialFactor) // Approximate lbs/inch^2 to kg/m^2
}

/**
 * Calculates weight (kilograms) range for normal BMI values.
 *
 * @param heightCm Height in centimeters.
 * @return [Pair] of [KBigDecimal]. First value is lowest weight. Second value is highest value. If
 *   input is negative or zero, will return range of zeros.
 */
internal fun calculateNormalWeightMetric(heightCm: KBigDecimal): Pair<KBigDecimal, KBigDecimal> {
  if (heightCm.isLessThan(KBigDecimal.ZERO) || heightCm.isEqualTo(KBigDecimal.ZERO))
    return KBigDecimal.ZERO to KBigDecimal.ZERO

  val heightMetres2 = heightCm.divide(cmToMFactor, MAX_SCALE, KRoundingMode.HALF_EVEN).pow(2)

  val lowest = lowestNormalIndex.multiply(heightMetres2)
  val highest = highestNormalIndex.multiply(heightMetres2)

  return lowest to highest
}

/**
 * Calculates weight (pounds) range for normal BMI values.
 *
 * @param heightFt Height in feet.
 * @param heightIn Height in inches.
 * @return [Pair] of [KBigDecimal]. First value is lowest weight. Second value is highest value. If
 *   input is negative or zero, will return range of zeros.
 */
internal fun calculateNormalWeightImperial(
  heightFt: KBigDecimal,
  heightIn: KBigDecimal,
): Pair<KBigDecimal, KBigDecimal> {
  if (heightFt.isLessThan(KBigDecimal.ZERO)) return KBigDecimal.ZERO to KBigDecimal.ZERO
  if (heightIn.isLessThan(KBigDecimal.ZERO)) return KBigDecimal.ZERO to KBigDecimal.ZERO
  if (heightFt.isEqualTo(KBigDecimal.ZERO) && heightIn.isEqualTo(KBigDecimal.ZERO))
    return KBigDecimal.ZERO to KBigDecimal.ZERO

  val heightInches2 =
    heightFt
      .multiply(footToInchFactor)
      .plus(heightIn)
      .pow(2)
      .divide(metricToImperialFactor, MAX_SCALE, KRoundingMode.HALF_EVEN)

  val lowest = lowestNormalIndex.multiply(heightInches2)
  val highest = highestNormalIndex.multiply(heightInches2)

  return lowest to highest
}

/** How much health risk a waist-to-height ratio shows, see [waistToHeightBand]. */
internal enum class WaistToHeightBand {
  HEALTHY,
  INCREASED,
  HIGH,
}

/**
 * Waist divided by height.
 *
 * @param waist Waist in the unit of [height].
 * @return Ratio, [KBigDecimal.ZERO] if input is negative or zero.
 */
internal fun calculateWaistToHeight(waist: KBigDecimal, height: KBigDecimal): KBigDecimal {
  if (waist <= KBigDecimal.ZERO || height <= KBigDecimal.ZERO) return KBigDecimal.ZERO
  return waist.divide(height, WAIST_TO_HEIGHT_SCALE, KRoundingMode.HALF_EVEN)
}

/** Height in inches for [calculateWaistToHeight]. */
internal fun heightInInches(heightFt: KBigDecimal, heightIn: KBigDecimal): KBigDecimal =
  heightFt.multiply(footToInchFactor).plus(heightIn)

/** The ratio is not meant for a BMI of 35 or more (NICE NG246, NHS). */
internal fun isWaistToHeightUsable(bodyMassIndex: KBigDecimal): Boolean =
  bodyMassIndex < waistToHeightBodyMassLimit

/** Bands of NICE NG246. There is none under 0.4, it is not said to be healthy. */
internal fun waistToHeightBand(ratio: KBigDecimal): WaistToHeightBand? =
  when {
    ratio < waistToHeightHealthy -> null
    ratio < waistToHeightIncreased -> WaistToHeightBand.HEALTHY
    ratio < waistToHeightHigh -> WaistToHeightBand.INCREASED
    else -> WaistToHeightBand.HIGH
  }

private const val WAIST_TO_HEIGHT_SCALE = 4
private val waistToHeightHealthy = KBigDecimal("0.4")
private val waistToHeightIncreased = KBigDecimal("0.5")
private val waistToHeightHigh = KBigDecimal("0.6")
private val waistToHeightBodyMassLimit = KBigDecimal("35")
private val cmToMFactor = KBigDecimal("100")
private val footToInchFactor = KBigDecimal("12")
private val metricToImperialFactor = KBigDecimal("703")

private val lowestNormalIndex = KBigDecimal("18.5")
private val highestNormalIndex = KBigDecimal("25")
