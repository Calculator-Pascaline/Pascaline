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

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.isEqualTo
import kotlin.math.abs
import kotlin.math.round

/**
 * Antiderivative (without integration constant). Supports sums, constant factors, common
 * functions of a linear argument `ax+b` (powers, 1/x, eˣ, sin, cos, tan, ln, √), and quotients
 * with a quadratic: (mx+n)/(px²+qx+r) gives ln and atan, c/√(r−px²) gives asin, c/√(px²+r)
 * gives asinh. Products use integration by parts (x·sin(x), x²·eˣ, x·ln(x)) and substitution
 * (2x·cos(x²), x·e^(x²), sin(x)·cos(x)), quotients f′/f give ln|f|. Logarithms have absolute
 * values: ∫1/x = ln|x|. Also powers of sin, cos and tan, products of sin and cos with different
 * arguments, √ of a quadratic, inverse trigonometric and hyperbolic functions.
 *
 * @throws ExpressionException.BadExpression For anything else, like `x·sin(x)`.
 */
internal fun Node.antiderivative(radianMode: Boolean, evaluator: Evaluator): Node {
  if (!dependsOnX()) return times(this, Node.X)
  fun Node.integrate() = antiderivative(radianMode, evaluator)

  return when (this) {
    Node.X -> divide(power(Node.X, number(2)), number(2))
    is Node.Negate -> Node.Negate(node.integrate())
    is Node.Binary ->
      when (operator) {
        BinaryOperator.PLUS -> Node.Binary(BinaryOperator.PLUS, left.integrate(), right.integrate())
        BinaryOperator.MINUS -> Node.Binary(BinaryOperator.MINUS, left.integrate(), right.integrate())
        BinaryOperator.MULTIPLY ->
          when {
            !left.dependsOnX() -> times(left, right.integrate())
            !right.dependsOnX() -> times(left.integrate(), right)
            else ->
              pullOutConstants(this)?.integrate()
                ?: integrateByParts(left, right, radianMode, evaluator)
                ?: integratePolynomialTimesRoot(left, right, radianMode, evaluator)
                ?: integrateExponentialTimesTrig(left, right, radianMode, evaluator)
                ?: integrateBySubstitution(left, right, radianMode, evaluator)
                ?: integrateSinCosPowers(left, right, radianMode, evaluator)
                ?: integrateTrigProduct(left, right, radianMode, evaluator)
                ?: unsupported()
          }
        BinaryOperator.DIVIDE ->
          when {
            !right.dependsOnX() -> divide(left.integrate(), right)
            else -> integrateQuotient(left, right, radianMode, evaluator)
          }
        BinaryOperator.POWER -> integratePower(left, right, radianMode, evaluator)
        BinaryOperator.MODULO -> unsupported()
      }
    is Node.Function -> integrateFunction(this, radianMode, evaluator)
    // ∫ f'(x) dx = f(x)
    is Node.Derivative -> if (at == null) body else unsupported()
    else -> unsupported()
  }
}

/** Has `∫...dx` without bounds, so result needs integration constant. */
internal fun Node.hasIndefiniteIntegral(): Boolean =
  when (this) {
    is Node.Integral -> lower == null || body.hasIndefiniteIntegral()
    // derivative of an antiderivative has no constant
    is Node.Derivative -> false
    is Node.Negate -> node.hasIndefiniteIntegral()
    is Node.Binary -> left.hasIndefiniteIntegral() || right.hasIndefiniteIntegral()
    is Node.Function -> argument.hasIndefiniteIntegral()
    is Node.Log -> base.hasIndefiniteIntegral() || argument.hasIndefiniteIntegral()
    is Node.Factorial -> node.hasIndefiniteIntegral()
    else -> false
  }

/** ∫ numerator/denominator dx, see [antiderivative]. */
private fun integrateQuotient(numerator: Node, denominator: Node, radianMode: Boolean, evaluator: Evaluator): Node {
  // c/√(...)
  if (denominator is Node.Function && denominator.function == FunctionType.SQRT) {
    val inner = denominator.argument
    // g′/√g = 2√g
    proportionToDerivative(numerator, inner, radianMode, evaluator)?.let { c ->
      return times(rationalNode(c * Rational.of(2)), denominator)
    }
    if (!numerator.dependsOnX()) return times(numerator, integrateOverSquareRoot(inner, radianMode, evaluator))
    val top = numerator.polynomialOrNull(evaluator) ?: unsupported()
    return integratePolynomialOverRoot(top, denominator, radianMode, evaluator)
  }
  // c/sin(u), c/cos(u), c/tan(u)
  if (!numerator.dependsOnX() && denominator is Node.Function && denominator.function in TRIG) {
    return times(numerator, integrateTrigPower(denominator.function, denominator.argument, -1, radianMode, evaluator))
  }
  // g′/g = ln|g|, like cos(x)/sin(x). Polynomials up to x² have their own forms below
  if (numerator.dependsOnX() && denominator.polynomialOrNull(evaluator)?.let { it.degree <= 2 } != true) {
    proportionToDerivative(numerator, denominator, radianMode, evaluator)?.let { c ->
      return times(rationalNode(c), lnAbs(denominator))
    }
  }
  // c/cos(u)² = c·cos(u)⁻², c/sin(u)ⁿ
  if (!numerator.dependsOnX() && denominator is Node.Binary && denominator.operator == BinaryOperator.POWER) {
    val base = denominator.left
    if (base is Node.Function && base.function in TRIG && !denominator.right.dependsOnX()) {
      return times(numerator, integratePower(base, Node.Negate(denominator.right), radianMode, evaluator))
    }
  }
  val top = numerator.polynomialOrNull(evaluator) ?: unsupported()
  val bottom = denominator.polynomialOrNull(evaluator) ?: unsupported()
  // Long division first: x³/(x²+1) = x − x/(x²+1)
  if (bottom.degree in 1..2 && top.degree >= bottom.degree) {
    val (quotient, remainder) = top.divideWithRemainder(bottom)
    val polynomialPart = polynomialNode(quotient.antiderivative())
    if (remainder.isZero) return polynomialPart
    return plus(polynomialPart, integrateQuotient(polynomialNode(remainder), denominator, radianMode, evaluator))
  }
  // Degree 3 and more: long division, then partial fractions
  if (bottom.degree >= 3) {
    if (top.degree < bottom.degree) return integratePartialFractions(top, bottom, radianMode)
    val (quotient, remainder) = top.divideWithRemainder(bottom)
    val polynomialPart = polynomialNode(quotient.antiderivative())
    if (remainder.isZero) return polynomialPart
    return plus(polynomialPart, integratePartialFractions(remainder, bottom, radianMode))
  }
  return when {
    // c/(ax+b) = c·ln|ax+b|/a
    bottom.degree == 1 && top.degree <= 0 -> times(numerator, overSlope(lnAbs(denominator), bottom[1]))
    bottom.degree == 2 && top.degree <= 1 -> integrateOverQuadratic(top, bottom, denominator, radianMode)
    else -> unsupported()
  }
}

