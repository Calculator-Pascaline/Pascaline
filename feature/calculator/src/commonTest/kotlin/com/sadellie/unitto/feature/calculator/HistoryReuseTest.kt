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
import com.sadellie.unitto.core.model.calculator.decodeReusedResults
import com.sadellie.unitto.core.model.calculator.encode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val MINUS = Token.Operator.MINUS

class HistoryReuseTest {
  @Test
  fun locatesResult() =
    assertEquals(listOf(ReusedResult(1, 2, 1)), locateReused("3+5×2", listOf(PendingReuse(1, "5", 2))))

  @Test
  fun deletedResultIsLeftOut() = assertEquals(emptyList(), locateReused("3+2", listOf(PendingReuse(1, "5", 2))))

  @Test
  fun digitsOfAnotherNumberAreNotTheResult() =
    assertEquals(emptyList(), locateReused("15+5.5", listOf(PendingReuse(1, "5", 0))))

  @Test
  fun sameNumberTwiceIsTheClosestOne() =
    assertEquals(listOf(ReusedResult(1, 2, 1)), locateReused("5+5", listOf(PendingReuse(1, "5", 2))))

  @Test
  fun sameResultTwiceTakesBoth() =
    assertEquals(
      listOf(ReusedResult(1, 0, 1), ReusedResult(1, 2, 1)),
      locateReused("5+5", listOf(PendingReuse(1, "5", 0), PendingReuse(1, "5", 2))),
    )

  @Test
  fun numberIsReplaced() =
    assertEquals(
      RewrittenExpression("6×2", listOf(ReusedResult(1, 0, 1))),
      rewriteReused("5×2", listOf(ReusedResult(1, 0, 1)), mapOf(1 to "6")),
    )

  @Test
  fun positionsFollowLongerNumbers() =
    assertEquals(
      RewrittenExpression("100+7×20", listOf(ReusedResult(1, 0, 3), ReusedResult(2, 6, 2))),
      rewriteReused("5+7×2", listOf(ReusedResult(1, 0, 1), ReusedResult(2, 4, 1)), mapOf(1 to "100", 2 to "20")),
    )

  @Test
  fun negativeResultGetsBrackets() =
    assertEquals(
      RewrittenExpression("3$MINUS(${MINUS}1)", listOf(ReusedResult(1, 2, 4))),
      rewriteReused("3${MINUS}5", listOf(ReusedResult(1, 2, 1)), mapOf(1 to "${MINUS}1")),
    )

  @Test
  fun wholeExpressionNeedsNoBrackets() =
    assertEquals(
      RewrittenExpression("${MINUS}1", listOf(ReusedResult(1, 0, 2))),
      rewriteReused("5", listOf(ReusedResult(1, 0, 1)), mapOf(1 to "${MINUS}1")),
    )

  @Test
  fun otherResultsStay() =
    assertEquals(
      RewrittenExpression("6+7", listOf(ReusedResult(1, 0, 1), ReusedResult(2, 2, 1))),
      rewriteReused("5+7", listOf(ReusedResult(1, 0, 1), ReusedResult(2, 2, 1)), mapOf(1 to "6")),
    )

  @Test
  fun sameResultChangesNothing() =
    assertNull(rewriteReused("5×2", listOf(ReusedResult(1, 0, 1)), mapOf(1 to "5")))

  @Test
  fun positionsOutsideOfExpressionAreIgnored() =
    assertNull(rewriteReused("5", listOf(ReusedResult(1, 4, 1)), mapOf(1 to "6")))

  @Test
  fun savedResultsAreReadBack() {
    val reused = listOf(ReusedResult(1, 0, 2), ReusedResult(12, 5, 1))
    assertEquals(reused, decodeReusedResults(reused.encode()))
  }

  @Test fun noResultsAreNotSaved() = assertNull(emptyList<ReusedResult>().encode())

  @Test fun nothingSavedIsNoResults() = assertEquals(emptyList(), decodeReusedResults(null))
}
