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

package com.sadellie.unitto.feature.calculator

import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.model.calculator.ReusedResult
import kotlin.math.abs

/** Result of calculation [sourceId] put in the input as [text], at about [position]. */
internal data class PendingReuse(val sourceId: Int, val text: String, val position: Int)

/** Expression with new results of earlier calculations and where they are now. */
internal data class RewrittenExpression(val expression: String, val reused: List<ReusedResult>)

/**
 * Where [pending] results are in the finished [expression]. Input can change after a result is put
 * in it: results that are gone are left out, and one that is there twice is the closest one to
 * where it was put.
 */
internal fun locateReused(expression: String, pending: List<PendingReuse>): List<ReusedResult> {
  val taken = mutableListOf<IntRange>()
  return pending
    .mapNotNull { reuse ->
      val start =
        expression
          .occurrencesOf(reuse.text)
          .filter { candidate -> taken.none { it.overlaps(candidate until candidate + reuse.text.length) } }
          .minByOrNull { abs(it - reuse.position) } ?: return@mapNotNull null
      taken += start until start + reuse.text.length
      ReusedResult(reuse.sourceId, start, reuse.text.length)
    }
    .sortedBy { it.start }
}

/**
 * [expression] with [operands] in place of the old results, keys are ids of their calculations.
 *
 * @return `null` when nothing changes.
 */
internal fun rewriteReused(
  expression: String,
  reused: List<ReusedResult>,
  operands: Map<Int, String>,
): RewrittenExpression? {
  val text = StringBuilder()
  val updated = mutableListOf<ReusedResult>()
  var copied = 0
  reused.sortedBy { it.start }.forEach { result ->
    if (result.start < copied || result.end > expression.length) return@forEach
    text.append(expression, copied, result.start)
    val isWhole = result.start == 0 && result.end == expression.length
    val operand =
      operands[result.sourceId]?.operandInExpression(isWhole) ?: expression.substring(result.start, result.end)
    updated += ReusedResult(result.sourceId, text.length, operand.length)
    text.append(operand)
    copied = result.end
  }
  text.append(expression, copied, expression.length)
  val rewritten = text.toString()
  return if (rewritten == expression) null else RewrittenExpression(rewritten, updated)
}

/** All but a positive number needs brackets next to other tokens: 3−5 with −1 is 3−(−1). */
private fun String.operandInExpression(isWhole: Boolean): String {
  val isPositiveNumber = all { it.toString() in Token.Digit.allWithDot }
  return if (isPositiveNumber || isWhole) this else Token.Operator.LEFT_BRACKET + this + Token.Operator.RIGHT_BRACKET
}

private fun String.occurrencesOf(text: String): List<Int> =
  generateSequence(indexOf(text)) { previous -> indexOf(text, previous + 1) }
    .takeWhile { it >= 0 }
    .filter { start -> !isDigitAt(start - 1) && !isDigitAt(start + text.length) }
    .toList()

/** 5 is not in 15 or 5.5. */
private fun String.isDigitAt(index: Int): Boolean =
  getOrNull(index)?.toString()?.let { it in Token.Digit.allWithDot } == true

private fun IntRange.overlaps(other: IntRange): Boolean = first <= other.last && other.first <= last
