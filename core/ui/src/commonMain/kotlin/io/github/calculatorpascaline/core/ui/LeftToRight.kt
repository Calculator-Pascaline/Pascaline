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

package io.github.calculatorpascaline.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Laid out from left to right even in Arabic: keypads keep 7 8 9 and = where calculators have
 * them, like math is written. Text inside still reads in its own direction.
 */
@Composable
fun LeftToRight(content: @Composable () -> Unit) {
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

/**
 * Text that keeps its left to right order inside a right to left line: binary groups, 8 bit and
 * expressions stay "100 1110" in Arabic instead of becoming "1110 100". Where the text goes on the
 * line does not change.
 */
fun String.leftToRight(): String = "\u2066$this\u2069"
