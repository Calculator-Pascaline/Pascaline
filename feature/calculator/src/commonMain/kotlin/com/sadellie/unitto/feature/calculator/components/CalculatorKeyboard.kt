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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.designsystem.ExpressivePreview
import com.sadellie.unitto.core.designsystem.LocalWindowSize
import com.sadellie.unitto.core.designsystem.icons.symbols.KeyboardArrowUp
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.ColumnWithConstraints
import com.sadellie.unitto.core.ui.KeyboardButtonToken
import com.sadellie.unitto.core.ui.KeypadButton
import com.sadellie.unitto.core.ui.KeypadButton.Companion.AcTanKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.AngleDegKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.AngleRadKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.ArCosKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.ArSinKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.BackspaceKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.BracketsKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.ClearKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.CommaKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.CosKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.DotKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.EqualKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.EulerKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.ExKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.FactorialKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.FractionKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.InvKey
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
import com.sadellie.unitto.core.ui.KeypadButton.Companion.KeyX
import com.sadellie.unitto.core.ui.KeypadButton.Companion.LeftBracketKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.LnKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.LogKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.MinusKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.ModuloKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.MultiplyKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.PercentKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.PiKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.PlusKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.Power10Key
import com.sadellie.unitto.core.ui.KeypadButton.Companion.PowerKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.RadicalKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.RightBracketKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.SinKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.TanKey
import com.sadellie.unitto.core.ui.KeypadFlow
import com.sadellie.unitto.core.ui.KEYPAD_SPACING_FRACTION
import com.sadellie.unitto.core.ui.inKeypadRow

