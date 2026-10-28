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

package io.github.sadellie.evaluatto

import com.sadellie.unitto.core.common.Dimension
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KMathContext
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.PhysicalUnit
import com.sadellie.unitto.core.common.PhysicalUnits
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.isEqualTo

/** Units used anywhere in [this]. */
internal fun Node.units(): List<PhysicalUnit> =
  when (this) {
    is Node.Unit -> listOf(unit)
    is Node.Convert -> value.units() + target.units()
    is Node.Negate -> node.units()
    is Node.Binary -> left.units() + right.units()
    is Node.Function -> argument.units()
    is Node.Log -> base.units() + argument.units()
    is Node.Factorial -> node.units()
    is Node.Integral -> listOfNotNull(lower, upper).flatMap { it.units() } + body.units()
    is Node.Derivative -> body.units() + (at?.units() ?: emptyList())
    is Node.Series -> lower.units() + upper.units() + body.units()
    else -> emptyList()
  }

/**
 * Dimension of [this]: units are multiplied and divided, but only equal dimensions can be added.
 * Functions (sin, ln, …) need dimensionless arguments.
 *
 * @throws ExpressionException.BadExpression When dimensions don't match, like 1_m + 1_s.
 */
internal fun Node.dimension(evaluator: Evaluator): Dimension {
  fun Node.d() = dimension(evaluator)
  fun requireNone(vararg nodes: Node) {
    if (nodes.any { !it.d().isDimensionless }) throw ExpressionException.BadExpression()
  }

  return when (this) {
    is Node.Unit -> unit.dimension
    is Node.Negate -> node.d()
    is Node.Binary ->
      when (operator) {
        BinaryOperator.PLUS,
        BinaryOperator.MINUS,
        BinaryOperator.MODULO -> {
          val left = left.d()
          if (left != right.d()) throw ExpressionException.BadExpression()
          left
        }
        BinaryOperator.MULTIPLY -> left.d() + right.d()
        BinaryOperator.DIVIDE -> left.d() - right.d()
        BinaryOperator.POWER -> {
          val base = left.d()
          requireNone(right)
          if (base.isDimensionless) return Dimension.NONE
          base * evaluator.evaluate(right).real().wholePower()
        }
      }
    is Node.Function -> {
      val argument = argument.d()
      when {
        argument.isDimensionless -> Dimension.NONE
        // √(m²) = m
        function == FunctionType.SQRT -> argument.half()
        // |−5 m| = 5 m
        function == FunctionType.ABS -> argument
        else -> throw ExpressionException.BadExpression()
      }
    }
    is Node.Log -> {
      requireNone(base, argument)
      Dimension.NONE
    }
    is Node.Factorial -> {
      requireNone(node)
      Dimension.NONE
    }
    is Node.Integral -> body.d()
    is Node.Derivative -> body.d()
    is Node.Series -> {
      requireNone(lower, upper)
      if (isProduct) requireNone(body)
      body.d()
    }
    is Node.Convert -> throw ExpressionException.BadExpression()
    else -> Dimension.NONE
  }
}

private fun KBigDecimal.wholePower(): Int {
  val whole = setScale(0, KRoundingMode.HALF_EVEN)
  if (!whole.isEqualTo(this)) throw ExpressionException.BadExpression()
  return whole.toPlainString().toIntOrNull() ?: throw ExpressionException.TooBig()
}

private fun Dimension.half(): Dimension {
  val exponents = listOf(m, kg, s, a, k, mol, cd)
  if (exponents.any { it % 2 != 0 }) throw ExpressionException.BadExpression()
  return Dimension(m / 2, kg / 2, s / 2, a / 2, k / 2, mol / 2, cd / 2)
}

/**
 * Calculates expression with units.
 * - `3_km→_m` converts to given units
 * - otherwise result is shown in units that were used (3_km+2_m = 3.002 km), in named SI unit with a
 *   prefix (2_kg×1_m÷_s^2 = 2 N) or in base units
 * - temperatures in °C and °F only in simple conversions: 20_°C→_°F
 */
