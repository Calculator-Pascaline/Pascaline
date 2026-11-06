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

package com.sadellie.unitto.feature.calculator.functions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.common.toFormattedString
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.CenterFocusWeak
import com.sadellie.unitto.core.designsystem.icons.symbols.Close
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.core.ui.textfield.ExpressionLayoutText
import com.sadellie.unitto.core.ui.textfield.ExpressionLayoutTextField
import com.sadellie.unitto.core.ui.textfield.formatExpression
import com.sadellie.unitto.core.ui.textfield.isInSeries
import com.sadellie.unitto.feature.calculator.components.CalculatorKeyboard
import io.github.sadellie.evaluatto.GraphFunction
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.functions_add
import unitto.core.common.generated.resources.functions_delete
import unitto.core.common.generated.resources.functions_empty
import unitto.core.common.generated.resources.functions_graph
import unitto.core.common.generated.resources.functions_reset_view
import unitto.core.common.generated.resources.functions_show
import unitto.core.common.generated.resources.functions_table
import unitto.core.common.generated.resources.functions_table_start
import unitto.core.common.generated.resources.functions_table_step
import unitto.core.common.generated.resources.functions_title
import unitto.core.common.generated.resources.functions_undefined

@Composable
internal fun FunctionsRoute(openDrawer: () -> Unit) {
  val viewModel: FunctionsViewModel = koinViewModel()
  when (val uiState = viewModel.uiState.collectAsStateWithLifecycleKMP().value) {
    FunctionsUIState.Loading -> EmptyScreen()
    is FunctionsUIState.Ready -> FunctionsScreen(uiState, openDrawer, viewModel)
  }
}

/**
 * One screen: the graph, or the table of values, with the functions under it. The graph follows
 * what is typed. On wide screens the functions are at the side.
 */
