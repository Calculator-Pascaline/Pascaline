/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2025 Elshan Agaev
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

package io.github.calculatorpascaline.feature.converter.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.calculatorpascaline.core.common.Token
import io.github.calculatorpascaline.core.designsystem.LocalWindowSize
import io.github.calculatorpascaline.core.ui.KeyboardButtonToken
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.BackspaceKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.BracketsKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.ClearKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.CommaKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.DivideKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.DotKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key0
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key1
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key2
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key3
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key4
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key5
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key6
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key7
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key8
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.Key9
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.KeyA
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.KeyB
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.KeyC
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.KeyD
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.KeyE
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.KeyF
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.MinusKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.MultiplyKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.PlusKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.PowerKey
import io.github.calculatorpascaline.core.ui.KeypadButton.Companion.RootKey
import io.github.calculatorpascaline.core.ui.KeypadFlow
import io.github.calculatorpascaline.core.ui.inKeypadRow

@Composable
internal fun DefaultKeyboard(
  modifier: Modifier,
  onAddTokenClick: (String) -> Unit,
  onClearClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onBracketsClick: () -> Unit,
  fractional: String,
) {
  val iconHeight: Float =
    if (LocalWindowSize.current.heightSizeClass < WindowHeightSizeClass.Medium)
      KeyboardButtonToken.ICON_HEIGHT_SHORT
    else KeyboardButtonToken.ICON_HEIGHT_TALL

  val fractionalKey = remember(fractional) { if (fractional == Token.PERIOD) DotKey else CommaKey }
  KeypadFlow(modifier = modifier, iconHeight = iconHeight) {
    KeypadRow {
      ButtonTertiary(ClearKey, onClearClick)
      ButtonFilled(BracketsKey, onBracketsClick)
      ButtonFilled(PowerKey, onAddTokenClick)
      ButtonFilled(RootKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key7.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key8.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key9.inKeypadRow(), onAddTokenClick)
      ButtonFilled(DivideKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key4, onAddTokenClick)
      ButtonLight(Key5, onAddTokenClick)
      ButtonLight(Key6, onAddTokenClick)
      ButtonFilled(MultiplyKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key1.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key2.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key3.inKeypadRow(), onAddTokenClick)
      ButtonFilled(MinusKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key0, onAddTokenClick)
      ButtonLight(fractionalKey, onAddTokenClick)
      ButtonLight(BackspaceKey, onLongClick = onClearClick, onClick = onDeleteClick)
      ButtonFilled(PlusKey, onAddTokenClick)
    }
  }
}

@Composable
internal fun NumberBaseKeyboard(
  modifier: Modifier,
  onAddTokenClick: (String) -> Unit,
  onClearClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val iconHeight: Float =
    if (LocalWindowSize.current.heightSizeClass < WindowHeightSizeClass.Medium)
      KeyboardButtonToken.ICON_HEIGHT_SHORT
    else KeyboardButtonToken.ICON_HEIGHT_TALL

  KeypadFlow(modifier = modifier, iconHeight = iconHeight) {
    KeypadRow {
      ButtonFilled(KeyA, onAddTokenClick)
      ButtonFilled(KeyB, onAddTokenClick)
      ButtonFilled(KeyC, onAddTokenClick)
    }
    KeypadRow {
      ButtonFilled(KeyD, onAddTokenClick)
      ButtonFilled(KeyE, onAddTokenClick)
      ButtonFilled(KeyF, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key7.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key8.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key9.inKeypadRow(), onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key4, onAddTokenClick)
      ButtonLight(Key5, onAddTokenClick)
      ButtonLight(Key6, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key1.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key2.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key3.inKeypadRow(), onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key0, onAddTokenClick)
      ButtonLight(BackspaceKey, onLongClick = onClearClick, onClick = onDeleteClick, weight = 2f)
    }
  }
}

@Preview
@Composable
private fun PreviewConverterKeyboard() {
  DefaultKeyboard(
    modifier = Modifier.fillMaxSize(),
    onAddTokenClick = {},
    onClearClick = {},
    onDeleteClick = {},
    onBracketsClick = {},
    fractional = Token.PERIOD,
  )
}

@Preview
@Composable
private fun PreviewConverterKeyboardNumberBase() {
  NumberBaseKeyboard(
    modifier = Modifier.fillMaxSize(),
    onAddTokenClick = {},
    onClearClick = {},
    onDeleteClick = {},
  )
}
