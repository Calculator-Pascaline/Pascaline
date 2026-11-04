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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.LocalWindowSize
import com.sadellie.unitto.core.designsystem.icons.symbols.Delete
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.feature.calculator.components.DayTitle
import com.sadellie.unitto.feature.calculator.components.HistoryRow
import com.sadellie.unitto.feature.calculator.components.SwipeActions
import com.sadellie.unitto.feature.calculator.components.withDays
import kotlinx.datetime.TimeZone
import unitto.core.common.generated.resources.calculator_clear_history
import unitto.core.common.generated.resources.calculator_clear_history_support
import unitto.core.common.generated.resources.calculator_no_history
import unitto.core.common.generated.resources.common_cancel
import unitto.core.common.generated.resources.common_clear
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
 * binary. Tap a result to type in its base. Word size makes results wrap like in a CPU. History is
 * above on phones and next to the keypad on wide screens.
 */
@Composable
private fun ProgrammerScreen(openDrawer: () -> Unit, viewModel: ProgrammerViewModel) {
  val history = viewModel.history.collectAsStateWithLifecycleKMP().value
  var showClearDialog by remember { mutableStateOf(false) }
  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.programmer_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    actions = {
      if (history.isNotEmpty()) {
        IconButton(onClick = { showClearDialog = true }) {
          Icon(Symbols.Delete, stringResource(Res.string.calculator_clear_history))
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    val historyList =
      @Composable { modifier: Modifier ->
        ProgrammerHistoryList(
          modifier = modifier,
          history = history,
          onUseExpression = viewModel::useExpression,
          onUseResult = viewModel::useResult,
          onDelete = viewModel::deleteHistoryEntry,
          onUpdateLabel = viewModel::updateHistoryLabel,
        )
      }
    if (LocalWindowSize.current.widthSizeClass == WindowWidthSizeClass.Expanded) {
      // History and results on the left, keypad on the whole height on the right
      BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
        val keypadWidth = minOf(maxWidth * WIDE_KEYPAD_WIDTH_FACTOR, WIDE_KEYPAD_MAX_WIDTH)
        Row(
          modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            historyList(Modifier.fillMaxWidth().weight(1f))
            Controls(viewModel)
          }
          ProgrammerKeypad(modifier = Modifier.width(keypadWidth).fillMaxHeight(), viewModel = viewModel)
        }
      }
    } else {
      BoxWithConstraints(
        modifier = Modifier.padding(paddingValues).fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
      ) {
        val keypadHeight = maxHeight * KEYPAD_HEIGHT_FACTOR
        Column(
          modifier = Modifier.widthIn(max = MAX_WIDTH).fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          historyList(Modifier.fillMaxWidth().weight(1f))
          Controls(viewModel)
          ProgrammerKeypad(modifier = Modifier.fillMaxWidth().height(keypadHeight), viewModel = viewModel)
        }
      }
    }
  }
  if (showClearDialog) {
    AlertDialog(
      icon = { Icon(Symbols.Delete, null) },
      title = { Text(stringResource(Res.string.calculator_clear_history)) },
      text = { Text(stringResource(Res.string.calculator_clear_history_support)) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.clearHistory()
            showClearDialog = false
          }
        ) {
          Text(stringResource(Res.string.common_clear))
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearDialog = false }) { Text(stringResource(Res.string.common_cancel)) }
      },
      onDismissRequest = { showClearDialog = false },
    )
  }
}

/** Word size, input and the result in every base. */
@Composable
private fun Controls(viewModel: ProgrammerViewModel) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    WordSizeSelector(selected = viewModel.wordSize, onSelect = viewModel::selectWordSize)
    Input(tokens = viewModel.tokens)
    Results(
      value = viewModel.result,
      isError = viewModel.isError,
      wordSize = viewModel.wordSize,
      selected = viewModel.base,
      onSelect = viewModel::selectBase,
    )
  }
}

/**
 * Calculations with days, newest at the bottom. Tap the expression to type it again, or the result
 * to use it. Swipe left to label or delete.
 */
@Composable
private fun ProgrammerHistoryList(
  modifier: Modifier,
  history: List<ProgrammerHistoryEntry>,
  onUseExpression: (ProgrammerHistoryEntry) -> Unit,
  onUseResult: (ProgrammerHistoryEntry) -> Unit,
  onDelete: (ProgrammerHistoryEntry) -> Unit,
  onUpdateLabel: (ProgrammerHistoryEntry, String) -> Unit,
) {
  if (history.isEmpty()) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      Text(
        text = stringResource(Res.string.calculator_no_history),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    return
  }
  val state = rememberLazyListState()
  LaunchedEffect(history.first().timestamp) { state.scrollToItem(0) }
  val rows =
    remember(history) {
      history.withDays(TimeZone.currentSystemDefault(), { it.timestamp }, { it.timestamp })
    }
  LazyColumn(
    modifier = modifier,
    state = state,
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
  ) {
    items(rows, { it.key }) { row ->
      when (row) {
        is HistoryRow.Day -> DayTitle(modifier = Modifier.animateItem(), date = row.date)
        is HistoryRow.Calculation -> {
          val entry = row.item
          SwipeActions(
            modifier = Modifier.animateItem(),
            label = entry.label,
            onDelete = { onDelete(entry) },
            onUpdateLabel = { onUpdateLabel(entry, it) },
          ) {
            HistoryCard(
              entry = entry,
              onUseExpression = { onUseExpression(entry) },
              onUseResult = { onUseResult(entry) },
            )
          }
        }
      }
    }
  }
}

/** Label, base and expression, then the result in that base. */
@Composable
private fun HistoryCard(entry: ProgrammerHistoryEntry, onUseExpression: () -> Unit, onUseResult: () -> Unit) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    val label = entry.label
    if (!label.isNullOrEmpty()) {
      Text(text = label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        modifier = Modifier.padding(end = 8.dp),
        text = "${entry.base.name} · ${entry.wordSize.bits}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        modifier = Modifier.weight(1f).clickable(onClick = onUseExpression),
        text = entry.expressionText,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 2,
      )
    }
    Text(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onUseResult),
      text = entry.resultText.groupedFor(entry.base),
      style = MaterialTheme.typography.titleLarge,
      fontFamily = FontFamily.Monospace,
      color = MaterialTheme.colorScheme.primary,
      textAlign = TextAlign.End,
    )
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
  val text = tokens.toProgrammerText().ifEmpty { "0" }
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
          else -> programmerFormat(value, base, wordSize).groupedFor(base)
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

/** Hexadecimal and binary digits in groups of 4, others of 3. */
private fun String.groupedFor(base: NumberBase) =
  grouped(if (base == NumberBase.BIN || base == NumberBase.HEX) NIBBLE_GROUP else THOUSANDS_GROUP)

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

private val MAX_WIDTH = 600.dp
/** Keypad next to history and results on wide screens: this part of the width, at most the max */
private const val WIDE_KEYPAD_WIDTH_FACTOR = 0.5f
private val WIDE_KEYPAD_MAX_WIDTH = 520.dp
/** Part of the screen height for the keypad on phones, history gets what is left */
private const val KEYPAD_HEIGHT_FACTOR = 0.5f
private val INPUT_MIN_HEIGHT = 64.dp
private val CARD_SHAPE = RoundedCornerShape(20.dp)

/** Binary and hexadecimal digits by 4 (a nibble), the others by 3 */
private const val NIBBLE_GROUP = 4
private const val THOUSANDS_GROUP = 3
