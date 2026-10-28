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

package com.sadellie.unitto.feature.calculator.datatable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Rows of a [DataTable] with [columns] columns, saved under [key] shortly after every change.
 *
 * @property rows null until loaded.
 */
internal class SavedTable(
  private val key: String,
  private val columns: Int,
  private val userPrefsRepository: UserPreferencesRepository,
  scope: CoroutineScope,
) {
  var rows by mutableStateOf<List<List<String>>?>(null)
    private set

  init {
    scope.launch {
      rows = decode(userPrefsRepository.appData(key).first())
      saveChanges()
    }
  }

  @OptIn(FlowPreview::class)
  private suspend fun saveChanges() {
    snapshotFlow { rows }
      .filterNotNull()
      .drop(1)
      .debounce(SAVE_DELAY_MS)
      .collect { userPrefsRepository.updateAppData(key, encode(it)) }
  }

  /** [row] can be the empty row at the end, which then becomes a real row. */
  fun setCell(row: Int, column: Int, value: String) {
    val current = rows ?: return
    val withRow = if (row >= current.size) current + listOf(List(columns) { "" }) else current
    rows =
      withRow
        .mapIndexed { index, cells ->
          if (index == row) cells.mapIndexed { i, cell -> if (i == column) value else cell } else cells
        }
        .dropLastWhile { cells -> cells.all { it.isBlank() } }
  }

  fun deleteRow(row: Int) {
    rows = rows?.filterIndexed { index, _ -> index != row }
  }

  fun clear() {
    rows = emptyList()
  }

  /** Cells separated by tabs, rows by new lines. */
  private fun encode(rows: List<List<String>>): String =
    rows.joinToString(LINE_SEPARATOR) { it.joinToString(SEPARATOR) }

  private fun decode(text: String): List<List<String>> =
    text
      .split(LINE_SEPARATOR)
      .filter { it.isNotEmpty() }
      .map { line -> line.split(SEPARATOR).let { cells -> List(columns) { cells.getOrElse(it) { "" } } } }
}

private const val SEPARATOR = "\t"
private const val LINE_SEPARATOR = "\n"
private const val SAVE_DELAY_MS = 500L