internal fun evaluateUnits(
  tree: Node,
  evaluator: Evaluator,
  formatter: NodeFormatter,
): ExpressionResult.Real {
  if (tree.units().any { it.isAffine }) return temperature(tree, evaluator)

  if (tree is Node.Convert) {
    val dimension = tree.value.dimension(evaluator)
    if (dimension != tree.target.dimension(evaluator)) throw ExpressionException.BadExpression()
    val value = evaluator.evaluate(tree.value).real()
    val target = evaluator.evaluate(tree.target).real()
    if (target.isEqualTo(KBigDecimal.ZERO)) throw ExpressionException.DivideByZero()
    return ExpressionResult.Real(value.divide(target, mathContext), unit = formatter.format(tree.target))
  }

  val dimension = tree.dimension(evaluator)
  val value = evaluator.evaluate(tree).real()
  if (dimension.isDimensionless) return ExpressionResult.Real(value)

  val unit = displayUnit(dimension, value, tree.units())
  val shown =
    if (unit != null) value.divide(KBigDecimal(unit.factor), mathContext) to unit.token
    else value to baseUnits(dimension)
  return ExpressionResult.Real(
    value = shown.first,
    unit = shown.second,
    si = UnitValue(value, siUnit(dimension)),
    baseUnits = baseUnits(dimension).takeIf { it != siUnit(dimension) },
    otherUnits =
      when (dimension) {
        // Results in kelvins, like 300_K or 5_K+3_K: value is already in kelvins
        PhysicalUnits.kelvin.dimension -> otherTemperatures(value, PhysicalUnits.kelvin)
        PhysicalUnits.joule.dimension -> listOfNotNull(wattHours(value, unit))
        else -> emptyList()
      },
  )
}

/** SI unit without prefix: named one (N, Hz) or base units. Mass is in kg. */
private fun siUnit(dimension: Dimension): String {
  if (dimension == PhysicalUnits.gram.dimension) return Token.Units.PREFIX + "kg"
  val named = PhysicalUnits.derived.firstOrNull { it.dimension == dimension && it.factor == "1" }
  return named?.token ?: baseUnits(dimension)
}

/** Unit to show [value] (in SI) of [dimension] in, null to use base units. */
/**
 * Unit that is the easiest to read: 1E9 Hz ==> 1 GHz, 5000 m ==> 5 km, 7200 s ==> 2 h. Units that
 * were used and have no prefixes (mi, L) are kept.
 */
private fun displayUnit(dimension: Dimension, value: KBigDecimal, used: List<PhysicalUnit>): PhysicalUnit? {
  val magnitude = value.abs()
  val named = PhysicalUnits.derived.firstOrNull { it.dimension == dimension }
  val isTime = dimension == PhysicalUnits.second.dimension
  val usedUnit = used.firstOrNull { !it.isConstant && !it.isAffine && it.dimension == dimension }
  // Imperial and other units without prefixes stay as they are
  if (usedUnit != null && !isTime && (named == null || !usedUnit.isPrefixedOf(named))) return usedUnit

  if (isTime) {
    return PhysicalUnits.timeUnits.firstOrNull { magnitude >= KBigDecimal(it.factor) }
      ?: prefixed(PhysicalUnits.second, magnitude)
  }
  return named?.let { prefixed(it, magnitude) }
}

/** km is prefixed m, but mi is not. */
private fun PhysicalUnit.isPrefixedOf(base: PhysicalUnit): Boolean =
  !isConstant &&
    dimension == base.dimension &&
    symbol.endsWith(base.symbol) &&
    symbol.length <= base.symbol.length + 1

/** Same unit with an SI prefix (kN, mN), so that value is from 1 to 1000. */
private fun prefixed(unit: PhysicalUnit, magnitude: KBigDecimal): PhysicalUnit {
  if (magnitude.isEqualTo(KBigDecimal.ZERO)) return unit
  val family =
    PhysicalUnits.all
      .filter { it.isPrefixedOf(unit) }
      .sortedByDescending { KBigDecimal(it.factor) }
  return family.firstOrNull { magnitude >= KBigDecimal(it.factor) } ?: family.lastOrNull() ?: unit
}

