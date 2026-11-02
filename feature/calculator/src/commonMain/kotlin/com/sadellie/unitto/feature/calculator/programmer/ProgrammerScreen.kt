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

package com.sadellie.unitto.feature.calculator.programmer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.KeyboardButtonToken
import com.sadellie.unitto.core.ui.KeypadButton
import com.sadellie.unitto.core.ui.KeypadButton.Companion.BackspaceKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.ClearKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.DivideKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.EqualKey
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
import com.sadellie.unitto.core.ui.KeypadButton.Companion.KeyA
import com.sadellie.unitto.core.ui.KeypadButton.Companion.KeyB
import com.sadellie.unitto.core.ui.KeypadButton.Companion.KeyC
import com.sadellie.unitto.core.ui.KeypadButton.Companion.KeyD
import com.sadellie.unitto.core.ui.KeypadButton.Companion.KeyE
import com.sadellie.unitto.core.ui.KeypadButton.Companion.KeyF
import com.sadellie.unitto.core.ui.KeypadButton.Companion.LeftBracketKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.MinusKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.MultiplyKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.PlusKey
import com.sadellie.unitto.core.ui.KeypadButton.Companion.RightBracketKey
import com.sadellie.unitto.core.ui.KeypadFlow
import com.sadellie.unitto.core.ui.KeypadFlowScope
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import io.github.sadellie.evaluatto.programmer.NumberBase
import io.github.sadellie.evaluatto.programmer.ProgrammerOperator
import io.github.sadellie.evaluatto.programmer.WordSize
import io.github.sadellie.evaluatto.programmer.programmerFormat
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.common_error
import unitto.core.common.generated.resources.programmer_title

@Composable
internal fun ProgrammerRoute(openDrawer: () -> Unit) {
  val viewModel: ProgrammerViewModel = koinViewModel()
  ProgrammerScreen(openDrawer, viewModel)
}

/**
 * Integer calculator for programmers: input in one base, result in hexadecimal, decimal, octal and
 * binary. Tap a result to type in its base. Word size makes results wrap like in a CPU.
 */
@Composable
private fun ProgrammerScreen(openDrawer: () -> Unit, viewModel: ProgrammerViewModel) {
  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.programmer_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    Box(modifier = Modifier.padding(paddingValues).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
      Column(
        modifier = Modifier.widthIn(max = MAX_WIDTH).fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        WordSizeSelector(selected = viewModel.wordSize, onSelect = viewModel::selectWordSize)
        Input(tokens = viewModel.tokens)
        Results(
          value = viewModel.result,
          isError = viewModel.isError,
          wordSize = viewModel.wordSize,
          selected = viewModel.base,
          onSelect = viewModel::selectBase,
        )
        ProgrammerKeypad(modifier = Modifier.fillMaxWidth().weight(1f), viewModel = viewModel)
      }
    }
  }
}

@Composable
private fun WordSizeSelector(selected: WordSize, onSelect: (WordSize) -> Unit) {
  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    WordSize.entries.forEachIndexed { index, size ->
      SegmentedButton(
        selected = size == selected,
        onClick = { onSelect(size) },
        shape = SegmentedButtonDefaults.itemShape(index, WordSize.entries.size),
        label = { Text("${size.bits} bit") },
      )
    }
  }
}

/** Typed expression, words are spaced: 12 AND FF. */
@Composable
private fun Input(tokens: List<String>) {
  val text =
    tokens
      .joinToString("") {
        when (it) {
          in SPACED_OPERATORS -> " $it "
          ProgrammerOperator.NOT -> "$it "
          else -> it
        }
      }
      .replace("  ", " ").trim().ifEmpty { "0" }
  Text(
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = INPUT_MIN_HEIGHT)
        .clip(CARD_SHAPE)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .padding(horizontal = 16.dp, vertical = 8.dp),
    text = text,
    style = MaterialTheme.typography.headlineMedium,
    textAlign = TextAlign.End,
    maxLines = 2,
  )
}

