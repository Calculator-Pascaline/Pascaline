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

package com.sadellie.unitto.feature.calculator.regression

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.icons.symbols.CenterFocusWeak
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.datatable.DataTable
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.functions.FunctionGraph
import com.sadellie.unitto.feature.calculator.functions.GraphWindow
import com.sadellie.unitto.feature.calculator.functions.PlotFunction
import com.sadellie.unitto.feature.calculator.functions.PlotPoint
import com.sadellie.unitto.feature.calculator.statistics.StatLine
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.functions_reset_view
import unitto.core.common.generated.resources.regression_correlation
import unitto.core.common.generated.resources.regression_count
import unitto.core.common.generated.resources.regression_covariance
import unitto.core.common.generated.resources.regression_cubic
import unitto.core.common.generated.resources.regression_curve
import unitto.core.common.generated.resources.regression_data
import unitto.core.common.generated.resources.regression_determination
import unitto.core.common.generated.resources.regression_deviation_x
import unitto.core.common.generated.resources.regression_deviation_y
import unitto.core.common.generated.resources.regression_empty
import unitto.core.common.generated.resources.regression_exponential
import unitto.core.common.generated.resources.regression_graph
import unitto.core.common.generated.resources.regression_linear
import unitto.core.common.generated.resources.regression_logarithmic
import unitto.core.common.generated.resources.regression_mean_x
import unitto.core.common.generated.resources.regression_mean_y
import unitto.core.common.generated.resources.regression_not_possible
import unitto.core.common.generated.resources.regression_power
import unitto.core.common.generated.resources.regression_proportional
import unitto.core.common.generated.resources.regression_quadratic
import unitto.core.common.generated.resources.regression_quartic
import unitto.core.common.generated.resources.regression_stats
import unitto.core.common.generated.resources.regression_title

@Composable
internal fun RegressionRoute(openDrawer: () -> Unit) {
  val viewModel: RegressionViewModel = koinViewModel()
  val format = viewModel.numberFormat.collectAsStateWithLifecycleKMP().value
  val rows = viewModel.table.rows
  if (format == null || rows == null) {
    EmptyScreen()
    return
  }
  RegressionScreen(openDrawer, viewModel, rows, format)
}

