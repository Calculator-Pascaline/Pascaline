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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.ui.textfield.addBracket
import com.sadellie.unitto.core.ui.textfield.addTokens
import com.sadellie.unitto.core.ui.textfield.deleteTokens
import com.sadellie.unitto.core.ui.textfield.placeCursorAtTheEnd
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import io.github.sadellie.evaluatto.SequenceFunction
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** One of u, v and w. Empty formula: the sequence is not used. */
internal class SequenceItem(val name: String) {
  var type by mutableStateOf(SequenceType.EXPLICIT)
  val formula = TextFieldState()
  var firstRank by mutableStateOf("0")
  var firstTerm by mutableStateOf("")
  var secondTerm by mutableStateOf("")

  /** null when the formula is empty or wrong. */
  fun definition(radianMode: Boolean): SequenceDefinition? {
    val text = formula.text.toString()
    if (text.isBlank()) return null
    val function = runCatching { SequenceFunction(text, radianMode) }.getOrNull()
    return SequenceDefinition(
      name = name,
      type = type,
      formula = function,
      firstRank = firstRank.toNumberOrNull()?.toInt() ?: 0,
      firstTerms = listOf(firstTerm, secondTerm).map { it.toNumberOrNull() ?: Double.NaN },
    )
  }
}

/** Sequences, like the NumWorks Sequences app, but as many as wanted. */
internal class SequencesViewModel(private val userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  val numberFormat =
    userPrefsRepository.calculatorPrefs
      .map { NumberFormat(it.formatterSymbols, it.precision, it.outputFormat) }
      .stateIn(viewModelScope, null)

  val radianMode = userPrefsRepository.calculatorPrefs.map { it.radianMode }.stateIn(viewModelScope, true)

  val formatterSymbols = userPrefsRepository.calculatorPrefs.map { it.formatterSymbols }.stateIn(viewModelScope, null)

  val sequences = mutableStateListOf<SequenceItem>()

  var isLoaded by mutableStateOf(false)
    private set

  /** Index of the sequence whose formula is typed with the keyboard, null when none is. */
  var selected by mutableStateOf<Int?>(null)
    private set

  init {
    viewModelScope.launch {
      decode(userPrefsRepository.appData(DATA_KEY).first())
      isLoaded = true
      saveChanges()
    }
  }

  @OptIn(FlowPreview::class)
  private suspend fun saveChanges() {
    snapshotFlow { encode() }
      .drop(1)
      .debounce(SAVE_DELAY_MS)
      .collect { userPrefsRepository.updateAppData(DATA_KEY, it) }
  }

  fun select(index: Int?) {
    selected = index
  }

  /** Adds a sequence with the first free name and starts typing its formula. */
  fun add() {
    val name = Token.Sequence.names.firstOrNull { name -> sequences.none { it.name == name } } ?: return
    sequences += SequenceItem(name)
    selected = sequences.lastIndex
  }

  fun delete(index: Int) {
    if (index !in sequences.indices) return
    sequences.removeAt(index)
    selected = null
  }

  private fun selectedFormula(): TextFieldState? = selected?.let { sequences.getOrNull(it)?.formula }

  fun addTokens(tokens: String) {
    selectedFormula()?.addTokens(tokens)
  }

  fun addBracket() {
    selectedFormula()?.addBracket()
  }

  fun deleteTokens() {
    selectedFormula()?.deleteTokens()
  }

  fun clearInput() {
    selectedFormula()?.clearText()
  }

  /** `u⇥RECURSIVE⇥2uₙ+1⇥0⇥3⇥` per line. */
  private fun encode(): String =
    sequences.joinToString(LINE_SEPARATOR) {
      listOf(it.name, it.type.name, it.formula.text.toString(), it.firstRank, it.firstTerm, it.secondTerm)
        .joinToString(SEPARATOR)
    }

  private fun decode(text: String) {
    text.split(LINE_SEPARATOR).forEach { line ->
      val parts = line.split(SEPARATOR)
      val name = parts.firstOrNull()?.takeIf { it in Token.Sequence.names } ?: return@forEach
      if (sequences.any { it.name == name }) return@forEach
      val item = SequenceItem(name).also { sequences += it }
      item.type = SequenceType.entries.firstOrNull { it.name == parts.getOrNull(1) } ?: SequenceType.EXPLICIT
      item.formula.edit { replace(0, length, parts.getOrElse(2) { "" }) }
      item.formula.placeCursorAtTheEnd()
      item.firstRank = parts.getOrElse(3) { "0" }
      item.firstTerm = parts.getOrElse(4) { "" }
      item.secondTerm = parts.getOrElse(5) { "" }
    }
  }
}

private const val DATA_KEY = "SEQUENCES_DATA"
private const val SEPARATOR = "\t"
private const val LINE_SEPARATOR = "\n"
private const val SAVE_DELAY_MS = 500L
