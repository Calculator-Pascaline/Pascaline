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

package com.sadellie.unitto.core.common

import java.awt.Font
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The browser has no fonts of its own, the web app brings them (sharedApp wasmJsMain). A character
 * none of them has shows as a box. The Math and Chinese fonts only keep what the app uses, so a new
 * string can need them cut again (see WithFallbackFonts in the web app's main.kt).
 */
class WebFontsTest {
  private val fonts =
    File("../../sharedApp/src/wasmJsMain/composeResources/font")
      .listFiles()!!
      .map { Font.createFont(Font.TRUETYPE_FONT, it) }

  @Test
  fun fontsFound() {
    assertTrue(fonts.size >= 4, "found ${fonts.map { it.name }}")
  }

  @Test
  fun everyCharacterHasAFont() {
    val missing =
      File("src/commonMain/composeResources")
        .listFiles { file -> file.name.startsWith("values") }!!
        .map { File(it, "strings.xml").readText() }
        .flatMap { text -> text.codePoints().toArray().toList() }
        .filter { it > 0x7F && !Character.isWhitespace(it) }
        .toSortedSet()
        .filter { codePoint -> fonts.none { it.canDisplay(codePoint) } }
        .map { "U+%04X %s".format(it, String(Character.toChars(it))) }
    assertEquals(emptyList(), missing)
  }
}
