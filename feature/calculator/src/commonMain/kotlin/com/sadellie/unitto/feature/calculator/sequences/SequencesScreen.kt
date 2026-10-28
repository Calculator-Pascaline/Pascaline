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

package com.sadellie.unitto.feature.calculator.sequences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.Token
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.CenterFocusWeak
import com.sadellie.unitto.core.designsystem.icons.symbols.Delete
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.core.ui.textfield.ExpressionLayoutText
import com.sadellie.unitto.core.ui.textfield.ExpressionLayoutTextField
import com.sadellie.unitto.feature.calculator.components.CalculatorKeyboard
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.functions.FUNCTION_COLORS
import com.sadellie.unitto.feature.calculator.functions.FunctionGraph
import com.sadellie.unitto.feature.calculator.functions.GraphWindow
import com.sadellie.unitto.feature.calculator.functions.PlotPoint
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.functions_reset_view
import unitto.core.common.generated.resources.sequences_add
import unitto.core.common.generated.resources.sequences_delete
import unitto.core.common.generated.resources.sequences_empty
import unitto.core.common.generated.resources.sequences_first_rank
import unitto.core.common.generated.resources.sequences_from
import unitto.core.common.generated.resources.sequences_graph
import unitto.core.common.generated.resources.sequences_hint
import unitto.core.common.generated.resources.sequences_sequences
import unitto.core.common.generated.resources.sequences_table
import unitto.core.common.generated.resources.sequences_title
import unitto.core.common.generated.resources.sequences_to

@Composable
internal fun SequencesRoute(openDrawer: () -> Unit) {
  val viewModel: SequencesViewModel = koinViewModel()
  val format = viewModel.numberFormat.collectAsStateWithLifecycleKMP().value
  val radianMode = viewModel.radianMode.collectAsStateWithLifecycleKMP().value
  val formatterSymbols = viewModel.formatterSymbols.collectAsStateWithLifecycleKMP().value
  if (format == null || formatterSymbols == null || !viewModel.isLoaded) {
    EmptyScreen()
    return
  }
  SequencesScreen(openDrawer, viewModel, format, formatterSymbols, radianMode)
}

/** Like the Sequences app of NumWorks: u, v and w, their terms on a graph and in a table. */
@Composable
private fun SequencesScreen(
  openDrawer: () -> Unit,
  viewModel: SequencesViewModel,
  format: NumberFormat,
  formatterSymbols: FormatterSymbols,
  radianMode: Boolean,
) {
  var tab by rememberSaveable { mutableIntStateOf(0) }
  // Read every field, so that terms are calculated again when one changes
  val key =
    viewModel.sequences.joinToString { "${it.type}|${it.formula.text}|${it.firstRank}|${it.firstTerm}|${it.secondTerm}" }
  val definitions = remember(key, radianMode) { viewModel.sequences.mapNotNull { it.definition(radianMode) } }
  val system = remember(definitions) { SequenceSystem(definitions) }

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.sequences_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
        listOf(Res.string.sequences_sequences, Res.string.sequences_graph, Res.string.sequences_table)
          .forEachIndexed { index, title ->
            Tab(
              selected = tab == index,
              onClick = {
                tab = index
                viewModel.select(null)
              },
              text = { Text(stringResource(title)) },
            )
          }
      }
      val content = Modifier.weight(1f).fillMaxWidth()
      when {
        tab == SEQUENCES_TAB -> SequencesTab(content, viewModel, definitions, formatterSymbols)
        definitions.none { it.formula != null } -> Message(content, stringResource(Res.string.sequences_empty))
        tab == GRAPH_TAB -> GraphTab(content, definitions, system, format)
        else -> TermsTable(content, definitions, system, format)
      }
    }
  }
}