/**
 * (mx+n)/(px²+qx+r) = m/(2p)·Q'/Q + (n − mq/(2p))/Q: the first part is a logarithm, the second
 * one depends on the discriminant.
 */
private fun integrateOverQuadratic(top: Polynomial, bottom: Polynomial, denominator: Node, radianMode: Boolean): Node {
  val m = top[1]
  val n = top[0]
  val p = bottom[2]
  val q = bottom[1]
  val two = Rational.of(2)
  val logFactor = m / (two * p)
  val rest = n - logFactor * q
  val parts =
    listOfNotNull(
      if (logFactor.isZero) null else times(rationalNode(logFactor), quadraticLn(bottom, denominator)),
      if (rest.isZero) null else times(rationalNode(rest), integrateOneOverQuadratic(bottom, radianMode)),
    )
  return parts.reduceOrNull { sum, part -> Node.Binary(BinaryOperator.PLUS, sum, part) } ?: number(0)
}

/**
 * ∫1/(px²+qx+r) dx with Δ = q²−4pr:
 * - Δ < 0: 2/√−Δ · atan((2px+q)/√−Δ), so ∫1/(1+x²) = atan(x)
 * - Δ = 0: −1/(p(x+q/(2p)))
 * - Δ > 0: 1/√Δ · ln((2px+q−√Δ)/(2px+q+√Δ)), written ln((x−1)/(x+1)) when √Δ is rational
 */
private fun integrateOneOverQuadratic(quadratic: Polynomial, radianMode: Boolean): Node {
  val p = quadratic[2]
  val q = quadratic[1]
  val r = quadratic[0]
  val two = Rational.of(2)
  val discriminant = q * q - Rational.of(4) * p * r
  return when {
    discriminant.isZero ->
      divide(rationalNode(-Rational.ONE / p), linear(Surd.rational(Rational.ONE), Surd.rational(q / (two * p))))
    discriminant.sign < 0 -> {
      val k = Surd.sqrt(-discriminant) ?: unsupported()
      val inverse = k.inverse()
      // (2px+q)/k
      val argument = linear(inverse.times(two * p), inverse.times(q))
      times(surdNode(inverse.times(two)), function(FunctionType.ARTAN, argument).fromDegrees(radianMode))
    }
    else -> {
      val k = Surd.sqrt(discriminant) ?: unsupported()
      val (below, above) =
        if (k.isRational) {
          // Roots are rational: (x − x₁)/(x − x₂)
          val one = Surd.rational(Rational.ONE)
          linear(one, Surd.rational((q - k.coefficient) / (two * p))) to
            linear(one, Surd.rational((q + k.coefficient) / (two * p)))
        } else {
          val start = linear(Surd.rational(two * p), Surd.rational(q))
          Node.Binary(BinaryOperator.MINUS, start, surdNode(k)) to Node.Binary(BinaryOperator.PLUS, start, surdNode(k))
        }
      times(surdNode(k.inverse()), lnAbs(divide(below, above)))
    }
  }
}

/**
 * ∫1/√(px²+qx+r) dx. With u = x + q/(2p) and s = r − q²/(4p):
 * - p < 0 < s: asin(u·√(−p/s)) / √(−p), so ∫1/√(1−x²) = asin(x)
 * - p > 0, s > 0: asinh(u·√(p/s)) / √p
 */
private fun integrateOverSquareRoot(inner: Node, radianMode: Boolean, evaluator: Evaluator): Node {
  val quadratic = inner.polynomialOrNull(evaluator)?.takeIf { it.degree == 2 } ?: unsupported()
  val p = quadratic[2]
  val q = quadratic[1]
  val shift = q / (Rational.of(2) * p)
  val s = quadratic[0] - q * q / (Rational.of(4) * p)
  if (s.isZero || (s.sign < 0 && p.sign < 0)) unsupported()
  val scale = Surd.sqrt(p.abs() / s.abs()) ?: unsupported()
  val factor = (Surd.sqrt(p.abs()) ?: unsupported()).inverse()
  val argument = linear(scale, scale.times(shift))
  val antiderivative =
    when {
      p.sign < 0 -> function(FunctionType.ARSIN, argument).fromDegrees(radianMode)
      s.sign > 0 -> function(FunctionType.ARSINH, argument)
      // √(pu² − |s|), for u above the root
      else -> function(FunctionType.ARCOSH, argument)
    }
  return times(surdNode(factor), antiderivative)
}

/**
 * ∫P(x)·f(x) dx for a polynomial P by parts, null when it doesn't apply.
 * - f is sin, cos, sinh, cosh or an exponential of ax+b: ∫P·f = P·F₁ − P′·F₂ + P″·F₃ − …, where
 *   Fₖ are the antiderivatives of f, until the derivatives of P are 0
 * - f is ln(ax+b): ∫P·ln(u) = Q·ln|u| − ∫Q·a/u, where Q = ∫P
 */
private fun integrateByParts(left: Node, right: Node, radianMode: Boolean, evaluator: Evaluator): Node? {
  val (polynomial, other) =
    left.polynomialOrNull(evaluator)?.let { it to right }
      ?: right.polynomialOrNull(evaluator)?.let { it to left }
      ?: return null
  if (polynomial.degree < 1 || other.polynomialOrNull(evaluator) != null) return null

  if (other is Node.Function && other.function == FunctionType.LN) {
    return integratePolynomialTimesLn(polynomial, other.argument, evaluator)
  }
  if (other is Node.Function && other.function in INVERSE_FUNCTIONS) {
    return integratePolynomialTimesInverse(polynomial, other, radianMode, evaluator)
  }
  if (!other.isRepeatedlyIntegrable(evaluator)) return null

  var antiderivative = other
  var derivative = polynomial
  val terms = mutableListOf<Node>()
  var sign = 1
  while (!derivative.isZero) {
    antiderivative = antiderivative.antiderivative(radianMode, evaluator)
    val term = times(polynomialNode(derivative), antiderivative)
    terms += if (sign > 0) term else Node.Negate(term)
    derivative = derivative.derivative()
    sign = -sign
  }
  return terms.reduce { sum, term -> Node.Binary(BinaryOperator.PLUS, sum, term) }
}

/**
 * By parts twice gives the same integral back, so it has a closed form. With e^u, u = ax+b, and
 * v = cx+d:
 * - ∫e^u·sin(v) = e^u·(a·sin(v) − c·cos(v))/(a²+c²)
 * - ∫e^u·cos(v) = e^u·(a·cos(v) + c·sin(v))/(a²+c²)
 *
 * Null when it doesn't apply. Only in radians: in degrees c would have a π/180 factor.
 */
