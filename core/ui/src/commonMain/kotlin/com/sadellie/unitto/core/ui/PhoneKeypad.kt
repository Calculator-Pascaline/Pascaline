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

package com.sadellie.unitto.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key1
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key2
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key3
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key7
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key8
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key9

/** True when keypads put 1 2 3 on top like a phone, false for 7 8 9 like a calculator. */
val LocalPhoneKeypad = staticCompositionLocalOf { false }

/** This digit key, or the one in its place on a phone keypad: 7 and 1, 8 and 2, 9 and 3 swap. */
@Composable
@ReadOnlyComposable
fun KeypadButton.KeypadButtonAdd.inKeypadRow(): KeypadButton.KeypadButtonAdd {
  if (!LocalPhoneKeypad.current) return this
  return when (this) {
    Key7 -> Key1
    Key8 -> Key2
    Key9 -> Key3
    Key1 -> Key7
    Key2 -> Key8
    Key3 -> Key9
    else -> this
  }
}