/** Result in every base, the selected one is the base of the input. */
@Composable
private fun Results(
  value: Long?,
  isError: Boolean,
  wordSize: WordSize,
  selected: NumberBase,
  onSelect: (NumberBase) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainer)) {
    NumberBase.entries.forEach { base ->
      val isSelected = base == selected
      val text =
        when {
          isError -> stringResource(Res.string.common_error)
          value == null -> ""
          base == NumberBase.BIN || base == NumberBase.HEX -> programmerFormat(value, base, wordSize).grouped(4)
          else -> programmerFormat(value, base, wordSize).grouped(3)
        }
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clickable { onSelect(base) }
            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          modifier = Modifier.widthIn(min = 48.dp),
          text = base.name,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(
          modifier = Modifier.weight(1f),
          text = text,
          style = MaterialTheme.typography.bodyLarge,
          fontFamily = FontFamily.Monospace,
          textAlign = TextAlign.End,
          color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

/** Digits from the right in groups: 1111 0000, 65 535. */
private fun String.grouped(size: Int): String {
  val sign = if (startsWith(Token.Operator.MINUS)) Token.Operator.MINUS else ""
  val digits = removePrefix(Token.Operator.MINUS)
  return sign + digits.reversed().chunked(size).joinToString(" ").reversed()
}

/**
 * Hexadecimal letters and bitwise operators on top, digits and arithmetic below. Digits that are
 * not in the base are disabled.
 */
@Composable
private fun ProgrammerKeypad(modifier: Modifier, viewModel: ProgrammerViewModel) {
  val digits = viewModel.digits
  KeypadFlow(modifier = modifier, iconHeight = KeyboardButtonToken.ICON_HEIGHT_TALL) {
    KeypadRow {
      Digit(this, KeyA, digits, viewModel)
      Digit(this, KeyB, digits, viewModel)
      Digit(this, KeyC, digits, viewModel)
      ButtonText(ProgrammerOperator.AND, { viewModel.addOperator(ProgrammerOperator.AND) })
      ButtonText(ProgrammerOperator.OR, { viewModel.addOperator(ProgrammerOperator.OR) })
      ButtonText(ProgrammerOperator.NAND, { viewModel.addOperator(ProgrammerOperator.NAND) })
    }
    KeypadRow {
      Digit(this, KeyD, digits, viewModel)
      Digit(this, KeyE, digits, viewModel)
      Digit(this, KeyF, digits, viewModel)
      ButtonText(ProgrammerOperator.XOR, { viewModel.addOperator(ProgrammerOperator.XOR) })
      ButtonText(ProgrammerOperator.NOR, { viewModel.addOperator(ProgrammerOperator.NOR) })
      ButtonText(ProgrammerOperator.NOT, viewModel::addNot)
    }
    KeypadRow {
      ButtonText(ProgrammerOperator.SHIFT_LEFT, { viewModel.addOperator(ProgrammerOperator.SHIFT_LEFT) })
      ButtonText(ProgrammerOperator.SHIFT_RIGHT, { viewModel.addOperator(ProgrammerOperator.SHIFT_RIGHT) })
      ButtonText(ProgrammerOperator.ROTATE_LEFT, { viewModel.addOperator(ProgrammerOperator.ROTATE_LEFT) })
      ButtonText(ProgrammerOperator.ROTATE_RIGHT, { viewModel.addOperator(ProgrammerOperator.ROTATE_RIGHT) })
      ButtonText(ProgrammerOperator.MOD, { viewModel.addOperator(ProgrammerOperator.MOD) })
    }
    KeypadRow {
      Digit(this, Key7, digits, viewModel)
      Digit(this, Key8, digits, viewModel)
      Digit(this, Key9, digits, viewModel)
      ButtonFilled(LeftBracketKey, { viewModel.addLeftBracket() })
      ButtonFilled(RightBracketKey, { viewModel.addRightBracket() })
    }
    KeypadRow {
      Digit(this, Key4, digits, viewModel)
      Digit(this, Key5, digits, viewModel)
      Digit(this, Key6, digits, viewModel)
      ButtonFilled(MultiplyKey, viewModel::addOperator)
      ButtonFilled(DivideKey, viewModel::addOperator)
    }
    KeypadRow {
      Digit(this, Key1, digits, viewModel)
      Digit(this, Key2, digits, viewModel)
      Digit(this, Key3, digits, viewModel)
      ButtonFilled(PlusKey, viewModel::addOperator)
      ButtonFilled(MinusKey, viewModel::addOperator)
    }
    KeypadRow {
      ButtonTertiary(ClearKey, viewModel::clear)
      Digit(this, Key0, digits, viewModel)
      ButtonFilled(BackspaceKey, viewModel::delete)
      ButtonFilledPrimary(EqualKey, viewModel::equal, weight = 2f)
    }
  }
}

/** Digit key, disabled when the digit is not in the base: 2 in binary. */
@Composable
private fun KeypadFlowScope.Digit(
  row: RowScope,
  key: KeypadButton.KeypadButtonAdd,
  digits: List<String>,
  viewModel: ProgrammerViewModel,
) = with(row) { ButtonLight(key, viewModel::addDigit, enabled = key.token in digits) }

/** Operators drawn with spaces around them in the input. */
private val SPACED_OPERATORS =
  listOf(
    ProgrammerOperator.AND,
    ProgrammerOperator.OR,
    ProgrammerOperator.XOR,
    ProgrammerOperator.NAND,
    ProgrammerOperator.NOR,
    ProgrammerOperator.MOD,
    ProgrammerOperator.SHIFT_LEFT,
    ProgrammerOperator.SHIFT_RIGHT,
    ProgrammerOperator.ROTATE_LEFT,
    ProgrammerOperator.ROTATE_RIGHT,
  )

private val MAX_WIDTH = 600.dp
private val INPUT_MIN_HEIGHT = 64.dp
private val CARD_SHAPE = RoundedCornerShape(20.dp)
