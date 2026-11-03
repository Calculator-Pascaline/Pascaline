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

package com.sadellie.unitto.feature.calculator.programmer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import io.github.sadellie.evaluatto.ExpressionException
import io.github.sadellie.evaluatto.programmer.NumberBase
import io.github.sadellie.evaluatto.programmer.ProgrammerOperator
import io.github.sadellie.evaluatto.programmer.WordSize
import io.github.sadellie.evaluatto.programmer.digits
import io.github.sadellie.evaluatto.programmer.programmerCalculate
import io.github.sadellie.evaluatto.programmer.programmerConvertNumber
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Integer calculator: input in one base, result in all of them. Input is a list of tokens (numbers,
 * operators, brackets), the cursor is always at the end. Base, word size and history are saved.
 */
internal class ProgrammerViewModel(private val userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  var base by mutableStateOf(NumberBase.DEC)
    private set

  var wordSize by mutableStateOf(WordSize.QWORD)
    private set

  val tokens = mutableStateListOf<String>()

  /** Calculations, newest first. */
  val history: StateFlow<List<ProgrammerHistoryEntry>> =
    userPrefsRepository
      .appData(HISTORY_KEY)
      .map(::decodeProgrammerHistory)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

  init {
    viewModelScope.launch {
      val savedBase = userPrefsRepository.appData(BASE_KEY).first()
      val savedWordSize = userPrefsRepository.appData(WORD_SIZE_KEY).first()
      NumberBase.entries.firstOrNull { it.name == savedBase }?.let { base = it }
      WordSize.entries.firstOrNull { it.name == savedWordSize }?.let { wordSize = it }
    }
  }

  /** Result of the input, null when it is empty or not complete. */
  val result: Long?
    get() =
      try {
        if (tokens.isEmpty()) null else programmerCalculate(tokens, base, wordSize)
      } catch (e: ExpressionException) {
        null
      } catch (e: ArithmeticException) {
        null
      }

  /** Division by zero and similar, shown instead of a result. */
  val isError: Boolean
    get() =
      try {
        if (tokens.isNotEmpty()) programmerCalculate(tokens, base, wordSize)
        false
      } catch (e: ExpressionException.DivideByZero) {
        true
      } catch (e: ExpressionException) {
        false
      }

  val digits: List<String>
    get() = base.digits()

  fun addDigit(digit: String) {
    if (digit !in base.digits()) return
    val last = tokens.lastOrNull()
    when {
      last != null && last.isNumber() -> {
        val longer = last + digit
        // Only numbers that fit in the word
        if (longer.toULongOrNull(base.radix)?.fitsIn(wordSize) == true) tokens[tokens.lastIndex] = longer
      }
      // (1+2)3 means (1+2)×3
      last == Token.Operator.RIGHT_BRACKET -> tokens.addAll(listOf(Token.Operator.MULTIPLY, digit))
      else -> tokens.add(digit)
    }
  }

  /** Operator between two values. Replaces the operator before it: 1+× is 1×. */
  fun addOperator(operator: String) {
    val last = tokens.lastOrNull()
    when {
      // Minus at the start or after an operator is a sign
      operator == Token.Operator.MINUS && (last == null || last.isOperator() || last == Token.Operator.LEFT_BRACKET) ->
        if (last != Token.Operator.MINUS) tokens.add(operator)
      last == null || last == Token.Operator.LEFT_BRACKET -> return
      last.isOperator() -> tokens[tokens.lastIndex] = operator
      else -> tokens.add(operator)
    }
  }

  /** NOT is before the value: NOT 5, 1 AND NOT 5. */
  fun addNot() {
    val last = tokens.lastOrNull()
    if (last != null && (last.isNumber() || last == Token.Operator.RIGHT_BRACKET)) {
      tokens.add(ProgrammerOperator.AND)
    }
    tokens.add(ProgrammerOperator.NOT)
  }

  fun addLeftBracket() {
    val last = tokens.lastOrNull()
    if (last != null && (last.isNumber() || last == Token.Operator.RIGHT_BRACKET)) tokens.add(Token.Operator.MULTIPLY)
    tokens.add(Token.Operator.LEFT_BRACKET)
  }

  fun addRightBracket() {
    val open = tokens.count { it == Token.Operator.LEFT_BRACKET } - tokens.count { it == Token.Operator.RIGHT_BRACKET }
    val last = tokens.lastOrNull() ?: return
    if (open > 0 && (last.isNumber() || last == Token.Operator.RIGHT_BRACKET)) tokens.add(Token.Operator.RIGHT_BRACKET)
  }

  /** Deletes last digit or last operator. */
  fun delete() {
    val last = tokens.lastOrNull() ?: return
    if (last.isNumber() && last.length > 1) tokens[tokens.lastIndex] = last.dropLast(1)
    else tokens.removeAt(tokens.lastIndex)
  }

  fun clear() = tokens.clear()

  /** Calculation goes to history and its result becomes the input, to keep calculating with it. */
  fun equal() {
    val value = result ?: return
    val entry =
      ProgrammerHistoryEntry(
        timestamp = Clock.System.now().toEpochMilliseconds(),
        tokens = tokens.toList(),
        base = base,
        wordSize = wordSize,
        result = value,
      )
    // A plain number is not a calculation
    if (tokens.any { !it.isProgrammerNumber() }) updateHistory { listOf(entry) + it }
    tokens.clear()
    tokens.addAll(entry.resultTokensIn(base))
  }

  /** Expression of [entry] replaces the input, written in the current base. */
  fun useExpression(entry: ProgrammerHistoryEntry) {
    tokens.clear()
    tokens.addAll(entry.tokensIn(base))
  }

  /** Result of [entry] is added like a typed number: 5+ and result 3 ==> 5+3. */
  fun useResult(entry: ProgrammerHistoryEntry) {
    val last = tokens.lastOrNull()
    when {
      last == null -> Unit
      last.isProgrammerNumber() || last == Token.Operator.RIGHT_BRACKET -> tokens.clear()
    }
    tokens.addAll(entry.resultTokensIn(base))
  }

  fun deleteHistoryEntry(entry: ProgrammerHistoryEntry) = updateHistory { history ->
    history.filter { it.timestamp != entry.timestamp }
  }

  /** Empty label removes it. */
  fun updateHistoryLabel(entry: ProgrammerHistoryEntry, label: String) = updateHistory { history ->
    history.map {
      if (it.timestamp == entry.timestamp) it.copy(label = label.trim().ifEmpty { null }) else it
    }
  }

  fun clearHistory() = updateHistory { emptyList() }

  private fun updateHistory(change: (List<ProgrammerHistoryEntry>) -> List<ProgrammerHistoryEntry>) {
    viewModelScope.launch {
      val current = decodeProgrammerHistory(userPrefsRepository.appData(HISTORY_KEY).first())
      userPrefsRepository.updateAppData(HISTORY_KEY, change(current).encode())
    }
  }

  /** Numbers that are already typed are written in [newBase]. */
  fun selectBase(newBase: NumberBase) {
    if (newBase == base) return
    for (index in tokens.indices) {
      if (tokens[index].isNumber()) tokens[index] = programmerConvertNumber(tokens[index], base, newBase, wordSize)
    }
    base = newBase
    viewModelScope.launch { userPrefsRepository.updateAppData(BASE_KEY, newBase.name) }
  }

  fun selectWordSize(newWordSize: WordSize) {
    wordSize = newWordSize
    // Numbers that don't fit any more are cut to the word, like result
    for (index in tokens.indices) {
      val number = tokens[index].takeIf { it.isNumber() }?.toULongOrNull(base.radix) ?: continue
      if (!number.fitsIn(newWordSize)) tokens[index] = programmerConvertNumber(tokens[index], base, base, newWordSize)
    }
    viewModelScope.launch { userPrefsRepository.updateAppData(WORD_SIZE_KEY, newWordSize.name) }
  }

  private fun String.isNumber() = isProgrammerNumber()

  private fun String.isOperator() = this in ProgrammerOperator.binary || this == ProgrammerOperator.NOT

  private fun ULong.fitsIn(wordSize: WordSize) =
    wordSize.bits == ULong.SIZE_BITS || this < (1uL shl wordSize.bits)
}

private const val BASE_KEY = "programmer_base"
private const val WORD_SIZE_KEY = "programmer_word_size"
private const val HISTORY_KEY = "programmer_history"
private const val STOP_TIMEOUT_MS = 5_000L
