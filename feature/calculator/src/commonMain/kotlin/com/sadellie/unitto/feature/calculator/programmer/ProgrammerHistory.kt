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

import com.sadellie.unitto.core.common.Token
import io.github.sadellie.evaluatto.programmer.NumberBase
import io.github.sadellie.evaluatto.programmer.ProgrammerOperator
import io.github.sadellie.evaluatto.programmer.WordSize
import io.github.sadellie.evaluatto.programmer.programmerConvertNumber
import io.github.sadellie.evaluatto.programmer.programmerFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Calculation of programmer mode: [tokens] in [base] gave [result] in [wordSize]. [timestamp] is
 * in milliseconds and is the id too. An entry with a [label] asks before being deleted.
 */
@Serializable
internal data class ProgrammerHistoryEntry(
  val timestamp: Long,
  val tokens: List<String>,
  val base: NumberBase,
  val wordSize: WordSize,
  val result: Long,
  val label: String? = null,
) {
  val expressionText: String
    get() = tokens.toProgrammerText()

  val resultText: String
    get() = programmerFormat(result, base, wordSize)

  /** Expression written in [newBase], to type it again: FF+1 in DEC is 255+1. */
  fun tokensIn(newBase: NumberBase): List<String> =
    tokens.map { if (it.isProgrammerNumber()) programmerConvertNumber(it, base, newBase, wordSize) else it }

  /** Result as tokens in [newBase]: −1 in DEC is −, 1. Other bases show bits of the word. */
  fun resultTokensIn(newBase: NumberBase): List<String> {
    val text = programmerFormat(result, newBase, wordSize)
    return if (text.startsWith(Token.Operator.MINUS)) listOf(Token.Operator.MINUS, text.removePrefix(Token.Operator.MINUS))
    else listOf(text)
  }
}

/** Newest first, at most [MAX_ENTRIES]. Empty list for nothing saved or a broken save. */
internal fun decodeProgrammerHistory(saved: String): List<ProgrammerHistoryEntry> =
  if (saved.isBlank()) emptyList()
  else runCatching { historyJson.decodeFromString<List<ProgrammerHistoryEntry>>(saved) }.getOrDefault(emptyList())

internal fun List<ProgrammerHistoryEntry>.encode(): String = historyJson.encodeToString(take(MAX_ENTRIES))

/** Typed expression, words are spaced: 12 AND FF, NOT 5. */
internal fun List<String>.toProgrammerText(): String =
  joinToString("") {
      when (it) {
        in SPACED_OPERATORS -> " $it "
        ProgrammerOperator.NOT -> "$it "
        else -> it
      }
    }
    .replace("  ", " ")
    .trim()

internal fun String.isProgrammerNumber() = isNotEmpty() && all { it.isLetterOrDigit() } && this !in WORD_OPERATORS

/** Operators drawn with spaces around them. */
private val SPACED_OPERATORS =
  listOf(
    ProgrammerOperator.AND,
    ProgrammerOperator.OR,
    ProgrammerOperator.XOR,
    ProgrammerOperator.NAND,
    ProgrammerOperator.NOR,
    ProgrammerOperator.MOD,
    ProgrammerOperator.SHIFT_LEFT,
    ProgrammerOperator.SHIFT_RIGHT,
    ProgrammerOperator.ROTATE_LEFT,
    ProgrammerOperator.ROTATE_RIGHT,
  )

/** Operators written with letters, they are not numbers. */
internal val WORD_OPERATORS =
  listOf(
    ProgrammerOperator.AND,
    ProgrammerOperator.OR,
    ProgrammerOperator.XOR,
    ProgrammerOperator.NOT,
    ProgrammerOperator.NAND,
    ProgrammerOperator.NOR,
    ProgrammerOperator.MOD,
    ProgrammerOperator.ROTATE_LEFT,
    ProgrammerOperator.ROTATE_RIGHT,
  )

private val historyJson = Json { ignoreUnknownKeys = true }

private const val MAX_ENTRIES = 200
