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

package com.sadellie.unitto.feature.calculator.prices

/** What is measured: offers can only be compared with offers of the same kind. */
internal enum class QuantityKind(val baseSymbol: String) {
  MASS("kg"),
  VOLUME("L"),
  LENGTH("m"),
  COUNT("pc"),
}

/**
 * Unit of the quantity of an offer.
 *
 * @property factor How many base units ([QuantityKind.baseSymbol]) one of this unit is: 1 g = 0.001 kg.
 */
internal enum class QuantityUnit(val symbol: String, val kind: QuantityKind, val factor: Double) {
  MILLIGRAM("mg", QuantityKind.MASS, 1e-6),
  GRAM("g", QuantityKind.MASS, 0.001),
  KILOGRAM("kg", QuantityKind.MASS, 1.0),
  OUNCE("oz", QuantityKind.MASS, 0.028349523125),
  POUND("lb", QuantityKind.MASS, 0.45359237),
  MILLILITER("mL", QuantityKind.VOLUME, 0.001),
  CENTILITER("cL", QuantityKind.VOLUME, 0.01),
  LITER("L", QuantityKind.VOLUME, 1.0),
  FLUID_OUNCE("fl oz", QuantityKind.VOLUME, 0.0295735295625),
  GALLON("gal", QuantityKind.VOLUME, 3.785411784),
  CENTIMETER("cm", QuantityKind.LENGTH, 0.01),
  METER("m", QuantityKind.LENGTH, 1.0),
  PIECE("pc", QuantityKind.COUNT, 1.0),
}

/** Price for a quantity, like 2.50 for 300 g. */
internal data class Offer(val price: Double, val quantity: Double, val unit: QuantityUnit) {
  /** Price of one base unit: 2.50 for 300 g ==> 8.33 per kg. */
  val unitPrice: Double
    get() = price / (quantity * unit.factor)
}

/**
 * Offers side by side.
 *
 * @property cheapest Index of the offer with the lowest unit price.
 * @property unitPrices Price per base unit of every offer, in the same order.
 */
internal class Comparison(val kind: QuantityKind, val cheapest: Int, val unitPrices: List<Double>) {
  /** How much less the cheapest is per base unit than offer [index]. */
  fun savingPerUnit(index: Int): Double = unitPrices[index] - unitPrices[cheapest]

  /** Share saved with the cheapest instead of offer [index]: 0.25 for 25 % less. */
  fun savingShare(index: Int): Double = savingPerUnit(index) / unitPrices[index]

  /** All offers cost the same per base unit. */
  val isTie: Boolean
    get() = unitPrices.all { kotlin.math.abs(it - unitPrices[cheapest]) <= SAME_PRICE * unitPrices[cheapest] }
}

/** Why offers can't be compared. */
internal enum class ComparisonProblem {
  /** Fewer than two complete offers */
  NOT_ENOUGH_OFFERS,

  /** Like grams and liters */
  DIFFERENT_KINDS,
}

/**
 * Compares offers that have a price and a quantity above 0. Others are left out, their unit price
 * is NaN.
 */
internal fun compare(offers: List<Offer?>): ComparisonResult {
  val complete = offers.withIndex().filter { (_, offer) -> offer != null && offer.price >= 0 && offer.quantity > 0 }
  if (complete.size < 2) return ComparisonResult.Problem(ComparisonProblem.NOT_ENOUGH_OFFERS)
  val kinds = complete.map { it.value!!.unit.kind }.distinct()
  if (kinds.size > 1) return ComparisonResult.Problem(ComparisonProblem.DIFFERENT_KINDS)
  val unitPrices = offers.map { offer -> offer?.takeIf { it.price >= 0 && it.quantity > 0 }?.unitPrice ?: Double.NaN }
  val cheapest = complete.minBy { it.value!!.unitPrice }.index
  return ComparisonResult.Compared(Comparison(kinds.single(), cheapest, unitPrices))
}

internal sealed interface ComparisonResult {
  class Compared(val comparison: Comparison) : ComparisonResult

  class Problem(val problem: ComparisonProblem) : ComparisonResult
}

private const val SAME_PRICE = 1e-9
