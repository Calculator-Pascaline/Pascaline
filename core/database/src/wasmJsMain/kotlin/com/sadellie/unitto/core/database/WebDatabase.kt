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

package com.sadellie.unitto.core.database

import com.sadellie.unitto.core.common.WebStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Database of the web app: history and units are saved in the browser, so they are there after a
 * reload. Currency rates are not used (no internet access). One instance of each DAO, everyone
 * sees the same data.
 */
internal class WebDatabase : UnittoDatabase {
  private val history = WebCalculatorHistoryDao()
  private val units = WebUnitsDao()
  private val currencyRates = CurrencyRatesDaoInMemory()

  override fun calculatorHistoryDao(): CalculatorHistoryDao = history

  override fun unitsDao(): UnitsDao = units

  override fun currencyRatesDao(): CurrencyRatesDao = currencyRates
}

private class WebCalculatorHistoryDao : CalculatorHistoryDao {
  private val entries = MutableStateFlow(load())

  override fun getAllDescending(): Flow<List<CalculatorHistoryEntity>> =
    entries.map { currentEntries -> currentEntries.sortedByDescending { it.timestamp } }

  override suspend fun insert(vararg historyEntity: CalculatorHistoryEntity) = update { current ->
    val maxId = current.maxOfOrNull { it.entityId } ?: -1
    val newEntities =
      historyEntity.asList().mapIndexed { index, entity -> entity.copy(entityId = maxId + 1 + index) }
    (current + newEntities).distinctBy { it.entityId }
  }

  override suspend fun delete(entityId: Int) = update { current ->
    current.filter { it.entityId != entityId }
  }

  override suspend fun updateLabel(entityId: Int, label: String) = update { current ->
    current.map { it.withLabel(entityId, label) }
  }

  override suspend fun clear() = update { emptyList() }

  private fun update(change: (List<CalculatorHistoryEntity>) -> List<CalculatorHistoryEntity>) {
    entries.update(change)
    val fields =
      entries.value.flatMap {
        listOf(it.entityId.toString(), it.timestamp.toString(), it.expression, it.result)
      }
    WebStorage.set(HISTORY_KEY, WebStorage.encode(fields))
    // Labels are apart so that history saved before labels still loads: id, label
    val labels =
      entries.value.mapNotNull { entry -> entry.label?.takeIf { it.isNotEmpty() }?.let { entry.entityId to it } }
        .flatMap { (id, label) -> listOf(id.toString(), label) }
    WebStorage.set(HISTORY_LABELS_KEY, WebStorage.encode(labels))
  }

  private fun load(): List<CalculatorHistoryEntity> {
    val labels =
      WebStorage.decode(WebStorage.get(HISTORY_LABELS_KEY) ?: "")
        .chunked(2)
        .filter { it.size == 2 }
        .associate { it[0] to it[1] }
    return WebStorage.decode(WebStorage.get(HISTORY_KEY) ?: "").chunked(HISTORY_FIELDS).mapNotNull {
      if (it.size != HISTORY_FIELDS) return@mapNotNull null
      val label = labels[it[0]]
      CalculatorHistoryEntity(
        entityId = it[0].toIntOrNull() ?: return@mapNotNull null,
        timestamp = it[1].toLongOrNull() ?: return@mapNotNull null,
        expression = it[2],
        result = it[3],
        isFavorite = label != null,
        label = label,
      )
    }
  }
}

/** Favorite units, pairs and how often units are used. */
private class WebUnitsDao : UnitsDao {
  private val entries = MutableStateFlow(load())

  override fun getAllFlow(): Flow<List<UnitsEntity>> = entries

  override suspend fun insertUnit(unit: UnitsEntity) {
    // upsert
    entries.update { current -> current.filter { it.unitId != unit.unitId } + unit }
    val fields =
      entries.value.flatMap {
        listOf(it.unitId, it.isFavorite.toString(), it.pairedUnitId ?: "", it.frequency.toString())
      }
    WebStorage.set(UNITS_KEY, WebStorage.encode(fields))
  }

  override suspend fun getByIdsSortedByFavoriteAndFrequencyDesc(unitIds: List<String>): UnitsEntity? =
    entries.value
      .filter { it.unitId in unitIds }
      .sortedByDescending { it.frequency }
      .maxByOrNull { it.isFavorite }

  override suspend fun getByIds(unitIds: List<String>): List<UnitsEntity> =
    entries.value.filter { it.unitId in unitIds }

  override suspend fun getById(unitId: String): UnitsEntity? =
    entries.value.firstOrNull { it.unitId == unitId }

  override suspend fun clear() {
    entries.update { emptyList() }
    WebStorage.set(UNITS_KEY, "")
  }

  private fun load(): List<UnitsEntity> =
    WebStorage.decode(WebStorage.get(UNITS_KEY) ?: "").chunked(UNITS_FIELDS).mapNotNull {
      if (it.size != UNITS_FIELDS) return@mapNotNull null
      UnitsEntity(
        unitId = it[0],
        isFavorite = it[1] == "true",
        pairedUnitId = it[2].ifEmpty { null },
        frequency = it[3].toIntOrNull() ?: 0,
      )
    }
}

private const val HISTORY_KEY = "history"
private const val HISTORY_FIELDS = 4
private const val HISTORY_LABELS_KEY = "history_labels"
private const val UNITS_KEY = "units"
private const val UNITS_FIELDS = 4
