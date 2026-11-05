/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

import com.sadellie.unitto.core.common.defaultIODispatcher
import com.sadellie.unitto.core.database.CalculatorHistoryDao
import com.sadellie.unitto.core.database.CalculatorHistoryEntity
import com.sadellie.unitto.core.model.calculator.CalculatorHistoryItem
import com.sadellie.unitto.core.model.calculator.ReusedResult
import com.sadellie.unitto.core.model.calculator.decodeReusedResults
import com.sadellie.unitto.core.model.calculator.encode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CalculatorHistoryRepositoryImpl(private val calculatorHistoryDao: CalculatorHistoryDao) :
  CalculatorHistoryRepository {
  override val historyFlow: Flow<List<CalculatorHistoryItem>> =
    calculatorHistoryDao
      .getAllDescending()
      .map { it.toHistoryItemList() }
      .flowOn(defaultIODispatcher)

  @OptIn(ExperimentalTime::class)
  override suspend fun add(expression: String, result: String, reused: List<ReusedResult>) =
    withContext(defaultIODispatcher) {
      calculatorHistoryDao.insert(
        CalculatorHistoryEntity(
          timestamp = Clock.System.now().toEpochMilliseconds(),
          expression = expression,
          result = result,
          reused = reused.encode(),
        )
      )
    }

  override suspend fun update(itemId: Int, expression: String, result: String, reused: List<ReusedResult>) =
    withContext(defaultIODispatcher) {
      calculatorHistoryDao.updateCalculation(itemId, expression, result, reused.encode())
    }

  override suspend fun delete(itemId: Int) =
    withContext(defaultIODispatcher) { calculatorHistoryDao.delete(itemId) }

  override suspend fun updateLabel(itemId: Int, label: String) =
    withContext(defaultIODispatcher) { calculatorHistoryDao.updateLabel(itemId, label.trim()) }

  override suspend fun clear() = withContext(defaultIODispatcher) { calculatorHistoryDao.clear() }

  private suspend fun List<CalculatorHistoryEntity>.toHistoryItemList():
    List<CalculatorHistoryItem> =
    withContext(Dispatchers.Default) {
      this@toHistoryItemList.map {
        CalculatorHistoryItem(
          id = it.entityId,
          expression = it.expression,
          result = it.result,
          timestamp = it.timestamp,
          label = it.label,
          reused = decodeReusedResults(it.reused),
        )
      }
    }
}