private fun integrateExponentialTimesTrig(left: Node, right: Node, radianMode: Boolean, evaluator: Evaluator): Node? {
  val (exponential, trig) =
    when {
      left.exponentArgument() != null -> left to right
      right.exponentArgument() != null -> right to left
      else -> return null
    }
  if (trig !is Node.Function || trig.function !in setOf(FunctionType.SIN, FunctionType.COS)) return null
  val a = exponential.exponentArgument()?.polynomialOrNull(evaluator)?.takeIf { it.degree == 1 }?.get(1) ?: return null
  val c = trig.argument.polynomialOrNull(evaluator)?.takeIf { it.degree == 1 }?.get(1) ?: return null
  val sin = function(FunctionType.SIN, trig.argument)
  val cos = function(FunctionType.COS, trig.argument)
  if (!radianMode) {
    // In degrees the inner derivative is c·π/180
    val k = times(rationalNode(c), divide(Node.Pi, number(DEGREES_IN_HALF_TURN)))
    val aNode = rationalNode(a)
    val combination =
      if (trig.function == FunctionType.SIN) minus(times(aNode, sin), times(k, cos))
      else plus(times(aNode, cos), times(k, sin))
    return divide(times(exponential, combination), plus(square(aNode), square(k)))
  }
  val scale = Rational.ONE / (a * a + c * c)
  val combination =
    if (trig.function == FunctionType.SIN) {
      Node.Binary(BinaryOperator.MINUS, times(rationalNode(a * scale), sin), times(rationalNode(c * scale), cos))
    } else {
      Node.Binary(BinaryOperator.PLUS, times(rationalNode(a * scale), cos), times(rationalNode(c * scale), sin))
    }
  return times(exponential, combination)
}

/**
 * ∫fⁿ(u)dx for f = sin, cos, tan and u = ax+b, with reduction formulas:
 * - ∫sinⁿ = −sinⁿ⁻¹·cos/(na) + (n−1)/n·∫sinⁿ⁻²
 * - ∫cosⁿ = sin·cosⁿ⁻¹/(na) + (n−1)/n·∫cosⁿ⁻²
 * - ∫tanⁿ = tanⁿ⁻¹/((n−1)a) − ∫tanⁿ⁻²
 * - ∫1/cos = ln|tan + 1/cos|/a, ∫1/sin = ln|tan(u/2)|/a, ∫1/tan = ln|sin|/a
 * - ∫1/cosⁿ = tan/(cosⁿ⁻²·(n−1)a) + (n−2)/(n−1)·∫1/cosⁿ⁻²
 * - ∫1/sinⁿ = −cos/(sinⁿ⁻¹·(n−1)a) + (n−2)/(n−1)·∫1/sinⁿ⁻²
 * - ∫1/tanⁿ = −1/(tanⁿ⁻¹·(n−1)a) − ∫1/tanⁿ⁻²
 *
 * In degrees, every term divided by a is also multiplied by 180/π.
 */
private fun integrateTrigPower(type: FunctionType, u: Node, n: Int, radianMode: Boolean, evaluator: Evaluator): Node {
  val slope = u.linearSlope(evaluator) ?: unsupported()
  val sin = function(FunctionType.SIN, u)
  val cos = function(FunctionType.COS, u)
  val tan = function(FunctionType.TAN, u)
  val f = function(type, u)
  fun overA(node: Node) = overSlope(node, slope).inDegrees(radianMode)
  fun ratio(top: Int, bottom: Int) = rationalNode(Rational.of(top.toLong()) / Rational.of(bottom.toLong()))
  fun integral(power: Int): Node =
    when {
      power == 0 -> Node.X
      power == 1 -> integrateFunction(f, radianMode, evaluator)
      power == -1 ->
        when (type) {
          FunctionType.COS -> overA(lnAbs(plus(tan, divide(number(1), cos))))
          FunctionType.SIN -> overA(lnAbs(function(FunctionType.TAN, divide(u, number(2)))))
          else -> overA(lnAbs(sin))
        }
      power > 0 ->
        when (type) {
          FunctionType.SIN ->
            plus(
              overA(Node.Negate(divide(times(power(sin, number(power - 1L)), cos), number(power.toLong())))),
              times(ratio(power - 1, power), integral(power - 2)),
            )
          FunctionType.COS ->
            plus(
              overA(divide(times(sin, power(cos, number(power - 1L))), number(power.toLong()))),
              times(ratio(power - 1, power), integral(power - 2)),
            )
          else -> minus(overA(divide(power(tan, number(power - 1L)), number(power - 1L))), integral(power - 2))
        }
      else -> {
        val m = -power
        when (type) {
          FunctionType.COS ->
            plus(
              overA(divide(tan, times(number(m - 1L), power(cos, number(m - 2L))))),
              times(ratio(m - 2, m - 1), integral(power + 2)),
            )
          FunctionType.SIN ->
            plus(
              overA(Node.Negate(divide(cos, times(number(m - 1L), power(sin, number(m - 1L)))))),
              times(ratio(m - 2, m - 1), integral(power + 2)),
            )
          else ->
            minus(overA(Node.Negate(divide(number(1), times(number(m - 1L), power(tan, number(m - 1L)))))), integral(power + 2))
        }
      }
    }
  return integral(n)
}

/**
 * ∫sinᵐ(u)·cosⁿ(u)dx with the same u = ax+b:
 * - m odd: with w = cos, sinᵐ·cosⁿ = sin·(1−w²)^((m−1)/2)·wⁿ, a polynomial in w times −w′/a
 * - n odd: the same with w = sin
 * - both even: cos² = 1 − sin², then powers of sin
 *
 * Null when it doesn't apply.
 */
