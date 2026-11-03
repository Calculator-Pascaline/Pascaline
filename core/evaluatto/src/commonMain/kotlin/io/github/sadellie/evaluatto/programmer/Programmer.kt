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

package io.github.sadellie.evaluatto.programmer

import com.sadellie.unitto.core.common.Token
import io.github.sadellie.evaluatto.ExpressionException

/** Bases of numbers in programmer mode. */
enum class NumberBase(val radix: Int) {
  HEX(16),
  DEC(10),
  OCT(8),
  BIN(2),
}

/** Numbers have this many bits, results wrap like in a CPU (two's complement). */
enum class WordSize(val bits: Int) {
  QWORD(64),
  DWORD(32),
  WORD(16),
  BYTE(8),
}

/** Operators of programmer mode that are words, others are [Token.Operator]. */
object ProgrammerOperator {
  const val AND = "AND"
  const val OR = "OR"
  const val XOR = "XOR"
  const val NOT = "NOT"
  const val NAND = "NAND"
  const val NOR = "NOR"
  const val MOD = "mod"
  const val SHIFT_LEFT = "<<"
  const val SHIFT_RIGHT = ">>"
  const val ROTATE_LEFT = "RoL"
  const val ROTATE_RIGHT = "RoR"

  /** Operators between two numbers, for checks in input. */
  val binary =
    listOf(
      AND,
      OR,
      XOR,
      NAND,
      NOR,
      MOD,
      SHIFT_LEFT,
      SHIFT_RIGHT,
      ROTATE_LEFT,
      ROTATE_RIGHT,
      Token.Operator.PLUS,
      Token.Operator.MINUS,
      Token.Operator.MULTIPLY,
      Token.Operator.DIVIDE,
    )
}

/**
 * Calculates integer expression of programmer mode. [tokens] are numbers in [base] (digits and A–F,
 * upper case), operators (see [ProgrammerOperator]) and brackets. Missing closing brackets are
 * added. Every result is wrapped to [wordSize].
 *
 * @throws ExpressionException When expression is not complete, or for division by zero.
 */
fun programmerCalculate(tokens: List<String>, base: NumberBase, wordSize: WordSize): Long =
  ProgrammerParser(tokens, base, wordSize).parse()

/**
 * [value] in [base]. Decimal numbers have a sign, others show the bits of the word: −1 in a byte is
 * FF, 377, 11111111.
 */
fun programmerFormat(value: Long, base: NumberBase, wordSize: WordSize): String {
  if (base == NumberBase.DEC) return value.wrap(wordSize).toString().replace("-", Token.Operator.MINUS)
  val bits = value.toULong() and wordSize.mask
  return bits.toString(base.radix).uppercase()
}

/**
 * Number [text] in [from] written in [to], for switching base of the input. Bits of the word stay:
 * FF in a byte is 255 in decimal, which is −1 in a calculation like FF is.
 */
fun programmerConvertNumber(text: String, from: NumberBase, to: NumberBase, wordSize: WordSize): String {
  val bits = text.toULongOrNull(from.radix)?.and(wordSize.mask) ?: return text
  return bits.toString(to.radix).uppercase()
}

/** Digits that can be typed in [base]. */
fun NumberBase.digits(): List<String> = (Token.Digit.all + Token.Letter.all).filter { it.toIntOrNull(radix) != null }

private val WordSize.mask: ULong
  get() = if (bits == 64) ULong.MAX_VALUE else (1uL shl bits) - 1uL

/** Lowest [WordSize.bits] of [this] as a signed number. */
internal fun Long.wrap(wordSize: WordSize): Long {
  if (wordSize.bits == 64) return this
  val shift = 64 - wordSize.bits
  return (this shl shift) shr shift
}