/** Like the Regression app of NumWorks: points, their graph with a curve, the curve and statistics. */
@Composable
private fun RegressionScreen(
  openDrawer: () -> Unit,
  viewModel: RegressionViewModel,
  rows: List<List<String>>,
  format: NumberFormat,
) {
  var tab by rememberSaveable { mutableIntStateOf(0) }
  val points = remember(rows) { rows.dataPoints() }
  val model = viewModel.model
  val result = remember(points, model) { regression(points, model) }

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.regression_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      PrimaryTabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.surface) {
        TABS.forEachIndexed { index, title ->
          Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(title)) })
        }
      }
      val content = Modifier.weight(1f).fillMaxWidth()
      when {
        tab == DATA_TAB ->
          DataTable(
            modifier = content,
            headers = listOf("x", "y"),
            rows = rows,
            onCellChange = viewModel.table::setCell,
            onDeleteRow = viewModel.table::deleteRow,
            onClear = viewModel.table::clear,
          )
        points.size < MIN_POINTS -> Message(content, stringResource(Res.string.regression_empty))
        tab == GRAPH_TAB -> GraphTab(content, points, model, result, viewModel::selectModel, format)
        tab == CURVE_TAB -> CurveTab(content, model, result, viewModel::selectModel, format)
        else -> StatsTab(content, points, format)
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

/** Points and the curve, with the kinds of curve on top. */
@Composable
private fun GraphTab(
  modifier: Modifier,
  points: List<DataPoint>,
  model: RegressionModel,
  result: RegressionResult?,
  onModelClick: (RegressionModel) -> Unit,
  format: NumberFormat,
) {
  var window by remember { mutableStateOf<GraphWindow?>(null) }
  val pointColor = MaterialTheme.colorScheme.primary
  val curveColor = MaterialTheme.colorScheme.tertiary
  val plotPoints = remember(points, pointColor) { points.map { PlotPoint(it.x, it.y, pointColor) } }
  val curves = listOfNotNull(result?.let { PlotFunction("y", curveColor, it::valueAt) })

  Column(modifier = modifier) {
    ModelChips(model, onModelClick)
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      FunctionGraph(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
        functions = curves,
        window = window,
        onWindowChange = { window = it },
        formatValue = { format.format(it) },
        points = plotPoints,
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
    Text(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      text = if (result == null) stringResource(Res.string.regression_not_possible) else equation(result, format),
      style = MaterialTheme.typography.titleMedium,
      color = if (result == null) MaterialTheme.colorScheme.error else curveColor,
      textAlign = TextAlign.Center,
    )
  }
}

/** Formula, coefficients, r and r² of the chosen kind of curve. */
@Composable
private fun CurveTab(
  modifier: Modifier,
  model: RegressionModel,
  result: RegressionResult?,
  onModelClick: (RegressionModel) -> Unit,
  format: NumberFormat,
) {
  Column(modifier = modifier) {
    ModelChips(model, onModelClick)
    Text(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      text = model.formula,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.Medium,
      textAlign = TextAlign.Center,
    )
    HorizontalDivider()
    if (result == null) {
      Message(Modifier.fillMaxWidth(), stringResource(Res.string.regression_not_possible))
      return@Column
    }
    val lines =
      model.coefficients.zip(result.coefficients).map { (name, value) -> name to format.format(value) } +
        listOfNotNull(
          result.correlation?.let { stringResource(Res.string.regression_correlation) to format.format(it) },
          result.determination?.let { stringResource(Res.string.regression_determination) to format.format(it) },
        )
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
      items(lines) { (label, value) ->
        StatLine(label, value)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }
    }
  }
}

@Composable
private fun StatsTab(modifier: Modifier, points: List<DataPoint>, format: NumberFormat) {
  val statistics = remember(points) { pointStatistics(points) } ?: return
  val lines =
    listOf(
      Res.string.regression_count to statistics.count.toDouble(),
      Res.string.regression_mean_x to statistics.meanX,
      Res.string.regression_mean_y to statistics.meanY,
      Res.string.regression_deviation_x to statistics.deviationX,
      Res.string.regression_deviation_y to statistics.deviationY,
      Res.string.regression_covariance to statistics.covariance,
      Res.string.regression_correlation to statistics.correlation,
    )
  LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
    items(lines) { (label, value) ->
      StatLine(stringResource(label), format.format(value))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@Composable
private fun ModelChips(model: RegressionModel, onModelClick: (RegressionModel) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    RegressionModel.entries.forEach { entry ->
      FilterChip(
        selected = entry == model,
        onClick = { onModelClick(entry) },
        label = { Text(stringResource(entry.title)) },
      )
    }
  }
}

/** `y = 2·x + 1` with the values of the coefficients. */
private fun equation(result: RegressionResult, format: NumberFormat): String {
  val values = result.coefficients.map { format.format(it) }
  val c = result.model.coefficients.zip(values).toMap()
  return when (result.model) {
    RegressionModel.LINEAR -> "y = ${c["a"]}·x + ${c["b"]}"
    RegressionModel.PROPORTIONAL -> "y = ${c["a"]}·x"
    RegressionModel.QUADRATIC -> "y = ${c["a"]}·x² + ${c["b"]}·x + ${c["c"]}"
    RegressionModel.CUBIC -> "y = ${c["a"]}·x³ + ${c["b"]}·x² + ${c["c"]}·x + ${c["d"]}"
    RegressionModel.QUARTIC -> "y = ${c["a"]}·x⁴ + ${c["b"]}·x³ + ${c["c"]}·x² + ${c["d"]}·x + ${c["e"]}"
    RegressionModel.LOGARITHMIC -> "y = ${c["a"]}·ln(x) + ${c["b"]}"
    RegressionModel.EXPONENTIAL -> "y = ${c["a"]}·e^(${c["b"]}·x)"
    RegressionModel.POWER -> "y = ${c["a"]}·x^${c["b"]}"
  }.replace("+ −", "− ")
}

private val RegressionModel.title: StringResource
  get() =
    when (this) {
      RegressionModel.LINEAR -> Res.string.regression_linear
      RegressionModel.PROPORTIONAL -> Res.string.regression_proportional
      RegressionModel.QUADRATIC -> Res.string.regression_quadratic
      RegressionModel.CUBIC -> Res.string.regression_cubic
      RegressionModel.QUARTIC -> Res.string.regression_quartic
      RegressionModel.LOGARITHMIC -> Res.string.regression_logarithmic
      RegressionModel.EXPONENTIAL -> Res.string.regression_exponential
      RegressionModel.POWER -> Res.string.regression_power
    }

private val TABS =
  listOf(Res.string.regression_data, Res.string.regression_graph, Res.string.regression_curve, Res.string.regression_stats)
private const val DATA_TAB = 0
private const val GRAPH_TAB = 1
private const val CURVE_TAB = 2
private const val MIN_POINTS = 2
