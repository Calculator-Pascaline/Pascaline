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

package com.sadellie.unitto.feature.calculator.sequences

import com.sadellie.unitto.core.common.Token
import io.github.sadellie.evaluatto.SequenceFunction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SequenceMathTest {
  private val u = Token.Sequence.term("u", 0)
  private val uNext = Token.Sequence.term("u", 1)
  private val v = Token.Sequence.term("v", 0)
  private val times = Token.Operator.MULTIPLY
  private val plus = Token.Operator.PLUS

  private fun sequence(
    name: String,
    type: SequenceType,
    formula: String,
    firstRank: Int = 0,
    vararg firstTerms: Double,
  ) = SequenceDefinition(name, type, SequenceFunction(formula), firstRank, firstTerms.toList())

  @Test
  fun explicit() {
    val system = SequenceSystem(listOf(sequence("u", SequenceType.EXPLICIT, "n${times}n")))
    assertEquals(listOf(0.0, 1.0, 4.0, 9.0), (0..3).map { system.term("u", it) })
  }

  // u(n+1) = 2·uₙ + 1, u(0) = 0: 0, 1, 3, 7, 15
  @Test
  fun recursive() {
    val system = SequenceSystem(listOf(sequence("u", SequenceType.RECURSIVE, "2$times$u${plus}1", 0, 0.0)))
    assertEquals(listOf(0.0, 1.0, 3.0, 7.0, 15.0), (0..4).map { system.term("u", it) })
  }

  // Fibonacci: u(n+2) = uₙ₊₁ + uₙ
  @Test
  fun fibonacci() {
    val system = SequenceSystem(listOf(sequence("u", SequenceType.DOUBLE_RECURSIVE, "$uNext$plus$u", 0, 0.0, 1.0)))
    assertEquals(6765.0, system.term("u", 20), 1e-9)
  }

  // n in the formula is the rank of the previous term: u(n+1) = uₙ + n
  @Test
  fun rankInRecursive() {
    val system = SequenceSystem(listOf(sequence("u", SequenceType.RECURSIVE, "$u${plus}n", 1, 5.0)))
    // u(1) = 5, u(2) = 5 + 1, u(3) = 6 + 2
    assertEquals(8.0, system.term("u", 3), 1e-12)
  }

  @Test
  fun beforeFirstRank() {
    val system = SequenceSystem(listOf(sequence("u", SequenceType.EXPLICIT, "n", 3)))
    assertTrue(system.term("u", 2).isNaN())
  }

  @Test
  fun otherSequence() {
    val system =
      SequenceSystem(
        listOf(sequence("u", SequenceType.EXPLICIT, "2${times}n"), sequence("v", SequenceType.EXPLICIT, "${u}${plus}1"))
      )
    assertEquals(7.0, system.term("v", 3), 1e-12)
  }

  // u(n) = vₙ and v(n) = uₙ need each other
  @Test
  fun needsItself() {
    val system =
      SequenceSystem(listOf(sequence("u", SequenceType.EXPLICIT, v), sequence("v", SequenceType.EXPLICIT, u)))
    assertTrue(system.term("u", 1).isNaN())
  }

  @Test
  fun termsOfTheFormula() = assertEquals(setOf("u" to 1, "u" to 0), SequenceFunction("$uNext$plus$u").terms)

  @Test
  fun longRecursion() {
    val system = SequenceSystem(listOf(sequence("u", SequenceType.RECURSIVE, "$u${plus}1", 0, 0.0)))
    assertEquals(5000.0, system.term("u", 5000), 1e-9)
  }
}
