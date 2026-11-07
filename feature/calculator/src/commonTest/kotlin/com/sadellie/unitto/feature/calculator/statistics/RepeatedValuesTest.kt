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

package com.sadellie.unitto.feature.calculator.statistics

import kotlin.test.Test
import kotlin.test.assertEquals

class RepeatedValuesTest {
  @Test
  fun newValueGoesAtTheEnd() = assertEquals(2 to null, listOf(listOf("1", ""), listOf("2", "")).placeFor("3"))

  @Test
  fun sameValueCountsTwice() = assertEquals(1 to "2", listOf(listOf("1", ""), listOf("5", "")).placeFor("5"))

  @Test
  fun countGoesOnFromThere() = assertEquals(0 to "4", listOf(listOf("5", "3")).placeFor("5.0"))

  @Test
  fun commaIsTheSameNumber() = assertEquals(0 to "2", listOf(listOf("2.5", "")).placeFor("2,5"))

  @Test
  fun wordsAreNotMerged() = assertEquals(1 to null, listOf(listOf("x", "")).placeFor("x"))
}