@Composable
internal fun CalculatorKeyboard(
  modifier: Modifier,
  onAddTokenClick: (String) -> Unit,
  onBracketsClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onClearClick: () -> Unit,
  onEqualClick: () -> Unit,
  radianMode: Boolean,
  onRadianModeClick: (Boolean) -> Unit,
  additionalButtons: Boolean,
  onAdditionalButtonsClick: (Boolean) -> Unit,
  inverseMode: Boolean,
  onInverseModeClick: (Boolean) -> Unit,
  showAcButton: Boolean,
  middleZero: Boolean,
  fractional: String,
  isInSeries: Boolean = false,
  firstFunctionKeys: List<Pair<String, String>> = emptyList(),
  showCalculus: Boolean = true,
) {
  // Portrait: few function rows that scroll, digits never change
  if (LocalWindowSize.current.heightSizeClass >= WindowHeightSizeClass.Medium) {
    var showUnits by remember { mutableStateOf(false) }
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
      BottomGrid(
        modifier = Modifier.fillMaxWidth().weight(1f),
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
    if (showUnits) {
      UnitsSheet(
        onDismiss = { showUnits = false },
        onUnitClick = {
          onAddTokenClick(it)
          showUnits = false
        },
      )
    }
    return
  }

  val pagerState = rememberPagerState(pageCount = { KEYBOARD_PAGES })

  Column(modifier = modifier) {
    HorizontalPager(
      state = pagerState,
      modifier = Modifier.weight(1f).fillMaxWidth(),
      userScrollEnabled = true,
    ) { page ->
      when (page) {
        0 -> {
          if (LocalWindowSize.current.heightSizeClass < WindowHeightSizeClass.Medium) {
            CompactKeyboard(
              modifier = Modifier.fillMaxSize(),
              onAddTokenClick = onAddTokenClick,
              onBracketsClick = onBracketsClick,
              onDeleteClick = onDeleteClick,
              onClearClick = onClearClick,
              onEqualClick = onEqualClick,
              radianMode = radianMode,
              onRadianModeClick = onRadianModeClick,
              inverseMode = inverseMode,
              onInverseModeClick = onInverseModeClick,
              showAcButton = showAcButton,
              middleZero = middleZero,
              fractional = fractional,
            )
          } else {
            ExpandedKeyboard(
              modifier = Modifier.fillMaxSize(),
              onAddTokenClick = onAddTokenClick,
              onBracketsClick = onBracketsClick,
              onDeleteClick = onDeleteClick,
              onClearClick = onClearClick,
              onEqualClick = onEqualClick,
              radianMode = radianMode,
              onRadianModeClick = onRadianModeClick,
              additionalButtons = additionalButtons,
              onAdditionalButtonsClick = onAdditionalButtonsClick,
              inverseMode = inverseMode,
              onInverseModeClick = onInverseModeClick,
              showAcButton = showAcButton,
              middleZero = middleZero,
              fractional = fractional,
            )
          }
        }
        1 -> {
          CalculusKeyboard(
            modifier = Modifier.fillMaxSize(),
            onAddTokenClick = onAddTokenClick,
          )
        }
        2 -> {
          TrigonometryKeyboard(
            modifier = Modifier.fillMaxSize(),
            onAddTokenClick = onAddTokenClick,
          )
        }
      }
    }

    PageDots(
      modifier = Modifier.fillMaxWidth().height(DOTS_HEIGHT).padding(bottom = 2.dp),
      pagerState = pagerState,
    )
  }
}

/** Integrals (indefinite without bounds), derivatives, sums and products. */
@Composable
private fun CalculusKeyboard(modifier: Modifier, onAddTokenClick: (String) -> Unit) {
  ButtonGrid(
    modifier = modifier,
    buttons =
      listOf(
        listOf("∫ □ dx" to Token.Layout.INTEGRAL, "d/dx(□)" to Token.Layout.DERIVATIVE),
        listOf("Σ □" to Token.Layout.SUM, "∏ □" to Token.Layout.PRODUCT),
        listOf(
          Token.Calculus.N to Token.Calculus.N,
          Token.Calculus.X to Token.Calculus.X,
          Token.Const.INFINITY to Token.Const.INFINITY,
        ),
      ),
    onClick = onAddTokenClick,
  )
}

/** All trigonometric and hyperbolic functions with their inverses. */
@Composable
private fun TrigonometryKeyboard(modifier: Modifier, onAddTokenClick: (String) -> Unit) {
  fun key(token: String) = token to "$token("
  ButtonGrid(
    modifier = modifier,
    buttons =
      listOf(
        listOf(key(Token.Func.SIN), key(Token.Func.COS), key(Token.Func.TAN)),
        listOf(key(Token.Func.ARSIN), key(Token.Func.ARCOS), key(Token.Func.ACTAN)),
        listOf(key(Token.Func.SINH), key(Token.Func.COSH), key(Token.Func.TANH)),
        listOf(key(Token.Func.ARSINH), key(Token.Func.ARCOSH), key(Token.Func.ARTANH)),
      ),
    onClick = onAddTokenClick,
  )
}

/** Rows of text keys, pairs are label and token. */
@Composable
private fun ButtonGrid(
  modifier: Modifier,
  buttons: List<List<Pair<String, String>>>,
  onClick: (String) -> Unit,
) {
  KeypadFlow(modifier = modifier.padding(8.dp)) {
    buttons.forEach { row ->
      KeypadRow { row.forEach { (label, token) -> ButtonText(label, { onClick(token) }) } }
    }
  }
}

/**
 * Keys that are always shown in portrait. Variable key types n inside of Σ and ∏, x everywhere
 * else.
 */
@Composable
private fun BottomGrid(
  modifier: Modifier,
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
  KeypadFlow(modifier = modifier, iconHeight = KeyboardButtonToken.ICON_HEIGHT_TALL) {
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

/** Digits, basic operators, AC, delete and equal. Never changes. */
@Composable
private fun DigitsGrid(
  modifier: Modifier,
  onAddTokenClick: (String) -> Unit,
  onBracketsClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onClearClick: () -> Unit,
  onEqualClick: () -> Unit,
  showAcButton: Boolean,
  middleZero: Boolean,
  fractional: String,
) {
  val fractionalKey = remember(fractional) { if (fractional == Token.PERIOD) DotKey else CommaKey }
  KeypadFlow(modifier = modifier, iconHeight = KeyboardButtonToken.ICON_HEIGHT_TALL) {
    KeypadRow {
      if (showAcButton) {
        ButtonTertiary(ClearKey, onClearClick)
        ButtonFilled(BracketsKey, onBracketsClick)
      } else {
        ButtonFilled(LeftBracketKey, onAddTokenClick)
        ButtonFilled(RightBracketKey, onAddTokenClick)
      }
      ButtonFilled(PercentKey, onAddTokenClick)
      ButtonFilled(FractionKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key7.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key8.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key9.inKeypadRow(), onAddTokenClick)
      ButtonFilled(MultiplyKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key4, onAddTokenClick)
      ButtonLight(Key5, onAddTokenClick)
      ButtonLight(Key6, onAddTokenClick)
      ButtonFilled(MinusKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonLight(Key1.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key2.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key3.inKeypadRow(), onAddTokenClick)
      ButtonFilled(PlusKey, onAddTokenClick)
    }
    KeypadRow {
      if (middleZero) {
        ButtonLight(fractionalKey, onAddTokenClick)
        ButtonLight(Key0, onAddTokenClick)
      } else {
        ButtonLight(Key0, onAddTokenClick)
        ButtonLight(fractionalKey, onAddTokenClick)
      }
      ButtonLight(BackspaceKey, onLongClick = onClearClick, onClick = onDeleteClick)
      ButtonFilledPrimary(EqualKey, onEqualClick)
    }
  }
}

@Composable
private fun ExpandedKeyboard(
  modifier: Modifier,
  onAddTokenClick: (String) -> Unit,
  onBracketsClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onClearClick: () -> Unit,
  onEqualClick: () -> Unit,
  radianMode: Boolean,
  onRadianModeClick: (Boolean) -> Unit,
  additionalButtons: Boolean,
  onAdditionalButtonsClick: (Boolean) -> Unit,
  inverseMode: Boolean,
  onInverseModeClick: (Boolean) -> Unit,
  showAcButton: Boolean,
  middleZero: Boolean,
  fractional: String,
) {
  ColumnWithConstraints(modifier = modifier) { constraints ->
    val spacerHeight = constraints.maxHeight * SPACER_HEIGHT_FACTOR
    val additionalButtonHeight = constraints.maxHeight * EXPANDED_ADDITIONAL_BUTTON_HEIGHT_FACTOR

    Spacer(modifier = Modifier.height(spacerHeight))

    ExpandedAdditionalKeyboard(
      expanded = additionalButtons,
      inverseMode = inverseMode,
      radianMode = radianMode,
      onRadianModeClick = onRadianModeClick,
      onInverseModeClick = onInverseModeClick,
      additionalButtonHeight = additionalButtonHeight,
      onAddTokenClick = onAddTokenClick,
      onAdditionalButtonsClick = onAdditionalButtonsClick,
    )

    Spacer(modifier = Modifier.height(spacerHeight))

    DigitsGrid(
      modifier = Modifier.weight(1f).fillMaxSize(),
      onAddTokenClick = onAddTokenClick,
      onBracketsClick = onBracketsClick,
      onDeleteClick = onDeleteClick,
      onClearClick = onClearClick,
      onEqualClick = onEqualClick,
      showAcButton = showAcButton,
      middleZero = middleZero,
      fractional = fractional,
    )

    Spacer(modifier = Modifier.height(spacerHeight))
  }
}

@Composable
private fun ExpandedAdditionalKeyboard(
  expanded: Boolean,
  inverseMode: Boolean,
  radianMode: Boolean,
  onRadianModeClick: (Boolean) -> Unit,
  onInverseModeClick: (Boolean) -> Unit,
  additionalButtonHeight: Dp,
  onAddTokenClick: (String) -> Unit,
  onAdditionalButtonsClick: (Boolean) -> Unit,
) {
  Crossfade(
    targetState = inverseMode,
    modifier = Modifier.animateContentSize(),
    label = "Additional keyboard state changes",
  ) { isInverse ->
    val iconHeight = KeyboardButtonToken.ICON_HEIGHT_TALL_SECONDARY
    val rows = if (expanded) EXPANDED_ADDITIONAL_ROWS_COUNT else 1
    val angleKey = if (radianMode) AngleRadKey else AngleDegKey

    Row {
      Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        // First row has an extra column: both power and x
        KeypadFlow(
          modifier = Modifier.height(additionalButtonHeight).fillMaxWidth(),
          iconHeight = iconHeight,
        ) {
          KeypadRow {
            ButtonTransparent(if (isInverse) ModuloKey else RadicalKey, onAddTokenClick)
            ButtonTransparent(PiKey, onAddTokenClick)
            ButtonTransparent(PowerKey, onAddTokenClick)
            ButtonTransparent(KeyX, onAddTokenClick)
            ButtonTransparent(FactorialKey, onAddTokenClick)
          }
        }

        if (expanded) {
          KeypadFlow(
            modifier = Modifier.height(additionalButtonHeight * (rows - 1)).fillMaxWidth(),
            iconHeight = iconHeight,
          ) {
            KeypadRow {
              ButtonTransparent(angleKey, { onRadianModeClick(!radianMode) })
              ButtonTransparent(if (isInverse) ArSinKey else SinKey, onAddTokenClick)
              ButtonTransparent(if (isInverse) ArCosKey else CosKey, onAddTokenClick)
              ButtonTransparent(if (isInverse) AcTanKey else TanKey, onAddTokenClick)
            }
            KeypadRow {
              ButtonTransparent(InvKey, { onInverseModeClick(!inverseMode) })
              ButtonTransparent(EulerKey, onAddTokenClick)
              ButtonTransparent(if (isInverse) ExKey else LnKey, onAddTokenClick)
              ButtonTransparent(if (isInverse) Power10Key else LogKey, onAddTokenClick)
            }
          }
        }
      }

      ToggleExpandedAdditionalKeysButton(
        modifier = Modifier.size(additionalButtonHeight),
        expanded = expanded,
        onClick = onAdditionalButtonsClick,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ToggleExpandedAdditionalKeysButton(
  modifier: Modifier,
  expanded: Boolean,
  onClick: (Boolean) -> Unit,
) {
  val expandRotation: Float by
  animateFloatAsState(
    targetValue = if (expanded) 0f else 180f,
    animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
    label = "Rotate on expand",
  )
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    IconButton(
      onClick = { onClick(!expanded) },
      colors =
        IconButtonDefaults.iconButtonColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
      shapes = IconButtonDefaults.shapes(),
      modifier =
        Modifier.size(
          IconButtonDefaults.smallContainerSize(IconButtonDefaults.IconButtonWidthOption.Uniform)
        ),
    ) {
      Icon(
        imageVector = Symbols.KeyboardArrowUp,
        contentDescription = null,
        modifier = Modifier.rotate(expandRotation).size(IconButtonDefaults.mediumIconSize),
      )
    }
  }
}

@Composable
private fun CompactKeyboard(
  modifier: Modifier,
  onAddTokenClick: (String) -> Unit,
  onBracketsClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onClearClick: () -> Unit,
  onEqualClick: () -> Unit,
  radianMode: Boolean,
  onRadianModeClick: (Boolean) -> Unit,
  inverseMode: Boolean,
  onInverseModeClick: (Boolean) -> Unit,
  showAcButton: Boolean,
  middleZero: Boolean,
  fractional: String,
) {
  Crossfade(targetState = inverseMode, label = "Inverse switch", modifier = modifier) { inverse ->
    CompactKeyboardGrid(
      modifier = Modifier.fillMaxSize(),
      inverse = inverse,
      onAddTokenClick = onAddTokenClick,
      onBracketsClick = onBracketsClick,
      onDeleteClick = onDeleteClick,
      onClearClick = onClearClick,
      onEqualClick = onEqualClick,
      radianMode = radianMode,
      onRadianModeClick = onRadianModeClick,
      inverseMode = inverseMode,
      onInverseModeClick = onInverseModeClick,
      showAcButton = showAcButton,
      middleZero = middleZero,
      fractional = fractional,
      angleKey = if (radianMode) AngleRadKey else AngleDegKey,
    )
  }
}

/** Landscape: functions on the left, digits on the right. Inverse functions with [inverse]. */
@Composable
private fun CompactKeyboardGrid(
  modifier: Modifier,
  inverse: Boolean,
  onAddTokenClick: (String) -> Unit,
  onBracketsClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onClearClick: () -> Unit,
  onEqualClick: () -> Unit,
  radianMode: Boolean,
  onRadianModeClick: (Boolean) -> Unit,
  inverseMode: Boolean,
  onInverseModeClick: (Boolean) -> Unit,
  showAcButton: Boolean,
  middleZero: Boolean,
  fractional: String,
  angleKey: KeypadButton.KeypadButtonSimple,
) {
  val secondary = KeyboardButtonToken.ICON_HEIGHT_SHORT_SECONDARY
  val fractionalKey = if (fractional == Token.PERIOD) DotKey else CommaKey
  KeypadFlow(modifier = modifier, iconHeight = KeyboardButtonToken.ICON_HEIGHT_SHORT) {
    KeypadRow {
      ButtonTransparent(angleKey, { onRadianModeClick(!radianMode) }, height = secondary)
      ButtonTransparent(KeyX, onAddTokenClick, height = secondary)
      ButtonTransparent(PiKey, onAddTokenClick, height = secondary)
      ButtonLight(Key7.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key8.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key9.inKeypadRow(), onAddTokenClick)
      if (showAcButton) {
        ButtonTertiary(ClearKey, onClearClick)
        ButtonFilled(BracketsKey, onBracketsClick)
      } else {
        ButtonFilled(LeftBracketKey, onAddTokenClick)
        ButtonFilled(RightBracketKey, onAddTokenClick)
      }
    }
    KeypadRow {
      ButtonTransparent(InvKey, { onInverseModeClick(!inverseMode) }, height = secondary)
      ButtonTransparent(PowerKey, onAddTokenClick, height = secondary)
      ButtonTransparent(FactorialKey, onAddTokenClick, height = secondary)
      ButtonLight(Key4, onAddTokenClick)
      ButtonLight(Key5, onAddTokenClick)
      ButtonLight(Key6, onAddTokenClick)
      ButtonFilled(MultiplyKey, onAddTokenClick)
      ButtonFilled(FractionKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonTransparent(if (inverse) ArSinKey else SinKey, onAddTokenClick, height = secondary)
      ButtonTransparent(if (inverse) ArCosKey else CosKey, onAddTokenClick, height = secondary)
      ButtonTransparent(if (inverse) AcTanKey else TanKey, onAddTokenClick, height = secondary)
      ButtonLight(Key1.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key2.inKeypadRow(), onAddTokenClick)
      ButtonLight(Key3.inKeypadRow(), onAddTokenClick)
      ButtonFilled(MinusKey, onAddTokenClick)
      ButtonFilled(PercentKey, onAddTokenClick)
    }
    KeypadRow {
      ButtonTransparent(EulerKey, onAddTokenClick, height = secondary)
      ButtonTransparent(if (inverse) ExKey else LnKey, onAddTokenClick, height = secondary)
      ButtonTransparent(if (inverse) Power10Key else LogKey, onAddTokenClick, height = secondary)
      if (middleZero) {
        ButtonLight(fractionalKey, onAddTokenClick)
        ButtonLight(Key0, onAddTokenClick)
      } else {
        ButtonLight(Key0, onAddTokenClick)
        ButtonLight(fractionalKey, onAddTokenClick)
      }
      ButtonLight(BackspaceKey, onLongClick = onClearClick, onClick = onDeleteClick)
      ButtonFilled(PlusKey, onAddTokenClick)
      ButtonFilledPrimary(EqualKey, onEqualClick)
    }
  }
}

private const val SPACER_HEIGHT_FACTOR = 0.025f
private const val KEYBOARD_PAGES = 3
/** Function keys height compared to digits */
private const val BOTTOM_ROWS = 5
private const val BOTTOM_COLUMNS = 5
/** Pages of function keys are 1.6 digit rows high */
private const val FUNCTION_ROW_SCALE = 0.4f
private val GRID_SPACING = 8.dp
private const val EXPANDED_ADDITIONAL_BUTTON_HEIGHT_FACTOR = 0.09f
private const val EXPANDED_ADDITIONAL_ROWS_COUNT = 3

@Preview(
  device = "spec:width=400dp,height=600dp,dpi=440",
  showSystemUi = false,
  showBackground = false,
)
@Composable
private fun PreviewExpandedKeyboard() = ExpressivePreview {
  var additionalButtons by remember { mutableStateOf(false) }
  ExpandedKeyboard(
    modifier = Modifier.fillMaxHeight(),
    onAddTokenClick = {},
    onBracketsClick = {},
    onDeleteClick = {},
    onClearClick = {},
    onEqualClick = {},
    radianMode = true,
    onRadianModeClick = {},
    additionalButtons = additionalButtons,
    onAdditionalButtonsClick = { additionalButtons = it },
    inverseMode = false,
    onInverseModeClick = {},
    showAcButton = true,
    middleZero = false,
    fractional = Token.PERIOD,
  )
}

@Preview(
  device = "spec:width=600dp,height=300dp,dpi=440",
  showSystemUi = false,
  showBackground = false,
)
@Composable
private fun PreviewCompactKeyboard() = ExpressivePreview {
  var inverseMode by remember { mutableStateOf(false) }
  CompactKeyboard(
    modifier = Modifier.fillMaxHeight(),
    onAddTokenClick = {},
    onBracketsClick = {},
    onDeleteClick = {},
    onClearClick = {},
    onEqualClick = {},
    radianMode = true,
    onRadianModeClick = {},
    inverseMode = inverseMode,
    onInverseModeClick = { inverseMode = it },
    showAcButton = true,
    middleZero = false,
    fractional = Token.PERIOD,
  )
}
