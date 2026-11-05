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

package com.sadellie.unitto.core.ui.textfield

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import com.sadellie.unitto.core.common.Token
import java.io.File
import java.util.zip.CRC32
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The expression editor (typing, brackets, slots, angle boxes, deleting) step by step, compared
 * with a recording (editor_recording.txt): any change in what the keys do shows here. The keys are
 * pressed in random order, always the same (fixed seed), from random cursor positions.
 *
 * To record again on purpose: `-Dpascaline.recordEditor=true`.
 */
class EditorCharacterizationTest {
  private val keys =
    listOf(
      "1", "2", "5", "0", Token.Digit.DOT,
      Token.Operator.PLUS, Token.Operator.MINUS, Token.Operator.MULTIPLY, Token.Operator.DIVIDE,
      Token.Operator.FACTORIAL, Token.Operator.PERCENT, Token.Operator.CONVERT,
      Token.Layout.POWER, Token.Layout.EXP, Token.Layout.POWER_10, Token.Layout.LOG_10,
      Token.Layout.SQRT, Token.Layout.FRACTION, Token.Layout.INTEGRAL, Token.Layout.INDEFINITE_INTEGRAL,
      Token.Layout.SUM, Token.Layout.PRODUCT, Token.Layout.DERIVATIVE,
      Token.Func.LN_BRACKET, Token.Func.ABS_BRACKET,
      Token.Const.PI, Token.Const.E, Token.Const.I, Token.Const.INFINITY,
      Token.Calculus.X, Token.Calculus.N, Token.Calculus.EVALUATED_AT,
      "${Token.Units.PREFIX}km", "${Token.Units.PREFIX}m", "${Token.Units.PREFIX}s",
      "u(n)", "v(n+1)",
    )

  private fun step(state: TextFieldState, random: Random): String {
    // Sometimes the cursor moves first, or a part is selected
    when (random.nextInt(SELECTION_CHANCES)) {
      0 -> state.edit { selection = TextRange(random.nextInt(length + 1)) }
      1 -> {
        val a = random.nextInt(state.text.length + 1)
        val b = random.nextInt(state.text.length + 1)
        state.edit { selection = TextRange(minOf(a, b), maxOf(a, b)) }
      }
    }
    return when (random.nextInt(ACTIONS)) {
      0, 1 -> {
        state.deleteTokens()
        "delete"
      }
      2 -> {
        state.addBracket()
        "bracket"
      }
      3 -> {
        val radians = random.nextBoolean()
        state.addFunctionWithAngleBox(Token.Func.SIN, radians)
        "sin $radians"
      }
      4 -> "skip ${state.skipAngleBox()}"
      5 -> "delete function ${state.deleteFunctionWithEmptyAngleBox()}"
      else -> {
        val key = keys[random.nextInt(keys.size)]
        state.addTokens(key)
        state.chooseAngleUnit(key)
        "type $key"
      }
    }
  }

  private fun record(): String = buildString {
    val random = Random(SEED)
    repeat(SEQUENCES) { sequence ->
      val state = TextFieldState()
      appendLine("# $sequence")
      repeat(STEPS) {
        val action = step(state, random)
        // The helpers as a checksum: the full lines would make megabytes
        val helpers = CRC32().apply { update(helpers(state.text.toString()).toByteArray()) }.value
        appendLine("$action => ${state.text.visible()} [${state.selection.start}, ${state.selection.end}] ${helpers.toString(16)}")
      }
    }
  }

  /**
   * What the helpers say at every cursor position: in a series (s), in a calculus bound (b),
   * fixed cursor, token length ahead, angle box; and how slots are drawn.
   */
  private fun helpers(text: String): String {
    val positions = 0..text.length
    val series = positions.joinToString("") { if (text.isInSeries(it)) "1" else "0" }
    val bound = positions.joinToString("") { if (text.isInCalculusBound(it)) "1" else "0" }
    val fixed = positions.joinToString(",") { text.fixCursor(it).toString() }
    val lengths = positions.joinToString(",") { text.tokenLengthAhead(it).toString() }
    val angle = positions.joinToString(",") { text.angleBoxAt(it)?.toString() ?: "-" }
    val rendering = text.renderSlots(Color.Gray)
    val replacements = rendering.replacements.toSortedMap().entries.joinToString(",") { "${it.key}=${it.value.code}" }
    val styles = rendering.styles.joinToString(",") { "${it.start}-${it.end}:${it.item}" }
    return "  s=$series b=$bound fix=$fixed len=$lengths angle=$angle draw=$replacements styles=$styles"
  }

  // Layout characters as letters, for a readable recording
  private fun CharSequence.visible(): String =
    toString()
      .replace(Token.Layout.SUPERSCRIPT_START, "^{")
      .replace(Token.Layout.SUBSCRIPT_START, "_{")
      .replace(Token.Layout.NUMERATOR_START, "n{")
      .replace(Token.Layout.DENOMINATOR_START, "d{")
      .replace(Token.Layout.RADICAND_START, "r{")
      .replace(Token.Layout.END, "}")

  @Test
  fun editorDoesWhatItDid() {
    val file = File("src/androidHostTest/resources/editor_recording.txt")
    val actual = record()
    if (System.getProperty("pascaline.recordEditor") == "true" || !file.exists()) {
      file.parentFile.mkdirs()
      file.writeText(actual)
    }
    assertEquals(file.readText(), actual)
  }
}

private const val SEED = 2026
private const val SEQUENCES = 200
private const val STEPS = 25
/** 1 in 4 keys moves the cursor, 1 in 4 selects */
private const val SELECTION_CHANCES = 4
private const val ACTIONS = 12