/** Recursive descent, from the loosest operators to the tightest. */
private class ProgrammerParser(
  private val tokens: List<String>,
  private val base: NumberBase,
  private val wordSize: WordSize,
) {
  private var position = 0

  fun parse(): Long {
    if (tokens.isEmpty()) throw ExpressionException.BadExpression()
    val value = or()
    // Closing brackets are added: (1+2 is (1+2)
    while (peek() == Token.Operator.RIGHT_BRACKET) position++
    if (position != tokens.size) throw ExpressionException.BadExpression()
    return value
  }

  private fun peek(): String? = tokens.getOrNull(position)

  private fun next(): String? = tokens.getOrNull(position++)

  private fun binaryLevel(operators: List<String>, operand: () -> Long, apply: (String, Long, Long) -> Long): Long {
    var left = operand()
    while (peek() in operators) {
      val operator = next()!!
      left = apply(operator, left, operand()).wrap(wordSize)
    }
    return left
  }

  private fun or(): Long =
    binaryLevel(listOf(ProgrammerOperator.OR, ProgrammerOperator.NOR), ::xor) { operator, a, b ->
      if (operator == ProgrammerOperator.OR) a or b else (a or b).inv()
    }

  private fun xor(): Long = binaryLevel(listOf(ProgrammerOperator.XOR), ::and) { _, a, b -> a xor b }

  private fun and(): Long =
    binaryLevel(listOf(ProgrammerOperator.AND, ProgrammerOperator.NAND), ::shift) { operator, a, b ->
      if (operator == ProgrammerOperator.AND) a and b else (a and b).inv()
    }

  private fun shift(): Long =
    binaryLevel(
      listOf(
        ProgrammerOperator.SHIFT_LEFT,
        ProgrammerOperator.SHIFT_RIGHT,
        ProgrammerOperator.ROTATE_LEFT,
        ProgrammerOperator.ROTATE_RIGHT,
      ),
      ::sum,
    ) { operator, a, b ->
      val bits = wordSize.bits
      val count = b.coerceIn(0, 64L).toInt()
      when (operator) {
        ProgrammerOperator.SHIFT_LEFT -> if (count >= 64) 0 else a shl count
        // Arithmetic shift: the sign stays, like >> in C for signed numbers
        ProgrammerOperator.SHIFT_RIGHT -> if (count >= 64) (if (a < 0) -1 else 0) else a.wrap(wordSize) shr count
        else -> {
          val n = (b % bits + bits).toInt() % bits
          val word = a.toULong() and wordSize.mask
          val rotated =
            if (n == 0) word
            else if (operator == ProgrammerOperator.ROTATE_LEFT) (word shl n) or (word shr (bits - n))
            else (word shr n) or (word shl (bits - n))
          (rotated and wordSize.mask).toLong()
        }
      }
    }

  private fun sum(): Long =
    binaryLevel(listOf(Token.Operator.PLUS, Token.Operator.MINUS), ::product) { operator, a, b ->
      if (operator == Token.Operator.PLUS) a + b else a - b
    }

  private fun product(): Long =
    binaryLevel(
      listOf(Token.Operator.MULTIPLY, Token.Operator.DIVIDE, ProgrammerOperator.MOD),
      ::unary,
    ) { operator, a, b ->
      when (operator) {
        Token.Operator.MULTIPLY -> a * b
        else -> {
          if (b == 0L) throw ExpressionException.DivideByZero()
          if (operator == Token.Operator.DIVIDE) a / b else a % b
        }
      }
    }

  private fun unary(): Long =
    when (peek()) {
      ProgrammerOperator.NOT -> {
        next()
        unary().inv().wrap(wordSize)
      }
      Token.Operator.MINUS -> {
        next()
        (-unary()).wrap(wordSize)
      }
      else -> primary()
    }

  private fun primary(): Long {
    val token = next() ?: throw ExpressionException.BadExpression()
    if (token == Token.Operator.LEFT_BRACKET) {
      val value = or()
      // Not closed at the end of input is fine
      if (peek() == Token.Operator.RIGHT_BRACKET) next()
      return value
    }
    val number = token.toULongOrNull(base.radix) ?: throw ExpressionException.BadExpression()
    return number.toLong().wrap(wordSize)
  }
}
