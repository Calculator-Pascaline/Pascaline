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

package com.sadellie.unitto.feature.calculator

import com.sadellie.unitto.core.common.Token

/**
 * Sections of "…" menu.
 *
 * @property key Saved in history, don't change.
 * @property isExpression Lines use layout slots (powers, fractions). Otherwise plain text.
 * @property isInsertable Tap inserts the line into input.
 */
internal enum class MenuSection(
  val key: String,
  val isExpression: Boolean,
  val isInsertable: Boolean,
) {
  FACTORIZATION("factorization", isExpression = true, isInsertable = true),
  ROOTS("roots", isExpression = true, isInsertable = false),
  DISCRIMINANT("discriminant", isExpression = true, isInsertable = false),
  DECIMAL("decimal", isExpression = false, isInsertable = true),
  HEXADECIMAL("hexadecimal", isExpression = false, isInsertable = false),
  BINARY("binary", isExpression = false, isInsertable = false),
  PRIME_FACTORS("prime_factors", isExpression = true, isInsertable = true),
  /** Line is `real,imaginary`, drawn as a point on complex plane. */
  COMPLEX_PLANE("complex_plane", isExpression = false, isInsertable = false),
  REAL_PART("real_part", isExpression = true, isInsertable = true),
  IMAGINARY_PART("imaginary_part", isExpression = true, isInsertable = true),
  MODULUS("modulus", isExpression = true, isInsertable = true),
  ARGUMENT("argument", isExpression = true, isInsertable = false),
  /** Result with units in SI: 5 km ==> 5000 m */
  SI_UNITS("si_units", isExpression = true, isInsertable = true),
  /** Temperature in the other units: 55 °C ==> 328.15 K and 131 °F */
  OTHER_TEMPERATURES("other_temperatures", isExpression = true, isInsertable = true),
}

/**
 * Result of a calculation as it is stored in history.
 *
 * @property lines Shown under the expression. First one is the result, others are extra
 *   information, like approximation or fraction. Empty means error.
 * @property menu Shown in "…" menu.
 */
internal data class HistoryResult(
  val lines: List<String>,
  val menu: List<Pair<MenuSection, List<String>>> = emptyList(),
) {
  val hasMenu: Boolean
    get() = menu.isNotEmpty()

  /** First line without things that can't be calculated: ≈ and integration constant. */
  val insertable: String?
    get() = lines.firstOrNull()?.toInsertable()

  /** One string with lines. Menu goes after [SECTION_SEPARATOR], each section has a header. */
  fun encode(): String {
    val visible = lines.joinToString(LINE_SEPARATOR)
    val sections = menu.filter { it.second.isNotEmpty() }
    if (sections.isEmpty()) return visible
    val menuText =
      sections.joinToString(LINE_SEPARATOR) { (section, sectionLines) ->
        (listOf(SECTION_HEADER + section.key) + sectionLines).joinToString(LINE_SEPARATOR)
      }
    return visible + LINE_SEPARATOR + SECTION_SEPARATOR + LINE_SEPARATOR + menuText
  }

  companion object {
    fun decode(result: String): HistoryResult {
      val all = result.lines().filter { it.isNotEmpty() }
      val separator = all.indexOf(SECTION_SEPARATOR)
      if (separator == -1) return HistoryResult(all)
      val menuLines = all.drop(separator + 1)
      val visible = all.take(separator)
      if (menuLines.firstOrNull()?.startsWith(SECTION_HEADER) != true) {
        return HistoryResult(visible, decodeOldMenu(menuLines))
      }

      val menu = mutableListOf<Pair<MenuSection, MutableList<String>>>()
      menuLines.forEach { line ->
        if (line.startsWith(SECTION_HEADER)) {
          val section = MenuSection.entries.firstOrNull { it.key == line.removePrefix(SECTION_HEADER) }
          if (section != null) menu += section to mutableListOf()
        } else {
          menu.lastOrNull()?.second?.add(line)
        }
      }
      return HistoryResult(visible, menu)
    }

    /** First version: factorization with "=" prefix, then roots. */
    private fun decodeOldMenu(lines: List<String>): List<Pair<MenuSection, List<String>>> {
      val factorization = lines.firstOrNull { it.startsWith(OLD_FACTORIZATION_PREFIX) }
      return listOfNotNull(
        factorization?.let {
          MenuSection.FACTORIZATION to listOf(it.removePrefix(OLD_FACTORIZATION_PREFIX))
        },
        (MenuSection.ROOTS to lines.filter { it != factorization }).takeIf { it.second.isNotEmpty() },
      )
    }
  }
}

/** Removes things that can't be calculated: ≈ and integration constant. */
internal fun String.toInsertable(): String =
  removePrefix(APPROXIMATELY).removeSuffix(Token.Operator.PLUS + Token.DisplayOnly.INTEGRATION_CONSTANT)

internal const val APPROXIMATELY = "≈"
private const val LINE_SEPARATOR = "\n"
private const val SECTION_SEPARATOR = "⋯"
private const val SECTION_HEADER = "§"
private const val OLD_FACTORIZATION_PREFIX = "="