private fun integrateSinCosPowers(left: Node, right: Node, radianMode: Boolean, evaluator: Evaluator): Node? {
  val first = left.trigPowerOrNull(evaluator) ?: return null
  val second = right.trigPowerOrNull(evaluator) ?: return null
  if (first.type == second.type) return null
  val (sinPower, cosPower) = if (first.type == FunctionType.SIN) first to second else second to first
  if (sinPower.inner.coefficients != cosPower.inner.coefficients) return null
  val u = sinPower.argument
  val slope = sinPower.inner[1].takeIf { sinPower.inner.degree == 1 } ?: return null
  val m = sinPower.power
  val n = cosPower.power
  if (m < 1 || n < 1) return null
  val oneMinusSquare = Polynomial(listOf(Rational.ONE, Rational.ZERO, -Rational.ONE))
  fun powersOf(w: Node, polynomial: Polynomial, sign: Int): Node {
    // ∫wʲ·w′ = wʲ⁺¹/(j+1)
    val terms =
      polynomial.coefficients.mapIndexedNotNull { j, c ->
        if (c.isZero) null
        else times(rationalNode(c * Rational.of(sign.toLong()) / Rational.of(j + 1L)), power(w, number(j + 1L)))
      }
    val sum = terms.reduceOrNull { acc, term -> plus(acc, term) } ?: number(0)
    return overSlope(sum, slope).inDegrees(radianMode)
  }
  return when {
    m % 2 == 1 -> powersOf(function(FunctionType.COS, u), oneMinusSquare.pow((m - 1) / 2) * Polynomial.X.pow(n), -1)
    n % 2 == 1 -> powersOf(function(FunctionType.SIN, u), oneMinusSquare.pow((n - 1) / 2) * Polynomial.X.pow(m), 1)
    else -> {
      // sinᵐ·(1 − sin²)^(n/2)
      val polynomial = oneMinusSquare.pow(n / 2) * Polynomial.X.pow(m)
      polynomial.coefficients
        .mapIndexedNotNull { j, c ->
          if (c.isZero) null
          else times(rationalNode(c), integrateTrigPower(FunctionType.SIN, u, j, radianMode, evaluator))
        }
        .reduce { acc, term -> plus(acc, term) }
    }
  }
}

private class TrigPower(val type: FunctionType, val argument: Node, val inner: Polynomial, val power: Int)

/** sin(u) or sin(u)ⁿ with a whole n, same for cos. */
private fun Node.trigPowerOrNull(evaluator: Evaluator): TrigPower? {
  val (function, power) =
    when {
      this is Node.Function -> this to 1
      this is Node.Binary && operator == BinaryOperator.POWER && left is Node.Function && !right.dependsOnX() -> {
        val n = evaluator.evaluate(right).real().toPlainString().toDoubleOrNull() ?: return null
        if (n != kotlin.math.floor(n) || n > MAX_TRIG_POWER) return null
        (left as Node.Function) to n.toInt()
      }
      else -> return null
    }
  if (function.function != FunctionType.SIN && function.function != FunctionType.COS) return null
  val inner = function.argument.polynomialOrNull(evaluator) ?: return null
  return TrigPower(function.function, function.argument, inner, power)
}

/** Constants hidden in a product: (2·sin²)·cos³ ==> 2·(sin²·cos³). Null when there are none. */
private fun pullOutConstants(product: Node.Binary): Node? {
  val factors = mutableListOf<Node>()
  fun collect(node: Node) {
    if (node is Node.Binary && node.operator == BinaryOperator.MULTIPLY) {
      collect(node.left)
      collect(node.right)
    } else {
      factors += node
    }
  }
  collect(product)
  val constants = factors.filter { !it.dependsOnX() }
  if (constants.isEmpty()) return null
  val rest = factors.filter { it.dependsOnX() }
  return times(
    constants.reduce { acc, node -> times(acc, node) },
    rest.reduce { acc, node -> times(acc, node) },
  )
}

/**
 * ∫P(x)/√Q dx for a polynomial P and a quadratic Q: it is R(x)·√Q + λ·∫1/√Q, where R has a degree
 * one less than P. R and λ come from R′Q + RQ′/2 + λ = P (derivative of the result times √Q).
 */
private fun integratePolynomialOverRoot(top: Polynomial, root: Node.Function, radianMode: Boolean, evaluator: Evaluator): Node {
  val quadratic = root.argument.polynomialOrNull(evaluator)?.takeIf { it.degree == 2 } ?: unsupported()
  val n = top.degree
  if (n < 1) unsupported()
  val half = Rational.ONE / Rational.of(2)
  // Columns: r₀ … rₙ₋₁, then λ. Rows: powers of x from 0 to n
  val columns =
    (0 until n).map { j ->
      val xj = Polynomial.X.pow(j)
      val xjDerivative = xj.derivative()
      xjDerivative * quadratic + xj * quadratic.derivative() * half
    } + Polynomial.constant(Rational.ONE)
  val matrix = (0..n).map { row -> columns.map { it[row] } }
  val solution = solveLinear(matrix, (0..n).map { top[it] }) ?: unsupported()
  val r = Polynomial(solution.dropLast(1))
  val lambda = solution.last()
  val algebraic = times(polynomialNode(r), root)
  if (lambda.isZero) return algebraic
  return plus(algebraic, times(rationalNode(lambda), integrateOverSquareRoot(root.argument, radianMode, evaluator)))
}

/** ∫P·√Q = ∫P·Q/√Q, see [integratePolynomialOverRoot]. Null when it doesn't apply. */
private fun integratePolynomialTimesRoot(left: Node, right: Node, radianMode: Boolean, evaluator: Evaluator): Node? {
  val (polynomial, root) =
    when {
      right is Node.Function && right.function == FunctionType.SQRT -> left.polynomialOrNull(evaluator) to right
      left is Node.Function && left.function == FunctionType.SQRT -> right.polynomialOrNull(evaluator) to left
      else -> return null
    }
  if (polynomial == null || polynomial.degree < 1) return null
  val quadratic = root.argument.polynomialOrNull(evaluator)?.takeIf { it.degree == 2 } ?: return null
  return try {
    integratePolynomialOverRoot(polynomial * quadratic, root, radianMode, evaluator)
  } catch (e: ExpressionException) {
    null
  }
}

/** Solves a square system with exact fractions, null when it has no single solution. */
private fun solveLinear(matrix: List<List<Rational>>, values: List<Rational>): List<Rational>? {
  val size = values.size
  val rows = matrix.mapIndexed { i, row -> (row + values[i]).toMutableList() }.toMutableList()
  for (column in 0 until size) {
    val pivot = (column until size).firstOrNull { !rows[it][column].isZero } ?: return null
    rows[column] = rows[pivot].also { rows[pivot] = rows[column] }
    val pivotValue = rows[column][column]
    for (k in column..size) rows[column][k] = rows[column][k] / pivotValue
    for (other in 0 until size) {
      if (other == column || rows[other][column].isZero) continue
      val factor = rows[other][column]
      for (k in column..size) rows[other][k] = rows[other][k] - factor * rows[column][k]
    }
  }
  return rows.map { it[size] }
}

/**
 * ∫P/D dx with deg P < deg D. D is factored over the fractions into linear and quadratic factors f
 * with multiplicities k, then P/D = Σ Nⱼ/fʲ (j = 1…k), with constant Nⱼ for linear f and linear Nⱼ
 * for quadratic f. The numerators come from a linear system, every fraction is then integrated.
 * Factors of degree 3 and more without rational roots (like x⁴+1) are not supported.
 */