/** `kg×m²/s²` with layout slots. */
private fun baseUnits(dimension: Dimension): String {
  fun part(symbol: String, power: Int) =
    Token.Units.PREFIX + symbol +
      if (power == 1) "" else Token.Operator.POWER + Token.Layout.SUPERSCRIPT_START + power + Token.Layout.END

  val exponents = dimension.exponents()
  val numerator = exponents.filter { it.second > 0 }.joinToString(Token.Operator.MULTIPLY) { part(it.first, it.second) }
  val denominator =
    exponents.filter { it.second < 0 }.joinToString(Token.Operator.MULTIPLY) { part(it.first, -it.second) }
  if (denominator.isEmpty()) return numerator
  return Token.Layout.NUMERATOR_START +
    numerator.ifEmpty { "1" } +
    Token.Layout.END +
    Token.Layout.DENOMINATOR_START +
    denominator +
    Token.Layout.END
}

/**
 * °C and °F have an offset, so only `number×unit` and `number×unit → unit` make sense. Like Epsilon,
 * everything else with them is not allowed.
 */
private fun temperature(tree: Node, evaluator: Evaluator): ExpressionResult.Real {
  val (valueNode, target) = if (tree is Node.Convert) tree.value to tree.target else tree to null
  val (number, from) = valueNode.numberTimesUnit(evaluator) ?: throw ExpressionException.BadExpression()

  // value in kelvins, then in target unit
  val kelvins = number.multiply(KBigDecimal(from.factor)) + KBigDecimal(from.offset ?: "0")
  if (target == null) {
    return ExpressionResult.Real(number, unit = from.token, otherUnits = otherTemperatures(kelvins, from))
  }
  val to = (target as? Node.Unit)?.unit ?: throw ExpressionException.BadExpression()
  if (from.dimension != to.dimension) throw ExpressionException.BadExpression()

  return ExpressionResult.Real(
    kelvins.inTemperature(to),
    unit = to.token,
    otherUnits = otherTemperatures(kelvins, to),
  )
}

/**
 * [joules] in watt-hours with the prefix that is the easiest to read: 55 J ==> 15.28 mWh, 356.4 MJ
 * ==> 99 kWh. Null when [shown] is already in watt-hours: SI units then give joules.
 */
private fun wattHours(joules: KBigDecimal, shown: PhysicalUnit?): UnitValue? {
  if (shown in PhysicalUnits.wattHourUnits) return null
  val magnitude = joules.abs()
  val units = PhysicalUnits.wattHourUnits
  val unit = units.lastOrNull { magnitude >= KBigDecimal(it.factor) } ?: units.first()
  return UnitValue(joules.divide(KBigDecimal(unit.factor), mathContext), unit.token)
}

/** [kelvins] in K, °C and °F, without [shown]: 328.15 K shown in °C ==> 328.15 K and 131 °F. */
private fun otherTemperatures(kelvins: KBigDecimal, shown: PhysicalUnit): List<UnitValue> =
  PhysicalUnits.temperatureUnits.filter { it != shown }.map { UnitValue(kelvins.inTemperature(it), it.token) }

private fun KBigDecimal.inTemperature(unit: PhysicalUnit): KBigDecimal =
  (this - KBigDecimal(unit.offset ?: "0")).divide(KBigDecimal(unit.factor), mathContext)

/** `20_°C`, `−4×_°F`, `_K` ==> number and unit. */
private fun Node.numberTimesUnit(evaluator: Evaluator): Pair<KBigDecimal, PhysicalUnit>? =
  when {
    this is Node.Unit -> KBigDecimal.ONE to unit
    this is Node.Negate -> node.numberTimesUnit(evaluator)?.let { (value, unit) -> -value to unit }
    this is Node.Binary && operator == BinaryOperator.MULTIPLY && right is Node.Unit && left.units().isEmpty() ->
      evaluator.evaluate(left).real() to (right as Node.Unit).unit
    else -> null
  }

private val mathContext = KMathContext(UNIT_PRECISION, KRoundingMode.HALF_EVEN)
private const val UNIT_PRECISION = 60
