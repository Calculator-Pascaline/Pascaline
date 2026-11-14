/*
 * Unitto is a calculator for Android
 * Copyright (c) 2025-2026 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
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

package io.github.calculatorpascaline.core.common

/**
 * Formatter symbols. Always use [Token].
 *
 * @property grouping Symbol for thousands separator.
 * @property fractional Symbol decimal separator.
 * @property indian controls group sizes
 * - `true` - 1,23,45,678
 * - `false` - 12,345,678
 */
data class FormatterSymbols(val grouping: String, val fractional: String, val indian: Boolean)

/**
 * True when the grouping symbol can go with either decimal symbol: space and apostrophe. Period and
 * comma decide the decimal symbol themselves.
 */
val FormatterSymbols.canChooseFractional: Boolean
  get() = grouping == Token.SPACE || grouping == Token.APOSTROPHE

/** These symbols with [fractional] as decimal symbol, the grouping symbol stays when it can. */
fun FormatterSymbols.withFractional(fractional: String): FormatterSymbols =
  copy(grouping = if (canChooseFractional) grouping else Token.SPACE, fractional = fractional)