@Composable
private fun Message(modifier: Modifier, text: String) {
  Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

/** Cards of u, v and w. Tap a formula to type it with the calculator keyboard. */
@Composable
private fun SequencesTab(
  modifier: Modifier,
  viewModel: SequencesViewModel,
  definitions: List<SequenceDefinition>,
  formatterSymbols: FormatterSymbols,
) {
  BoxWithConstraints(modifier = modifier) {
    val keyboardHeight = maxHeight * KEYBOARD_HEIGHT_FACTOR
    Column(modifier = Modifier.fillMaxSize()) {
      LazyColumn(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(CARD_SPACING),
        verticalArrangement = Arrangement.spacedBy(CARD_SPACING),
      ) {
        item {
          Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(Res.string.sequences_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        itemsIndexed(viewModel.sequences, key = { _, item -> item.name }) { index, item ->
          val definition = definitions.firstOrNull { it.name == item.name }
          SequenceCard(
            item = item,
            color = sequenceColor(item.name),
            isSelected = viewModel.selected == index,
            isValid = definition == null || definition.formula != null,
            formatterSymbols = formatterSymbols,
            onFormulaClick = { viewModel.select(index) },
            onDelete = { viewModel.delete(index) },
          )
        }
        if (viewModel.sequences.size < Token.Sequence.names.size) {
          item {
            TextButton(onClick = viewModel::add) {
              Icon(Symbols.Add, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(stringResource(Res.string.sequences_add))
            }
          }
        }
      }
      if (viewModel.selected == null) return@Column
      CalculatorKeyboard(
        modifier = Modifier.height(keyboardHeight).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        onAddTokenClick = viewModel::addTokens,
        onBracketsClick = viewModel::addBracket,
        onDeleteClick = viewModel::deleteTokens,
        onClearClick = viewModel::clearInput,
        onEqualClick = { viewModel.select(null) },
        radianMode = true,
        onRadianModeClick = {},
        additionalButtons = false,
        onAdditionalButtonsClick = {},
        inverseMode = false,
        onInverseModeClick = {},
        showAcButton = true,
        middleZero = false,
        fractional = formatterSymbols.fractional,
        // x,n,t key types n
        isInSeries = true,
        // Terms of the sequences first, no integrals: formulas of sequences can't have them
        firstFunctionKeys =
          viewModel.sequences.flatMap { listOf(Token.Sequence.term(it.name, 0), Token.Sequence.term(it.name, 1)) }.map { it to it },
        showCalculus = false,
      )
    }
  }
}

/** Kind of sequence, formula, first rank and first terms. */
@Composable
private fun SequenceCard(
  item: SequenceItem,
  color: Color,
  isSelected: Boolean,
  isValid: Boolean,
  formatterSymbols: FormatterSymbols,
  onFormulaClick: () -> Unit,
  onDelete: () -> Unit,
) {
  val background =
    if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer
  val border = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = BORDER_ALPHA) else Color.Transparent
  val name = item.name
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(CARD_SHAPE)
        .background(background)
        .border(CARD_BORDER, border, CARD_SHAPE)
        .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Row(
        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        SequenceType.entries.forEach { type ->
          FilterChip(
            selected = item.type == type,
            onClick = { item.type = type },
            label = { Text(type.left(name)) },
          )
        }
      }
      IconButton(onClick = onDelete) {
        Icon(Symbols.Delete, stringResource(Res.string.sequences_delete))
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth().clickable(onClick = onFormulaClick),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "${item.type.left(name)} =",
        style = MaterialTheme.typography.titleLarge,
        color = color,
        fontWeight = FontWeight.Medium,
      )
      val textColor = if (isValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
      Box(modifier = Modifier.weight(1f).height(FORMULA_HEIGHT).padding(start = 8.dp), contentAlignment = Alignment.CenterStart) {
        if (isSelected) {
          ExpressionLayoutTextField(
            modifier = Modifier.fillMaxSize(),
            state = item.formula,
            formatterSymbols = formatterSymbols,
            textColor = textColor,
            minRatio = FORMULA_MIN_RATIO,
            alignStart = true,
            keyboardOptions = KeyboardOptions.Default,
            textStyle = MaterialTheme.typography.titleLarge,
          )
        } else {
          ExpressionLayoutText(
            modifier = Modifier.padding(start = 4.dp),
            value = item.formula.text.toString(),
            formatterSymbols = formatterSymbols,
            style = MaterialTheme.typography.titleLarge,
            textColor = textColor,
            minScale = FORMULA_MIN_RATIO,
          )
        }
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      NumberField(Modifier.weight(1f), item.firstRank, { item.firstRank = it }, stringResource(Res.string.sequences_first_rank))
      if (item.type != SequenceType.EXPLICIT) {
        NumberField(Modifier.weight(1f), item.firstTerm, { item.firstTerm = it }, "$name(n₀)")
      }
      if (item.type == SequenceType.DOUBLE_RECURSIVE) {
        NumberField(Modifier.weight(1f), item.secondTerm, { item.secondTerm = it }, "$name(n₀+1)")
      }
    }
  }
}

/** Terms as points, from one rank to another. */
@Composable
private fun GraphTab(
  modifier: Modifier,
  definitions: List<SequenceDefinition>,
  system: SequenceSystem,
  format: NumberFormat,
) {
  val firstRank = definitions.minOf { it.firstRank }
  var fromText by rememberSaveable { mutableStateOf("") }
  var toText by rememberSaveable { mutableStateOf("") }
  val from = fromText.toNumberOrNull()?.toInt() ?: firstRank
  val to = (toText.toNumberOrNull()?.toInt() ?: (from + DEFAULT_TERMS)).coerceIn(from, from + MAX_GRAPH_TERMS)
  var window by remember { mutableStateOf<GraphWindow?>(null) }
  val points =
    remember(system, from, to) {
      definitions.flatMap { definition ->
        val color = sequenceColor(definition.name)
        (from..to).mapNotNull { n ->
          system.term(definition.name, n).takeIf { it.isFinite() }?.let { PlotPoint(n.toDouble(), it, color) }
        }
      }
    }

  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      NumberField(Modifier.weight(1f), fromText, { fromText = it }, stringResource(Res.string.sequences_from), from.toString())
      NumberField(Modifier.weight(1f), toText, { toText = it }, stringResource(Res.string.sequences_to), to.toString())
    }
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      FunctionGraph(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        functions = emptyList(),
        window = window,
        onWindowChange = { window = it },
        formatValue = { format.format(it) },
        points = points,
      )
      if (window != null) {
        SmallFloatingActionButton(
          modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
          onClick = { window = null },
        ) {
          Icon(Symbols.CenterFocusWeak, stringResource(Res.string.functions_reset_view))
        }
      }
    }
  }
}

