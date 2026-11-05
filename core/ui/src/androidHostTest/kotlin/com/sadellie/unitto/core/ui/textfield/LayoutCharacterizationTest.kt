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

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.Token
import java.io.File
import java.util.zip.CRC32
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * How the expressions of [EditorCharacterizationTest] are drawn (fractions, powers, roots, bounds,
 * carets), compared with a recording (layout_recording.txt): one checksum per expression.
 */
@RunWith(RobolectricTestRunner::class)
class LayoutCharacterizationTest {
  private val measurer =
    TextMeasurer(
      defaultFontFamilyResolver = createFontFamilyResolver(RuntimeEnvironment.getApplication()),
      defaultDensity = Density(1f),
      defaultLayoutDirection = LayoutDirection.Ltr,
    )

  private fun ExpressionLayout.describe(): String = buildString {
    append("w=$width a=$ascent d=$descent;")
    texts.forEach { append("t ${it.layout.layoutInput.text} ${it.x} ${it.baseline};") }
    bars.forEach { append("b $it;") }
    placeholders.forEach { append("p ${it.rect} ${it.strokeWidth} ${it.angleBoxIndex};") }
    strokes.forEach { append("s ${it.points} ${it.width};") }
    carets.forEach { append("c ${it.index} ${it.x} ${it.top} ${it.bottom};") }
  }

  @Test
  fun layoutDoesWhatItDid() {
    val expressions =
      File("src/androidHostTest/resources/editor_recording.txt")
        .readLines()
        .filter { " => " in it }
        .map { it.substringAfter(" => ").substringBeforeLast(" [").readable() }
        .distinct()
    val actual = buildString {
      expressions.forEach { text ->
        val layout =
          measurer.layoutExpression(
            text,
            TextStyle(fontSize = FONT_SIZE.sp),
            FormatterSymbols(Token.PERIOD, Token.COMMA, false),
            Density(1f),
          )
        val checksum = CRC32().apply { update(layout.describe().toByteArray()) }.value
        appendLine("${checksum.toString(16)} $text")
      }
    }
    val file = File("src/androidHostTest/resources/layout_recording.txt")
    if (!file.exists()) file.writeText(actual)
    assertEquals(file.readText(), actual)
  }

  // Back from the letters of the editor recording
  private fun String.readable(): String =
    replace("^{", Token.Layout.SUPERSCRIPT_START)
      .replace("_{", Token.Layout.SUBSCRIPT_START)
      .replace("n{", Token.Layout.NUMERATOR_START)
      .replace("d{", Token.Layout.DENOMINATOR_START)
      .replace("r{", Token.Layout.RADICAND_START)
      .replace("}", Token.Layout.END)
}

private const val FONT_SIZE = 40