private fun integratePartialFractions(top: Polynomial, bottom: Polynomial, radianMode: Boolean): Node {
  val factorization = bottom.factorize()
  val pieces = mutableListOf<Pair<Polynomial, Int>>()
  if (factorization.zeroRoots > 0) pieces += Polynomial.X to factorization.zeroRoots
  for ((factor, multiplicity) in factorization.factors) {
    if (factor !is Factor.Exact) unsupported()
    pieces += factor.polynomial to multiplicity
  }
  // Unknowns: coefficient of xⁱ in the numerator over fʲ
  class Unknown(val factor: Polynomial, val power: Int, val xPower: Int, val column: Polynomial)
  val unknowns =
    pieces.flatMap { (factor, multiplicity) ->
      (1..multiplicity).flatMap { power ->
        val rest = bottom.divideExact(factor.pow(power)) ?: unsupported()
        (0 until factor.degree).map { i -> Unknown(factor, power, i, Polynomial.X.pow(i) * rest) }
      }
    }
  if (unknowns.size != bottom.degree) unsupported()
  val matrix = (0 until bottom.degree).map { row -> unknowns.map { it.column[row] } }
  val solution = solveLinear(matrix, (0 until bottom.degree).map { top[it] }) ?: unsupported()

  val terms =
    unknowns.indices
      .groupBy { unknowns[it].factor.coefficients to unknowns[it].power }
      .mapNotNull { (_, indices) ->
        val first = unknowns[indices.first()]
        val numerator = Polynomial(List(first.factor.degree) { i -> solution[indices.first { unknowns[it].xPower == i }] })
        if (numerator.isZero) null else integrateOverFactorPower(numerator, first.factor, first.power, radianMode)
      }
  return terms.reduceOrNull { sum, term -> plus(sum, term) } ?: number(0)
}

/** ∫N/fʲ dx for a linear or quadratic f, see [integratePartialFractions]. */
private fun integrateOverFactorPower(numerator: Polynomial, factor: Polynomial, power: Int, radianMode: Boolean): Node {
  val factorNode = polynomialNode(factor)
  if (factor.degree == 1) {
    val c = rationalNode(numerator[0])
    // c/(ax+b)ʲ
    val antiderivative =
      if (power == 1) lnAbs(factorNode)
      else divide(power(factorNode, number(1L - power)), number(1L - power))
    return times(c, overSlope(antiderivative, factor[1]))
  }
  if (power == 1) return integrateOverQuadratic(numerator, factor, factorNode, radianMode)
  // (mx+n)/Qʲ = m/(2p)·Q′/Qʲ + (n − mq/(2p))/Qʲ
  val m = numerator[1]
  val n = numerator[0]
  val p = factor[2]
  val q = factor[1]
  val logFactor = m / (Rational.of(2) * p)
  val rest = n - logFactor * q
  val parts =
    listOfNotNull(
      if (logFactor.isZero) null
      else times(rationalNode(logFactor / Rational.of(1L - power)), power(factorNode, number(1L - power))),
      if (rest.isZero) null else times(rationalNode(rest), integrateOneOverQuadraticPower(factor, power, radianMode)),
    )
  return parts.reduceOrNull { sum, part -> plus(sum, part) } ?: number(0)
}

/**
 * ∫1/Qʲ dx for Q = px²+qx+r and D = 4pr − q², with the reduction formula
 * Iⱼ = (2px+q)/((j−1)·D·Qʲ⁻¹) + 2p(2j−3)/((j−1)·D)·Iⱼ₋₁.
 */
private fun integrateOneOverQuadraticPower(quadratic: Polynomial, power: Int, radianMode: Boolean): Node {
  if (power == 1) return integrateOneOverQuadratic(quadratic, radianMode)
  val p = quadratic[2]
  val q = quadratic[1]
  val d = Rational.of(4) * p * quadratic[0] - q * q
  if (d.isZero) unsupported()
  val j = Rational.of(power.toLong())
  val first =
    divide(
      polynomialNode(Polynomial(listOf(q, Rational.of(2) * p))),
      times(rationalNode((j - Rational.ONE) * d), power(polynomialNode(quadratic), number(power - 1L))),
    )
  val factor = Rational.of(2) * p * (Rational.of(2) * j - Rational.of(3)) / ((j - Rational.ONE) * d)
  return plus(first, times(rationalNode(factor), integrateOneOverQuadraticPower(quadratic, power - 1, radianMode)))
}

/** Quotient and remainder of polynomial division. */
private fun Polynomial.divideWithRemainder(divisor: Polynomial): Pair<Polynomial, Polynomial> {
  val remainder = coefficients.toMutableList()
  val quotient = MutableList(maxOf(degree - divisor.degree + 1, 0)) { Rational.ZERO }
  for (power in degree - divisor.degree downTo 0) {
    val factor = remainder[power + divisor.degree] / divisor.leading
    quotient[power] = factor
    divisor.coefficients.forEachIndexed { i, c -> remainder[power + i] = remainder[power + i] - factor * c }
  }
  return Polynomial(quotient) to Polynomial(remainder)
}

/**
 * Products of sin and cos with different arguments, with product-to-sum formulas:
 * - sin(A)·cos(B) = ½(sin(A+B) + sin(A−B))
 * - sin(A)·sin(B) = ½(cos(A−B) − cos(A+B))
 * - cos(A)·cos(B) = ½(cos(A−B) + cos(A+B))
 *
 * Null when it doesn't apply.
 */
private fun integrateTrigProduct(left: Node, right: Node, radianMode: Boolean, evaluator: Evaluator): Node? {
  if (left !is Node.Function || right !is Node.Function) return null
  val types = setOf(FunctionType.SIN, FunctionType.COS)
  if (left.function !in types || right.function !in types) return null
  val a = left.argument.polynomialOrNull(evaluator)?.takeIf { it.degree <= 1 } ?: return null
  val b = right.argument.polynomialOrNull(evaluator)?.takeIf { it.degree <= 1 } ?: return null
  val sum = polynomialNode(a + b)
  // Positive slope reads better: cos(−x) = cos(x), sin(−x) = −sin(x)
  val isFlipped = (a - b)[1].sign < 0
  val difference = polynomialNode(if (isFlipped) b - a else a - b)
  val sinOfDifference =
    if (isFlipped) Node.Negate(function(FunctionType.SIN, difference)) else function(FunctionType.SIN, difference)
  val result =
    when {
      // sin(A)·cos(B) and cos(A)·sin(B)
      left.function == FunctionType.SIN && right.function == FunctionType.COS ->
        plus(function(FunctionType.SIN, sum), sinOfDifference)
      left.function == FunctionType.COS && right.function == FunctionType.SIN ->
        minus(function(FunctionType.SIN, sum), sinOfDifference)
      left.function == FunctionType.SIN -> minus(function(FunctionType.COS, difference), function(FunctionType.COS, sum))
      else -> plus(function(FunctionType.COS, difference), function(FunctionType.COS, sum))
    }
  return times(half(), result).antiderivative(radianMode, evaluator)
}

