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

import io.github.sadellie.evaluatto.SequenceFunction

/** Kinds of sequences of the NumWorks Sequences app. */
internal enum class SequenceType {
  /** `u(n) = f(n)` */
  EXPLICIT,

  /** `u(n+1) = f(uₙ, n)` with u(n₀) given */
  RECURSIVE,

  /** `u(n+2) = f(uₙ₊₁, uₙ, n)` with u(n₀) and u(n₀+1) given */
  DOUBLE_RECURSIVE,
}

/**
 * @property formula null when it is empty or wrong, then the sequence has no terms.
 * @property firstRank n₀, the rank of the first term.
 * @property firstTerms u(n₀) and u(n₀+1) for recursive sequences.
 */
internal class SequenceDefinition(
  val name: String,
  val type: SequenceType,
  val formula: SequenceFunction?,
  val firstRank: Int,
  val firstTerms: List<Double>,
)

/**
 * Terms of u, v and w together, because their formulas can use each other. Terms are kept once
 * they are calculated. A term that needs itself (like `u(n) = uₙ + 1`) is NaN.
 */
internal class SequenceSystem(definitions: List<SequenceDefinition>) {
  private val byName = definitions.associateBy { it.name }
  private val terms = HashMap<Pair<String, Int>, Double>()
  private val calculating = HashSet<Pair<String, Int>>()

  /** u(n), NaN when it is not defined (before the first rank, wrong formula, too big). */
  fun term(name: String, n: Int): Double {
    val sequence = byName[name] ?: return Double.NaN
    val formula = sequence.formula ?: return Double.NaN
    if (n < sequence.firstRank || n - sequence.firstRank > MAX_RANKS) return Double.NaN
    val key = name to n
    terms[key]?.let { return it }
    if (calculating.isEmpty() && sequence.type != SequenceType.EXPLICIT) {
      // Earlier terms one after another: going straight to u(5000) would need 5000 nested calls
      for (rank in sequence.firstRank until n) if ((name to rank) !in terms) term(name, rank)
    }
    if (!calculating.add(key)) return Double.NaN

    val previous = n - sequence.firstRank
    val value =
      when (sequence.type) {
        SequenceType.EXPLICIT -> formula.at(n)
        SequenceType.RECURSIVE -> if (previous == 0) sequence.firstTerms.firstOrNaN() else formula.at(n - 1)
        SequenceType.DOUBLE_RECURSIVE ->
          when (previous) {
            0 -> sequence.firstTerms.firstOrNaN()
            1 -> sequence.firstTerms.getOrNull(1) ?: Double.NaN
            else -> formula.at(n - 2)
          }
      }
    calculating.remove(key)
    terms[key] = value
    return value
  }

  /** Formula at rank [rank]: uₙ is the term at [rank], uₙ₊₁ the next one. */
  private fun SequenceFunction.at(rank: Int): Double =
    valueAt(rank.toDouble()) { other, shift -> term(other, rank + shift) }

  private fun List<Double>.firstOrNaN() = firstOrNull() ?: Double.NaN
}

/** Terms further than this from the first one are not calculated. */
internal const val MAX_RANKS = 10_000
