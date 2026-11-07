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

package com.sadellie.unitto.feature.calculator.integrals

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

private typealias Compiled = (Double, Double, Double) -> Double

private val FUNCTIONS: Map<String, (Double) -> Double> =
  mapOf(
    "asin" to ::asin,
    "acos" to ::acos,
    "atan" to ::atan,
    "sinh" to ::sinh,
    "cosh" to ::cosh,
    "tanh" to ::tanh,
    "sin" to ::sin,
    "cos" to ::cos,
    "tan" to ::tan,
    "exp" to ::exp,
    "ln" to ::ln,
    "log" to ::log10,
    "sqrt" to ::sqrt,
    "abs" to ::abs,
  )

/** Longest names first, so that sinh is not read as sin and h. */
private val NAMES = (FUNCTIONS.keys + "pi").sortedByDescending { it.length }

/** How the keyboard and the calculator write things, as the parser reads them. */
private val SPELLINGS =
  listOf(
    "−" to "-",
    "×" to "*",
    "·" to "*",
    "÷" to "/",
    "," to ".",
    "²" to "^2",
    "³" to "^3",
    "π" to "pi",
    "√" to "sqrt",
  )

/**
 * f(x, y, z) as it is typed: `x·y + 2`, `√(1 − x² − y²)`, `2x sin(y)`, `e^(−z)`. A number or a
 * bracket right before a letter or a bracket multiplies, like on paper.
 *
 * @throws IllegalArgumentException when it can't be read.
 */
internal class MultiFunction(text: String) {
  private val tokens = tokenize(text)
  private var position = 0
  private val compiled: Compiled

  init {
    require(tokens.isNotEmpty())
    compiled = sum()
    require(position == tokens.size)
  }

  /** NaN where it is not defined. */
  fun valueAt(x: Double, y: Double = 0.0, z: Double = 0.0): Double = compiled(x, y, z).takeIf { it.isFinite() } ?: Double.NaN

  private val next: String?
    get() = tokens.getOrNull(position)

  private fun take(): String {
    require(position < tokens.size)
    return tokens[position++]
  }

  private fun sum(): Compiled {
    var left = product()
    while (next == "+" || next == "-") {
      val isPlus = take() == "+"
      val first = left
      val second = product()
      left = if (isPlus) { x, y, z -> first(x, y, z) + second(x, y, z) } else { x, y, z -> first(x, y, z) - second(x, y, z) }
    }
    return left
  }

  private fun product(): Compiled {
    var left = signed()
    while (true) left = nextFactor(left) ?: return left
  }

  /** [left] times or divided by what comes next, null at the end of the product. */
  private fun nextFactor(left: Compiled): Compiled? {
    val operator = next
    return when {
      operator == "*" -> take().let { times(left, signed()) }
      operator == "/" -> take().let { signed().let { right -> { x, y, z -> left(x, y, z) / right(x, y, z) } } }
      // 2x, 3(x + 1), x y
      operator != null && startsFactor(operator) -> times(left, power())
      else -> null
    }
  }

  private fun times(left: Compiled, right: Compiled): Compiled = { x, y, z -> left(x, y, z) * right(x, y, z) }

  private fun startsFactor(token: String) = token == "(" || token.first().isLetterOrDigit() || token.first() == '.'

  private fun signed(): Compiled =
    when (next) {
      "-" -> take().let { signed().let { f -> { x, y, z -> -f(x, y, z) } } }
      "+" -> take().let { signed() }
      else -> power()
    }

  /** From the right: 2^3^2 is 2^9. */
  private fun power(): Compiled {
    val base = primary()
    if (next != "^") return base
    take()
    val exponent = signed()
    return { x, y, z -> base(x, y, z).pow(exponent(x, y, z)) }
  }

  private fun primary(): Compiled {
    val token = take()
    val number = token.toDoubleOrNull()
    val function = FUNCTIONS[token]
    return when {
      number != null -> { _, _, _ -> number }
      token == "(" -> sum().also { require(take() == ")") }
      token == "x" -> { x, _, _ -> x }
      token == "y" -> { _, y, _ -> y }
      token == "z" -> { _, _, z -> z }
      token == "pi" -> { _, _, _ -> PI }
      token == "e" -> { _, _, _ -> E }
      // sin(x) or sin x
      function != null -> power().let { argument -> { x, y, z -> function(argument(x, y, z)) } }
      else -> throw IllegalArgumentException(token)
    }
  }
}

private fun tokenize(text: String): List<String> {
  val clean = SPELLINGS.fold(text.lowercase()) { result, (from, to) -> result.replace(from, to) }.filterNot { it.isWhitespace() }
  val tokens = mutableListOf<String>()
  var index = 0
  while (index < clean.length) {
    val char = clean[index]
    val token =
      when {
        char.isDigit() || char == '.' -> clean.substring(index).takeWhile { it.isDigit() || it == '.' }
        char.isLetter() -> NAMES.firstOrNull { clean.startsWith(it, index) } ?: char.toString()
        else -> char.toString()
      }
    tokens += token
    index += token.length
  }
  return tokens
}