/**
 * ∫√(px²+qx+r)dx. With u = x + q/(2p) and s = r − q²/(4p), so that the quadratic is pu² + s:
 * - p < 0 < s: u·√Q/2 + s/(2√−p)·asin(u·√(−p/s)), so ∫√(1−x²) = x√(1−x²)/2 + asin(x)/2
 * - p > 0, s > 0: u·√Q/2 + s/(2√p)·asinh(u·√(p/s))
 */
private fun integrateSquareRootOfQuadratic(root: Node.Function, radianMode: Boolean, evaluator: Evaluator): Node {
  val quadratic = root.argument.polynomialOrNull(evaluator) ?: unsupported()
  val p = quadratic[2]
  val q = quadratic[1]
  val shift = q / (Rational.of(2) * p)
  val s = quadratic[0] - q * q / (Rational.of(4) * p)
  if (s.isZero || (s.sign < 0 && p.sign < 0)) unsupported()
  val u = polynomialNode(Polynomial(listOf(shift, Rational.ONE)))
  val scale = Surd.sqrt(p.abs() / s.abs()) ?: unsupported()
  val factor = (Surd.sqrt(p.abs()) ?: unsupported()).inverse().times(s / Rational.of(2))
  val argument = linear(scale, scale.times(shift))
  val inverse =
    when {
      p.sign < 0 -> function(FunctionType.ARSIN, argument).fromDegrees(radianMode)
      s.sign > 0 -> function(FunctionType.ARSINH, argument)
      else -> function(FunctionType.ARCOSH, argument)
    }
  return plus(times(half(), times(u, root)), times(surdNode(factor), inverse))
}

private fun plus(left: Node, right: Node) = Node.Binary(BinaryOperator.PLUS, left, right)

private fun minus(left: Node, right: Node) = Node.Binary(BinaryOperator.MINUS, left, right)

private fun square(node: Node) = power(node, number(2))

private fun half() = divide(number(1), number(2))

/** √(1−u²) */
private fun oneMinusSquareRoot(u: Node) = function(FunctionType.SQRT, minus(number(1), square(u)))

/**
 * Part of an antiderivative of asin, acos or atan that is not multiplied by the function. In degrees
 * these functions are 180/π bigger, so is their antiderivative.
 */
private fun Node.inDegreesOf(radianMode: Boolean): Node =
  if (radianMode) this else times(divide(number(DEGREES_IN_HALF_TURN), Node.Pi), this)

/** u of e^u or exp(u), null for anything else. */
private fun Node.exponentArgument(): Node? =
  when {
    this is Node.Binary && operator == BinaryOperator.POWER && left == Node.Euler -> right
    this is Node.Function && function == FunctionType.EXP -> argument
    else -> null
  }

/** sin, cos, sinh, cosh of ax+b, e^(ax+b) and c^(ax+b): their antiderivatives are of the same kind. */
private fun Node.isRepeatedlyIntegrable(evaluator: Evaluator): Boolean =
  when {
    this is Node.Function ->
      function in REPEATEDLY_INTEGRABLE && argument.polynomialOrNull(evaluator)?.degree == 1
    this is Node.Binary && operator == BinaryOperator.POWER ->
      !left.dependsOnX() && right.polynomialOrNull(evaluator)?.degree == 1
    else -> false
  }

/**
 * ∫P·f(u) dx = Q·f(u) − ∫Q·f′(u)·a, where Q = ∫P, u = ax+b and f is an inverse function. The
 * remaining integral is a fraction or polynomial over a root, like ∫x²/(1+x²).
 */
private fun integratePolynomialTimesInverse(
  polynomial: Polynomial,
  function: Node.Function,
  radianMode: Boolean,
  evaluator: Evaluator,
): Node? {
  val u = function.argument.polynomialOrNull(evaluator)?.takeIf { it.degree == 1 } ?: return null
  val q = polynomial.antiderivative()
  val numerator = polynomialNode(q * u[1])
  val one = Polynomial.constant(Rational.ONE)
  val square = u * u
  // f′(u) without the inner derivative
  val (remaining, sign) =
    when (function.function) {
      FunctionType.ARTAN -> divide(numerator, polynomialNode(one + square)) to -1
      FunctionType.ARTANH -> divide(numerator, polynomialNode(one - square)) to -1
      FunctionType.ARSIN -> divide(numerator, function(FunctionType.SQRT, polynomialNode(one - square))) to -1
      FunctionType.ARCOS -> divide(numerator, function(FunctionType.SQRT, polynomialNode(one - square))) to 1
      FunctionType.ARSINH -> divide(numerator, function(FunctionType.SQRT, polynomialNode(square + one))) to -1
      FunctionType.ARCOSH -> divide(numerator, function(FunctionType.SQRT, polynomialNode(square - one))) to -1
      else -> return null
    }
  val rest =
    try {
      remaining.antiderivative(radianMode, evaluator)
    } catch (e: ExpressionException) {
      return null
    }
  // asin, acos and atan in degrees are 180/π bigger, so are their derivatives
  val isTrig = function.function in setOf(FunctionType.ARSIN, FunctionType.ARCOS, FunctionType.ARTAN)
  val scaledRest = if (isTrig) rest.inDegreesOf(radianMode) else rest
  val first = times(polynomialNode(q), function)
  return if (sign < 0) minus(first, scaledRest) else plus(first, scaledRest)
}

/** ∫P·ln(u) dx with u = ax+b: Q = ∫P = S·u + R, so the result is (Q − R)·ln|u| − a·∫S. */
private fun integratePolynomialTimesLn(polynomial: Polynomial, u: Node, evaluator: Evaluator): Node? {
  val linear = u.polynomialOrNull(evaluator)?.takeIf { it.degree == 1 } ?: return null
  val q = polynomial.antiderivative()
  val (quotient, remainder) = q.divideByLinear(linear)
  val logPart = times(polynomialNode(q - Polynomial.constant(remainder)), lnAbs(u))
  val rest = (quotient * linear[1]).antiderivative()
  return if (rest.isZero) logPart else Node.Binary(BinaryOperator.MINUS, logPart, polynomialNode(rest))
}

/** Quotient and remainder of division by `ax+b`. */
private fun Polynomial.divideByLinear(linear: Polynomial): Pair<Polynomial, Rational> {
  val a = linear[1]
  val b = linear[0]
  val quotient = MutableList(maxOf(degree, 0)) { Rational.ZERO }
  var carry = Rational.ZERO
  for (power in degree downTo 1) {
    val value = (this[power] - carry) / a
    quotient[power - 1] = value
    carry = value * b
  }
  return Polynomial(quotient) to (this[0] - carry)
}