/** n and the terms of every sequence, from the smallest first rank. */
@Composable
private fun TermsTable(
  modifier: Modifier,
  definitions: List<SequenceDefinition>,
  system: SequenceSystem,
  format: NumberFormat,
) {
  val firstRank = definitions.minOf { it.firstRank }
  BoxWithConstraints(modifier = modifier) {
    val columns = definitions.size + 1
    val columnWidth = max(MIN_COLUMN_WIDTH, maxWidth / columns)
    Box(modifier = Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
      Column(modifier = Modifier.width(columnWidth * columns).fillMaxSize()) {
        TableRow(
          cells = listOf("n" to MaterialTheme.colorScheme.onSurface) +
            definitions.map { "${it.name}(n)" to sequenceColor(it.name) },
          columnWidth = columnWidth,
          isHeader = true,
        )
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          items(TABLE_ROWS) { row ->
            val n = firstRank + row
            TableRow(
              cells = listOf(n.toString() to MaterialTheme.colorScheme.onSurface) +
                definitions.map { format.format(system.term(it.name, n)) to MaterialTheme.colorScheme.onSurface },
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

@Composable
private fun TableRow(cells: List<Pair<String, Color>>, columnWidth: androidx.compose.ui.unit.Dp, isHeader: Boolean) {
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
private fun NumberField(
  modifier: Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String? = null,
) {
  OutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, maxLines = 1) },
    placeholder = placeholder?.let { { Text(it) } },
    singleLine = true,
    isError = value.isNotBlank() && value.toNumberOrNull() == null,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

/** Same color for a sequence wherever it is shown. */
private fun sequenceColor(name: String): Color =
  FUNCTION_COLORS[Token.Sequence.names.indexOf(name).coerceAtLeast(0) % FUNCTION_COLORS.size]

/** Left side of the formula: `u(n)`, `u(n+1)` or `u(n+2)`. */
private fun SequenceType.left(name: String): String =
  when (this) {
    SequenceType.EXPLICIT -> "$name(n)"
    SequenceType.RECURSIVE -> "$name(n+1)"
    SequenceType.DOUBLE_RECURSIVE -> "$name(n+2)"
  }

private const val SEQUENCES_TAB = 0
private const val GRAPH_TAB = 1
private const val TABLE_ROWS = 101
private const val DEFAULT_TERMS = 20
private const val MAX_GRAPH_TERMS = 1000
private const val KEYBOARD_HEIGHT_FACTOR = 0.55f
private const val FORMULA_MIN_RATIO = 0.5f
private const val BORDER_ALPHA = 0.5f
private val FORMULA_HEIGHT = 48.dp
private val CARD_SPACING = 8.dp
private val CARD_SHAPE = RoundedCornerShape(20.dp)
private val CARD_BORDER = 1.5.dp
private val MIN_COLUMN_WIDTH = 110.dp
