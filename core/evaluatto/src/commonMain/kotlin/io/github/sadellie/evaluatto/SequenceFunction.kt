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

import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.Token

/**
 * Formula of a term of a sequence, like in the Sequences app of NumWorks. It can use n and terms
 * of the sequences u, v and w: `uₙ` and `uₙ₊₁` (see [Token.Sequence]). What n means depends on the
 * kind of sequence: in `u(n+1) = 2uₙ + n`, n is the rank of the previous term.
 *
 * Integrals, derivatives and sums are not allowed.
 *
 * @throws ExpressionException When input can't be parsed or uses something that is not allowed.
 */
class SequenceFunction(input: String, radianMode: Boolean = true) {
  private val compiled: (Ranks) -> Double

  /** Terms used by the formula: name and shift (1 for uₙ₊₁). */
  val terms: Set<Pair<String, Int>>

  init {
    val tree = Parser(input.tokenize(), KRoundingMode.HALF_EVEN).parse()
    if (tree is Node.Convert || tree.units().isNotEmpty()) throw ExpressionException.BadExpression()
    val used = mutableSetOf<Pair<String, Int>>()
    compiled =
      DoubleCompiler<Ranks>(radianMode) { node ->
          when (node) {
            Node.N -> { ranks -> ranks.n }
            is Node.SequenceTerm -> {
              used += node.name to node.shift
              val name = node.name
              val shift = node.shift
              val term: (Ranks) -> Double = { ranks -> ranks.term(name, shift) }
              term
            }
            else -> throw ExpressionException.BadExpression()
          }
        }
        .compile(tree)
    terms = used
  }

  /**
   * Value for rank [n], [term] gives terms of the sequences: name and shift from n. NaN when it is
   * not defined.
   */
  fun valueAt(n: Double, term: (name: String, shift: Int) -> Double): Double =
    try {
      compiled(Ranks(n, term)).takeIf { it.isFinite() } ?: Double.NaN
    } catch (e: Exception) {
      Double.NaN
    }

  private class Ranks(val n: Double, val term: (String, Int) -> Double)
}