/**
 * ∫A·B dx = c·∫f(u)du with u = g(x) when B = f(g(x)) and A = c·g′(x), like ∫2x·cos(x²)dx = sin(x²).
 * Also ∫B·B′ = B²/2, like ∫sin(x)·cos(x)dx = sin(x)²/2. Null when it doesn't apply.
 */
private fun integrateBySubstitution(left: Node, right: Node, radianMode: Boolean, evaluator: Evaluator): Node? {
  // B·B′: sin(x)·cos(x) is ½sin(x)² rather than −½cos(x)², so a positive factor is preferred
  val squares =
    listOf(left to right, right to left).mapNotNull { (factor, composite) ->
      proportionToDerivative(factor, composite, radianMode, evaluator)?.let { c -> c to composite }
    }
  (squares.firstOrNull { it.first.sign > 0 } ?: squares.firstOrNull())?.let { (c, composite) ->
    return times(rationalNode(c / Rational.of(2)), power(composite, number(2)))
  }
  for ((factor, composite) in listOf(left to right, right to left)) {
    val (outer, inner) = composite.splitComposition() ?: continue
    val c = proportionToDerivative(factor, inner, radianMode, evaluator) ?: continue
    val antiderivative =
      try {
        outer.antiderivative(radianMode, evaluator)
      } catch (e: ExpressionException) {
        continue
      }
    return times(rationalNode(c), antiderivative.replaceX(inner))
  }
  return null
}

/** f(g(x)) ==> f(x) and g(x) for functions, eᵍ and gⁿ. Null when g is just x. */
private fun Node.splitComposition(): Pair<Node, Node>? {
  val (outer, inner) =
    when {
      this is Node.Function -> Node.Function(function, Node.X) to argument
      this is Node.Binary && operator == BinaryOperator.POWER && !left.dependsOnX() -> power(left, Node.X) to right
      this is Node.Binary && operator == BinaryOperator.POWER && !right.dependsOnX() -> power(Node.X, right) to left
      else -> return null
    }
  return if (inner == Node.X) null else outer to inner
}

/**
 * c when [node] = c·g′(x), null otherwise. Checked numerically at a few points, then c must be a
 * simple fraction like 1/2.
 */
private fun proportionToDerivative(node: Node, g: Node, radianMode: Boolean, evaluator: Evaluator): Rational? {
  if (!node.dependsOnX() || !g.dependsOnX()) return null
  val derivative =
    try {
      g.derivative(radianMode).simplify()
    } catch (e: ExpressionException) {
      return null
    }
  val ratios =
    SAMPLE_POINTS.mapNotNull { x ->
      try {
        val at = Complex(KBigDecimal.valueOf(x))
        val top = evaluator.evaluate(node, at).real().toPlainString().toDouble()
        val bottom = evaluator.evaluate(derivative, at).real().toPlainString().toDouble()
        if (abs(bottom) < MIN_DERIVATIVE) null else top / bottom
      } catch (e: Exception) {
        null
      }
    }
  if (ratios.size < MIN_SAMPLES) return null
  val ratio = ratios.first()
  if (ratios.any { abs(it - ratio) > RATIO_TOLERANCE * maxOf(1.0, abs(ratio)) }) return null
  return simpleFraction(ratio)
}

/** 0.5 ==> 1/2, null when it is not a fraction with a small denominator. */
private fun simpleFraction(value: Double): Rational? {
  for (denominator in 1L..MAX_DENOMINATOR) {
    val numerator = round(value * denominator)
    if (abs(numerator / denominator - value) < RATIO_TOLERANCE * maxOf(1.0, abs(value))) {
      return Rational.of(numerator.toLong()) / Rational.of(denominator)
    }
  }
  return null
}

/** [this] with every x replaced by [value]. */
private fun Node.replaceX(value: Node): Node =
  when (this) {
    Node.X -> value
    is Node.Negate -> Node.Negate(node.replaceX(value))
    is Node.Binary -> Node.Binary(operator, left.replaceX(value), right.replaceX(value))
    is Node.Function -> Node.Function(function, argument.replaceX(value))
    is Node.Log -> Node.Log(base.replaceX(value), argument.replaceX(value))
    is Node.Factorial -> Node.Factorial(node.replaceX(value), isNegated)
    else -> this
  }

internal fun polynomialNode(polynomial: Polynomial): Node {
  val terms =
    (polynomial.degree downTo 0).mapNotNull { power ->
      val coefficient = polynomial[power]
      when {
        coefficient.isZero -> null
        power == 0 -> rationalNode(coefficient)
        else -> {
          val xPower = if (power == 1) Node.X else power(Node.X, number(power.toLong()))
          if (coefficient == Rational.ONE) xPower else times(rationalNode(coefficient), xPower)
        }
      }
    }
  return terms.reduceOrNull { sum, term -> Node.Binary(BinaryOperator.PLUS, sum, term) } ?: number(0)
}

/** ln|u| */
private fun lnAbs(node: Node) = ln(function(FunctionType.ABS, node))

/** ln(Q) when Q is always positive (no real roots, positive leading coefficient), ln|Q| otherwise. */
private fun quadraticLn(quadratic: Polynomial, node: Node): Node {
  val discriminant = quadratic[1] * quadratic[1] - Rational.of(4) * quadratic[2] * quadratic[0]
  return if (discriminant.sign < 0 && quadratic[2].sign > 0) ln(node) else lnAbs(node)
}

/** `a·x + b` with exact coefficients. */
private fun linear(a: Surd, b: Surd): Node {
  val ax = times(surdNode(a), Node.X)
  return if (b.isZero) ax else Node.Binary(BinaryOperator.PLUS, ax, surdNode(b))
}

private fun surdNode(value: Surd): Node {
  val coefficient = rationalNode(value.coefficient)
  if (value.isRational) return coefficient
  val root = function(FunctionType.SQRT, Node.Number(KBigDecimal.valueOf(value.radicand)))
  return if (value.coefficient == Rational.ONE) root else times(coefficient, root)
}

/** 1/(c√r) = √r/(c·r) */
private fun Surd.inverse(): Surd = Surd(Rational.ONE / (coefficient * Rational.of(radicand)), radicand)

private fun Surd.times(value: Rational): Surd = Surd(coefficient * value, radicand)

/** atan and asin give degrees when radian mode is off, antiderivatives need radians. */
private fun Node.fromDegrees(radianMode: Boolean): Node =
  if (radianMode) this else times(divide(Node.Pi, number(DEGREES_IN_HALF_TURN)), this)

