/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package com.sadellie.unitto.feature.calculator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.designsystem.LocalWindowSize
import com.sadellie.unitto.core.ui.ColumnWithConstraints
import com.sadellie.unitto.core.ui.KEYPAD_SPACING_FRACTION
import com.sadellie.unitto.core.ui.KeyboardButtonToken
import com.sadellie.unitto.core.ui.KeypadButton.Companion.BackspaceKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.BracketsKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.ClearKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.CommaKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.DotKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.EqualKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.FactorialKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.FractionKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key0
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key1
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key2
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key3
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key4
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key5
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key6
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key7
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key8
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Key9
import com.sadellie.unitto.core.ui.KeypadButton.Companion.MinusKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.MultiplyKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.PlusKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.PowerKey
import com.sadellie.unitto.core.ui.KeypadFlow
import com.sadellie.unitto.core.ui.inKeypadRow

/**
 * Function keys that scroll and a keypad that never changes: above each other when there is height
 * for it, side by side in landscape on a phone, where rows are short.
 */
@Composable
internal fun CalculatorKeyboard(
  modifier: Modifier,
  onAddTokenClick: (String) -> Unit,
  onBracketsClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onClearClick: () -> Unit,
  onEqualClick: () -> Unit,
  fractional: String,
  isInSeries: Boolean = false,
  firstFunctionKeys: List<Pair<String, String>> = emptyList(),
  showCalculus: Boolean = true,
) {
  var showUnits by remember { mutableStateOf(false) }
  val keypad: @Composable (Modifier, Float) -> Unit = { keypadModifier, iconHeight ->
    BottomGrid(
      modifier = keypadModifier,
      iconHeight = iconHeight,
      onAddTokenClick = onAddTokenClick,
      onBracketsClick = onBracketsClick,
      onDeleteClick = onDeleteClick,
      onClearClick = onClearClick,
      onEqualClick = onEqualClick,
      onUnitsClick = { showUnits = true },
      isInSeries = isInSeries,
      fractional = fractional,
    )
  }
  if (LocalWindowSize.current.heightSizeClass >= WindowHeightSizeClass.Medium) {
    ColumnWithConstraints(modifier = modifier) { constraints ->
      // Function rows are lower than digit rows: digits get most of the height
      val rowHeight =
        (constraints.maxHeight - DOTS_HEIGHT) / (BOTTOM_ROWS + PAGE_ROWS * FUNCTION_ROW_SCALE)
      FunctionRows(
        modifier = Modifier.fillMaxWidth(),
        rowHeight = rowHeight * FUNCTION_ROW_SCALE,
        onAddTokenClick = onAddTokenClick,
        // Same gaps as the keypad under it
        spacing = constraints.maxWidth * KEYPAD_SPACING_FRACTION,
        rowGap = rowHeight * BOTTOM_ROWS * KEYPAD_SPACING_FRACTION,
        firstKeys = firstFunctionKeys,
        showCalculus = showCalculus,
      )
      keypad(Modifier.fillMaxWidth().weight(1f), KeyboardButtonToken.ICON_HEIGHT_TALL)
    }
  } else {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(LANDSCAPE_PANE_GAP)) {
      BoxWithConstraints(modifier = Modifier.weight(FUNCTIONS_PANE_WEIGHT).fillMaxHeight()) {
        FunctionRows(
          modifier = Modifier.fillMaxWidth(),
          rowHeight = (maxHeight - DOTS_HEIGHT) / PAGE_ROWS,
          onAddTokenClick = onAddTokenClick,
          firstKeys = firstFunctionKeys,
          showCalculus = showCalculus,
        )
      }
      keypad(Modifier.weight(KEYPAD_PANE_WEIGHT).fillMaxHeight(), KeyboardButtonToken.ICON_HEIGHT_SHORT)
    }
  }
  if (showUnits) {
    UnitsSheet(
      onDismiss = { showUnits = false },
      onUnitClick = {
        onAddTokenClick(it)
        showUnits = false
      },
    )
  }
}

/**
 * Keys that are always shown. Variable key types n inside of Σ and ∏, x everywhere else.
 */
@Composable
private fun BottomGrid(
  modifier: Modifier,
  iconHeight: Float,
  onAddTokenClick: (String) -> Unit,
  onBracketsClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onClearClick: () -> Unit,
  onEqualClick: () -> Unit,
  onUnitsClick: () -> Unit,
  isInSeries: Boolean,
  fractional: String,
) {
  val fractionalKey = remember(fractional) { if (fractional == Token.PERIOD) DotKey else CommaKey }
  KeypadFlow(modifier = modifier, iconHeight = iconHeight) {
    KeypadRow {
      ButtonText("unit", onUnitsClick)
      ButtonText(Token.Const.I, { onAddTokenClick(Token.Const.I) })
      ButtonText(
        "x,n,t",
        { onAddTokenClick(if (isInSeries) Token.Calculus.N else Token.Calculus.X) },
      )
      ButtonTertiary(ClearKey, onClearClick)
      ButtonFilled(BackspaceKey, onDeleteClick)
    }
    KeypadRow {
      ButtonLight(Key7.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key8.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key9.inKeypadRow(), onAddTokenClick)
      ButtonFilled(PowerKey, onAddTokenClick)
      ButtonFilled(BracketsKey, onBracketsClick)
    }
    KeypadRow {
      ButtonLight(Key4, onAddTokenClick)
      ButtonLight(Key5, onAddTokenClick)
      ButtonLight(Key6, onAddTokenClick)
      ButtonFilled(MultiplyKey, onAddTokenClick)
      ButtonFilled(FractionKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key1.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key2.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key3.inKeypadRow(), onAddTokenClick)
      ButtonFilled(PlusKey, onAddTokenClick)
      ButtonFilled(MinusKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key0, onAddTokenClick)
      ButtonLight(fractionalKey, onAddTokenClick)
      ButtonFilled(FactorialKey, onAddTokenClick)
      ButtonFilledPrimary(EqualKey, onEqualClick, weight = 2f)
    }
  }
}

/** Function keys height compared to digits */
private const val BOTTOM_ROWS = 5
/** Pages of function keys are 1.6 digit rows high */
private const val FUNCTION_ROW_SCALE = 0.4f
private const val FUNCTIONS_PANE_WEIGHT = 2f
private const val KEYPAD_PANE_WEIGHT = 3f
private val LANDSCAPE_PANE_GAP = 12.dp
