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

package com.sadellie.unitto.feature.calculator.functions

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.ui.textfield.addBracket
import com.sadellie.unitto.core.ui.textfield.addTokens
import com.sadellie.unitto.core.ui.textfield.deleteTokens
import com.sadellie.unitto.core.ui.textfield.placeCursorAtTheEnd
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One function of the Functions screen, like `f(x)=x²`.
 *
 * @property color Index in [FUNCTION_COLORS].
 */
internal class GraphFunctionItem(
  val name: String,
  val color: Int,
  val visible: Boolean,
  val input: TextFieldState,
) {
  fun copy(visible: Boolean) = GraphFunctionItem(name, color, visible, input)
}

internal sealed interface FunctionsUIState {
  data object Loading : FunctionsUIState

  /** @property selected Index of the function being edited, null when none is. */
  data class Ready(
    val functions: List<GraphFunctionItem>,
    val selected: Int?,
    val radianMode: Boolean,
    val formatterSymbols: FormatterSymbols,
    val precision: Int,
    val outputFormat: Int,
  ) : FunctionsUIState
}

internal class FunctionsViewModel(private val userPrefsRepository: UserPreferencesRepository) :
  ViewModel() {
  private val _functions = MutableStateFlow<List<GraphFunctionItem>?>(null)
  private val _selected = MutableStateFlow<Int?>(null)

  val uiState =
    combine(userPrefsRepository.calculatorPrefs, _functions, _selected) { prefs, functions, selected ->
        if (functions == null) return@combine FunctionsUIState.Loading
        FunctionsUIState.Ready(
          functions = functions,
          selected = selected?.takeIf { it in functions.indices },
          radianMode = prefs.radianMode,
          formatterSymbols = prefs.formatterSymbols,
          precision = prefs.precision,
          outputFormat = prefs.outputFormat,
        )
      }
      .stateIn(viewModelScope, FunctionsUIState.Loading)

  init {
    viewModelScope.launch {
      _functions.value = decode(userPrefsRepository.graphFunctions.first())
      saveChanges()
    }
  }

  /** Saves functions shortly after they are typed, shown or hidden. */
  @OptIn(FlowPreview::class)
  private suspend fun saveChanges() {
    _functions.filterNotNull().collectLatest { functions ->
      snapshotFlow { encode(functions) }
        .debounce(SAVE_DELAY_MS)
        .collect { userPrefsRepository.updateGraphFunctions(it) }
    }
  }

  fun select(index: Int?) = _selected.update { index }

  /** Adds a function with the first free name and color, and starts editing it. */
  fun add() {
    val functions = _functions.value ?: return
    val name = FUNCTION_NAMES.firstOrNull { name -> functions.none { it.name == name } } ?: return
    val color = FUNCTION_NAMES.indexOf(name) % FUNCTION_COLORS.size
    _functions.value = functions + GraphFunctionItem(name, color, visible = true, TextFieldState())
    _selected.value = functions.size
  }

  fun delete(index: Int) {
    val functions = _functions.value ?: return
    _functions.value = functions.filterIndexed { i, _ -> i != index }
    _selected.value = null
  }

  fun setVisible(index: Int, visible: Boolean) {
    val functions = _functions.value ?: return
    _functions.value = functions.mapIndexed { i, item -> if (i == index) item.copy(visible) else item }
  }

  private fun selectedInput(): TextFieldState? {
    val index = _selected.value ?: return null
    return _functions.value?.getOrNull(index)?.input
  }

  fun addTokens(tokens: String) {
    selectedInput()?.addTokens(tokens)
  }

  fun addBracket() {
    selectedInput()?.addBracket()
  }

  fun deleteTokens() {
    selectedInput()?.deleteTokens()
  }

  fun clearInput() {
    selectedInput()?.clearText()
  }

  /** Equal key finishes editing. */
  fun done() = _selected.update { null }

  /** `f⇥1⇥x²` per line: name, visible flag and input. */
  private fun encode(functions: List<GraphFunctionItem>): String =
    functions.joinToString(LINE_SEPARATOR) {
      listOf(it.name, if (it.visible) "1" else "0", it.input.text.toString()).joinToString(SEPARATOR)
    }

  private fun decode(value: String): List<GraphFunctionItem> =
    value
      .split(LINE_SEPARATOR)
      .mapNotNull { line ->
        val parts = line.split(SEPARATOR, limit = 3)
        if (parts.size < 3 || parts[0] !in FUNCTION_NAMES) return@mapNotNull null
        val input = TextFieldState(parts[2]).apply { placeCursorAtTheEnd() }
        val color = FUNCTION_NAMES.indexOf(parts[0]) % FUNCTION_COLORS.size
        GraphFunctionItem(parts[0], color, visible = parts[1] == "1", input)
      }
      .distinctBy { it.name }
}

/** Same names as NumWorks. */
internal val FUNCTION_NAMES = listOf("f", "g", "h", "p", "q", "r", "s", "t")
private const val SEPARATOR = "\t"
private const val LINE_SEPARATOR = "\n"
private const val SAVE_DELAY_MS = 500L