private fun Node.polynomialOrNull(evaluator: Evaluator): Polynomial? =
  try {
    toPolynomial(evaluator)
  } catch (e: ExpressionException) {
    null
  }

private fun integratePower(base: Node, exponent: Node, radianMode: Boolean, evaluator: Evaluator): Node {
  // sinⁿ, cosⁿ, tanⁿ
  if (!exponent.dependsOnX() && base is Node.Function && base.function in TRIG) {
    val n = evaluator.evaluate(exponent).real().toPlainString().toDoubleOrNull() ?: unsupported()
    if (n != kotlin.math.floor(n) || n < -MAX_TRIG_POWER || n > MAX_TRIG_POWER) unsupported()
    return integrateTrigPower(base.function, base.argument, n.toInt(), radianMode, evaluator)
  }
  // (ax+b)^n
  if (!exponent.dependsOnX()) {
    val slope = base.linearSlope(evaluator) ?: unsupported()
    val n = evaluator.evaluate(exponent).real()
    if (n.isEqualTo(-KBigDecimal.ONE)) return overSlope(lnAbs(base), slope)
    val next = Node.Binary(BinaryOperator.PLUS, exponent, number(1))
    return overSlope(divide(power(base, next), next), slope)
  }
  val slope = exponent.linearSlope(evaluator) ?: unsupported()
  // e^(ax+b)
  if (base == Node.Euler) return overSlope(power(base, exponent), slope)
  // c^(ax+b) = c^(ax+b) / (a·ln c)
  if (!base.dependsOnX()) return overSlope(divide(power(base, exponent), ln(base)), slope)
  unsupported()
}

private fun integrateFunction(node: Node.Function, radianMode: Boolean, evaluator: Evaluator): Node {
  val u = node.argument
  // √(px²+qx+r)
  if (node.function == FunctionType.SQRT && u.polynomialOrNull(evaluator)?.degree == 2) {
    return integrateSquareRootOfQuadratic(node, radianMode, evaluator)
  }
  val slope = u.linearSlope(evaluator) ?: unsupported()
  val antiderivative =
    when (node.function) {
      FunctionType.SIN -> Node.Negate(function(FunctionType.COS, u)).inDegrees(radianMode)
      FunctionType.COS -> function(FunctionType.SIN, u).inDegrees(radianMode)
      FunctionType.TAN -> Node.Negate(lnAbs(function(FunctionType.COS, u))).inDegrees(radianMode)
      FunctionType.EXP -> node
      FunctionType.SINH -> function(FunctionType.COSH, u)
      FunctionType.COSH -> function(FunctionType.SINH, u)
      FunctionType.LN -> Node.Binary(BinaryOperator.MINUS, times(u, ln(u)), u)
      // 2u√u/3
      FunctionType.SQRT -> divide(times(number(2), times(u, node)), number(3))
      FunctionType.LOG -> Node.Binary(BinaryOperator.MINUS, times(u, node), divide(u, ln(number(10))))
      FunctionType.TANH -> ln(function(FunctionType.COSH, u))
      // By parts: ∫f(u) = u·f(u) − ∫u·f′(u)
      FunctionType.ARTAN ->
        minus(times(u, node), times(half(), ln(plus(number(1), square(u)))).inDegreesOf(radianMode))
      FunctionType.ARSIN -> plus(times(u, node), oneMinusSquareRoot(u).inDegreesOf(radianMode))
      FunctionType.ARCOS -> minus(times(u, node), oneMinusSquareRoot(u).inDegreesOf(radianMode))
      FunctionType.ARSINH -> minus(times(u, node), function(FunctionType.SQRT, plus(square(u), number(1))))
      FunctionType.ARCOSH -> minus(times(u, node), function(FunctionType.SQRT, minus(square(u), number(1))))
      FunctionType.ARTANH -> plus(times(u, node), times(half(), lnAbs(minus(number(1), square(u)))))
      else -> unsupported()
    }
  return overSlope(antiderivative, slope)
}

/** Slope `a` of a linear `ax+b`, null when [this] is not linear in x. */
private fun Node.linearSlope(evaluator: Evaluator): Rational? {
  val polynomial =
    try {
      toPolynomial(evaluator)
    } catch (e: ExpressionException) {
      return null
    }
  if (polynomial.degree != 1) return null
  return polynomial[1]
}

/** Divides by slope of the inner function (reverse chain rule). */
private fun overSlope(node: Node, slope: Rational): Node =
  if (slope == Rational.ONE) node else times(rationalNode(Rational.ONE / slope), node)

/** ∫cos(x°)dx = sin(x°)·180/π */
private fun Node.inDegrees(radianMode: Boolean): Node =
  if (radianMode) this else times(divide(number(DEGREES_IN_HALF_TURN), Node.Pi), this)

internal fun rationalNode(value: Rational): Node {
  val numerator = Node.Number(value.numerator)
  if (value.isInteger) return numerator
  if (value.sign < 0) {
    return Node.Negate(divide(Node.Number(-value.numerator), Node.Number(value.denominator)))
  }
  return divide(numerator, Node.Number(value.denominator))
}

private fun unsupported(): Nothing = throw ExpressionException.BadExpression()

private fun number(value: Long) = Node.Number(KBigDecimal.valueOf(value))

private fun function(type: FunctionType, argument: Node) = Node.Function(type, argument)

private fun ln(node: Node) = Node.Function(FunctionType.LN, node)

private fun times(left: Node, right: Node) = Node.Binary(BinaryOperator.MULTIPLY, left, right)

private fun divide(left: Node, right: Node) = Node.Binary(BinaryOperator.DIVIDE, left, right)

private fun power(left: Node, right: Node) = Node.Binary(BinaryOperator.POWER, left, right)

private const val DEGREES_IN_HALF_TURN = 180L
private val REPEATEDLY_INTEGRABLE =
  setOf(FunctionType.SIN, FunctionType.COS, FunctionType.SINH, FunctionType.COSH, FunctionType.EXP)
private val SAMPLE_POINTS = listOf(0.37, 0.81, 1.23, 1.77, 2.41, 3.13)
private const val MIN_SAMPLES = 3
private const val MIN_DERIVATIVE = 1e-9
private const val RATIO_TOLERANCE = 1e-9
private const val MAX_DENOMINATOR = 1000L
private val TRIG = setOf(FunctionType.SIN, FunctionType.COS, FunctionType.TAN)
private val INVERSE_FUNCTIONS =
  setOf(
    FunctionType.ARTAN,
    FunctionType.ARTANH,
    FunctionType.ARSIN,
    FunctionType.ARCOS,
    FunctionType.ARSINH,
    FunctionType.ARCOSH,
  )
private const val MAX_TRIG_POWER = 20.0
