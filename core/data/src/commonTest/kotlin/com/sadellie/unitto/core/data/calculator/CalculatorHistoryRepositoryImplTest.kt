/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2026 Elshan Agaev
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

package com.sadellie.unitto.core.data.calculator

import com.sadellie.unitto.core.database.CalculatorHistoryDao
import com.sadellie.unitto.core.database.CalculatorHistoryDaoInMemory
import com.sadellie.unitto.core.database.CalculatorHistoryEntity
import com.sadellie.unitto.core.model.calculator.CalculatorHistoryItem
import com.sadellie.unitto.core.model.calculator.ReusedResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * This only verifies correct binding with dao and conversion from [CalculatorHistoryEntity] to
 * [CalculatorHistoryItem] since [CalculatorHistoryRepository] is a wrapper for
 * [CalculatorHistoryDao].
 */
class CalculatorHistoryRepositoryImplTest {

  private val calculatorHistoryDao = CalculatorHistoryDaoInMemory()

  private val calculatorHistoryRepository = CalculatorHistoryRepositoryImpl(calculatorHistoryDao)

  @Test
  fun historyFlow_returnCorrectItems() = runTest {
    // insert some entries
    val entries =
      listOf(
        CalculatorHistoryEntity(0, 0, "expression 0", "result 0"),
        CalculatorHistoryEntity(1, 1, "expression 1", "result 1"),
        CalculatorHistoryEntity(2, 2, "expression 2", "result 2"),
      )
    entries.forEach { calculatorHistoryDao.insert(it) }

    // get back same data but converted and in right order
    val expected =
      listOf(
        CalculatorHistoryItem(2, "expression 2", "result 2", timestamp = 2),
        CalculatorHistoryItem(1, "expression 1", "result 1", timestamp = 1),
        CalculatorHistoryItem(0, "expression 0", "result 0", timestamp = 0),
      )
    val actual = calculatorHistoryRepository.historyFlow.first()

    assertEquals(expected, actual)
  }

  @Test
  fun update_keepsPlaceAndLabel() = runTest {
    calculatorHistoryDao.insert(CalculatorHistoryEntity(0, 5, "2+3", "5", label = "Rent"))
    calculatorHistoryDao.insert(CalculatorHistoryEntity(1, 6, "5×2", "10"))

    val reused = listOf(ReusedResult(sourceId = 0, start = 0, length = 1))
    calculatorHistoryRepository.update(1, "6×2", "12", reused)
    calculatorHistoryRepository.update(0, "2+4", "6", emptyList())

    assertEquals(
      listOf(
        CalculatorHistoryItem(1, "6×2", "12", timestamp = 6, reused = reused),
        CalculatorHistoryItem(0, "2+4", "6", timestamp = 5, label = "Rent"),
      ),
      calculatorHistoryRepository.historyFlow.first(),
    )
  }

  @Test
  fun add_addToFlow() = runTest {
    // insert some entries
    val entries =
      listOf(
        CalculatorHistoryEntity(0, 0, "expression 0", "result 0"),
        CalculatorHistoryEntity(1, 1, "expression 1", "result 1"),
        CalculatorHistoryEntity(2, 2, "expression 2", "result 2"),
      )
    entries.forEach { calculatorHistoryDao.insert(it) }

    // add one
    calculatorHistoryRepository.add("expression 3", "result 3")

    // descending list, latest added item is first
    val actual = calculatorHistoryRepository.historyFlow.first().first()
    // timestamp is handled internally and not exposed, can't compare entire item
    assertEquals("expression 3", actual.expression)
    assertEquals("result 3", actual.result)
  }

  @Test
  fun delete_removesFromFlow() = runTest {
    // insert some entries
    val entries =
      listOf(
        CalculatorHistoryEntity(0, 0, "expression 0", "result 0"),
        CalculatorHistoryEntity(1, 1, "expression 1", "result 1"),
        CalculatorHistoryEntity(2, 2, "expression 2", "result 2"),
      )
    entries.forEach { calculatorHistoryDao.insert(it) }

    // remove one
    calculatorHistoryRepository.delete(1)

    // make sure it is removed and other entries are in place
    val expected =
      listOf(
        CalculatorHistoryItem(2, "expression 2", "result 2", timestamp = 2),
        CalculatorHistoryItem(0, "expression 0", "result 0", timestamp = 0),
      )
    val actual = calculatorHistoryRepository.historyFlow.first()

    assertEquals(expected, actual)
  }

  @Test
  fun clear_emptyFlow() = runTest {
    // insert some entries
    val entries =
      listOf(
        CalculatorHistoryEntity(0, 0, "expression 0", "result 0"),
        CalculatorHistoryEntity(1, 1, "expression 1", "result 1"),
        CalculatorHistoryEntity(2, 2, "expression 2", "result 2"),
      )
    entries.forEach { calculatorHistoryDao.insert(it) }

    // clear database
    calculatorHistoryRepository.clear()

    // make sure flow is empty
    val expected = emptyList<CalculatorHistoryItem>()
    val actual = calculatorHistoryRepository.historyFlow.first()

    assertEquals(expected, actual)
  }

  @Test
  fun updateLabel_makesFavorite() = runTest {
    calculatorHistoryDao.insert(CalculatorHistoryEntity(0, 0, "expression 0", "result 0"))

    calculatorHistoryRepository.updateLabel(0, " Rent ")
    val labeled = calculatorHistoryRepository.historyFlow.first().first()
    assertEquals("Rent", labeled.label)
    assertEquals(true, labeled.isFavorite)

    // empty label removes favorite
    calculatorHistoryRepository.updateLabel(0, "")
    assertEquals(false, calculatorHistoryRepository.historyFlow.first().first().isFavorite)
  }
}