@Composable
private fun FunctionsScreen(
  uiState: FunctionsUIState.Ready,
  openDrawer: () -> Unit,
  viewModel: FunctionsViewModel,
) {
  var showsTable by rememberSaveable { mutableStateOf(false) }
  var window by remember { mutableStateOf<GraphWindow?>(null) }
  val undefined = stringResource(Res.string.functions_undefined)
  val formatValue = { value: Double ->
    formatValue(value, uiState.precision, uiState.outputFormat, uiState.formatterSymbols, undefined)
  }

  // Parsed again only when an expression changes
  val inputs = uiState.functions.map { it.input.text.toString() }
  val parsed =
    remember(inputs, uiState.radianMode) {
      inputs.map { input ->
        if (input.isBlank()) null
        else runCatching { GraphFunction(input, uiState.radianMode) }.getOrNull()
      }
    }
  val plots =
    uiState.functions.mapIndexedNotNull { index, item ->
      val function = parsed.getOrNull(index) ?: return@mapIndexedNotNull null
      if (item.visible) PlotFunction(item.name, FUNCTION_COLORS[item.color], function) else null
    }

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.functions_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    actions = {
      TextButton(onClick = { showsTable = !showsTable }) {
        Text(stringResource(if (showsTable) Res.string.functions_graph else Res.string.functions_table))
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    BoxWithConstraints(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      val picture: @Composable (Modifier) -> Unit = { modifier ->
        if (showsTable) {
          ValuesTable(modifier = modifier, plots = plots, formatValue = formatValue)
        } else {
          GraphPane(modifier, plots, window, { window = it }, formatValue)
        }
      }
      val isValid = { index: Int -> uiState.functions[index].input.text.isBlank() || parsed.getOrNull(index) != null }
      val keyboardHeight = maxHeight * KEYBOARD_HEIGHT_FACTOR
      val keyboard: @Composable () -> Unit = {
        FunctionsKeyboard(Modifier.height(keyboardHeight).fillMaxWidth(), uiState, viewModel)
      }
      if (maxWidth >= WIDE_LAYOUT_WIDTH) {
        Row(modifier = Modifier.fillMaxSize()) {
          Column(modifier = Modifier.width(PANEL_WIDTH).fillMaxHeight()) {
            FunctionList(Modifier.weight(1f).fillMaxWidth(), uiState, isValid, viewModel)
            keyboard()
          }
          VerticalDivider()
          picture(Modifier.weight(1f).fillMaxHeight())
        }
      } else {
        val listHeight = maxHeight * if (uiState.selected != null) LIST_HEIGHT_EDITING else LIST_HEIGHT
        Column(modifier = Modifier.fillMaxSize()) {
          picture(Modifier.weight(1f).fillMaxWidth())
          HorizontalDivider()
          FunctionList(
            Modifier.heightIn(max = listHeight).fillMaxWidth(),
            uiState,
            isValid,
            viewModel,
          )
          keyboard()
        }
      }
    }
  }
}

/** The graph with what goes over it. */
@Composable
private fun GraphPane(
  modifier: Modifier,
  plots: List<PlotFunction>,
  window: GraphWindow?,
  onWindowChange: (GraphWindow?) -> Unit,
  formatValue: (Double) -> String,
) {
  Box(modifier = modifier) {
    FunctionGraph(
      modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
      functions = plots,
      window = window,
      onWindowChange = onWindowChange,
      formatValue = formatValue,
    )
    if (plots.isEmpty()) {
      Text(
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        text = stringResource(Res.string.functions_empty),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
    if (window != null) {
      SmallFloatingActionButton(
        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
        onClick = { onWindowChange(null) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
      ) {
        Icon(Symbols.CenterFocusWeak, stringResource(Res.string.functions_reset_view))
      }
    }
  }
}

/** Flat rows of functions, the one being edited stays in view. Tap one to edit it. */
@Composable
private fun FunctionList(
  modifier: Modifier,
  uiState: FunctionsUIState.Ready,
  isValid: (Int) -> Boolean,
  viewModel: FunctionsViewModel,
) {
  val listState = rememberLazyListState()
  LaunchedEffect(uiState.selected) { uiState.selected?.let { listState.animateScrollToItem(it) } }
  LazyColumn(modifier = modifier, state = listState) {
    itemsIndexed(uiState.functions, key = { _, item -> item.name }) { index, item ->
      FunctionRow(
        item = item,
        isSelected = uiState.selected == index,
        isValid = isValid(index),
        formatterSymbols = uiState.formatterSymbols,
        onClick = { viewModel.select(index) },
        onVisibleChange = { viewModel.setVisible(index, it) },
        onDelete = { viewModel.delete(index) },
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    if (uiState.functions.size < FUNCTION_NAMES.size) {
      item {
        TextButton(modifier = Modifier.padding(horizontal = 8.dp), onClick = viewModel::add) {
          Icon(Symbols.Add, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(stringResource(Res.string.functions_add))
        }
      }
    }
  }
}

/** Calculator keyboard while a function is being edited, its equal key finishes. */
@Composable
private fun FunctionsKeyboard(modifier: Modifier, uiState: FunctionsUIState.Ready, viewModel: FunctionsViewModel) {
  val selected = uiState.selected?.let { uiState.functions.getOrNull(it) } ?: return
  CalculatorKeyboard(
    modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    onAddTokenClick = viewModel::addTokens,
    onBracketsClick = viewModel::addBracket,
    onDeleteClick = viewModel::deleteTokens,
    onClearClick = viewModel::clearInput,
    onEqualClick = viewModel::done,
    fractional = uiState.formatterSymbols.fractional,
    isInSeries = isInSeries(selected.input),
  )
}

/** `● f(x)=x²  ×`. The dot shows or hides the curve, the expression is editable when [isSelected]. */
@Composable
private fun FunctionRow(
  item: GraphFunctionItem,
  isSelected: Boolean,
  isValid: Boolean,
  formatterSymbols: FormatterSymbols,
  onClick: () -> Unit,
  onVisibleChange: (Boolean) -> Unit,
  onDelete: () -> Unit,
) {
  val color = FUNCTION_COLORS[item.color]
  val background = if (isSelected) color.copy(alpha = SELECTED_TINT) else Color.Transparent
  val textColor = if (isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
  Row(
    modifier = Modifier.fillMaxWidth().background(background).clickable(onClick = onClick).padding(end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val showLabel = stringResource(Res.string.functions_show)
    Box(
      modifier =
        Modifier.size(ROW_HEIGHT)
          .toggleable(value = item.visible, role = Role.Checkbox, onValueChange = onVisibleChange)
          .semantics { contentDescription = showLabel },
      contentAlignment = Alignment.Center,
    ) {
      Box(
        Modifier.size(DOT_SIZE)
          .clip(CircleShape)
          .background(if (item.visible) color else Color.Transparent)
          .border(DOT_BORDER, color, CircleShape)
      )
    }
    Text(
      modifier = Modifier.padding(end = 4.dp),
      text = "${item.name}(x)=",
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Same text size while editing and not, aligned to the start
    Box(modifier = Modifier.weight(1f).height(ROW_HEIGHT), contentAlignment = Alignment.CenterStart) {
      if (isSelected) {
        ExpressionLayoutTextField(
          modifier = Modifier.fillMaxSize(),
          state = item.input,
          formatterSymbols = formatterSymbols,
          textColor = textColor,
          minRatio = INPUT_MIN_RATIO,
          alignStart = true,
          keyboardOptions = KeyboardOptions.Default,
          textStyle = MaterialTheme.typography.titleLarge,
        )
      } else {
        ExpressionLayoutText(
          modifier = Modifier.padding(start = TEXT_START_PADDING),
          value = item.input.text.toString(),
          formatterSymbols = formatterSymbols,
          style = MaterialTheme.typography.titleLarge,
          textColor = textColor,
          minScale = INPUT_MIN_RATIO,
        )
      }
    }
    IconButton(onClick = onDelete) {
      Icon(
        Symbols.Close,
        stringResource(Res.string.functions_delete),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** x from [start] with [step], and values of every visible function. */
@Composable
private fun ValuesTable(modifier: Modifier, plots: List<PlotFunction>, formatValue: (Double) -> String) {
  var start by rememberSaveable { mutableStateOf("0") }
  var step by rememberSaveable { mutableStateOf("1") }
  val startValue = start.toNumberOrNull() ?: 0.0
  val stepValue = step.toNumberOrNull()?.takeIf { it != 0.0 } ?: 1.0

  Column(modifier = modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      NumberField(Modifier.weight(1f), start, { start = it }, stringResource(Res.string.functions_table_start))
      NumberField(Modifier.weight(1f), step, { step = it }, stringResource(Res.string.functions_table_step))
    }
    HorizontalDivider()
    BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
      val columns = plots.size + 1
      val columnWidth = max(MIN_COLUMN_WIDTH, maxWidth / columns)
      Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        Column(modifier = Modifier.width(columnWidth * columns).fillMaxSize()) {
          TableRow(
            cells = listOf("x" to MaterialTheme.colorScheme.onSurface) +
              plots.map { "${it.name}(x)" to it.color },
            columnWidth = columnWidth,
            isHeader = true,
          )
          HorizontalDivider()
          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(TABLE_ROWS) { row ->
              val x = startValue + row * stepValue
              TableRow(
                cells = listOf(formatValue(x) to MaterialTheme.colorScheme.onSurface) +
                  plots.map { formatValue(it.valueAt(x)) to MaterialTheme.colorScheme.onSurface },
                columnWidth = columnWidth,
                isHeader = false,
              )
              HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TableRow(cells: List<Pair<String, Color>>, columnWidth: Dp, isHeader: Boolean) {
  Row(
    modifier =
      Modifier.background(if (isHeader) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
  ) {
    cells.forEach { (text, color) ->
      Text(
        modifier = Modifier.width(columnWidth).padding(horizontal = 8.dp, vertical = 12.dp),
        text = text,
        color = color,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
    }
  }
}

@Composable
private fun NumberField(modifier: Modifier, value: String, onValueChange: (String) -> Unit, label: String) {
  OutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

/** Accepts −, comma and dot: `−2,5` ==> −2.5. */
private fun String.toNumberOrNull(): Double? =
  replace(Token.Operator.MINUS, "-").replace(",", ".").trim().toDoubleOrNull()

/** Number as the calculator shows it, `undef` when it is not defined. */
private fun formatValue(
  value: Double,
  precision: Int,
  outputFormat: Int,
  formatterSymbols: FormatterSymbols,
  undefined: String,
): String {
  if (!value.isFinite()) return undefined
  return KBigDecimal.valueOf(value)
    .toFormattedString(precision, outputFormat)
    .replace("-", Token.Operator.MINUS)
    .formatExpression(formatterSymbols)
}

@Composable
private fun isInSeries(input: TextFieldState): Boolean {
  val inSeries by remember(input) { derivedStateOf { input.text.isInSeries(input.selection.max) } }
  return inSeries
}

private const val TABLE_ROWS = 101
private const val KEYBOARD_HEIGHT_FACTOR = 0.5f
private const val LIST_HEIGHT = 0.4f
private const val LIST_HEIGHT_EDITING = 0.16f
private const val SELECTED_TINT = 0.08f
private val WIDE_LAYOUT_WIDTH = 720.dp
private val PANEL_WIDTH = 360.dp
private val DOT_SIZE = 18.dp
private val DOT_BORDER = 2.dp
private const val INPUT_MIN_RATIO = 0.5f
private val ROW_HEIGHT = 56.dp
// Same as the caret margin of the editable field
private val TEXT_START_PADDING = 4.dp
private val MIN_COLUMN_WIDTH = 110.dp
