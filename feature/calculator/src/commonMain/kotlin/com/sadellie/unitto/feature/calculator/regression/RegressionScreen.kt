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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.datatable.DataTable
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsColumns
import com.sadellie.unitto.feature.calculator.physics.number
import com.sadellie.unitto.feature.calculator.probability.rounded
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import com.sadellie.unitto.feature.calculator.statistics.StatLine
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.regression_best
import unitto.core.common.generated.resources.regression_best_note
import unitto.core.common.generated.resources.regression_best_title
import unitto.core.common.generated.resources.regression_correlation
import unitto.core.common.generated.resources.regression_count
import unitto.core.common.generated.resources.regression_covariance
import unitto.core.common.generated.resources.regression_cubic
import unitto.core.common.generated.resources.regression_curve
import unitto.core.common.generated.resources.regression_data
import unitto.core.common.generated.resources.regression_deviation_x
import unitto.core.common.generated.resources.regression_deviation_y
import unitto.core.common.generated.resources.regression_example
import unitto.core.common.generated.resources.regression_explained
import unitto.core.common.generated.resources.regression_exponential
import unitto.core.common.generated.resources.regression_hint
import unitto.core.common.generated.resources.regression_linear
import unitto.core.common.generated.resources.regression_logarithmic
import unitto.core.common.generated.resources.regression_mean_x
import unitto.core.common.generated.resources.regression_mean_y
import unitto.core.common.generated.resources.regression_not_possible
import unitto.core.common.generated.resources.regression_power
import unitto.core.common.generated.resources.regression_predict
import unitto.core.common.generated.resources.regression_predict_at
import unitto.core.common.generated.resources.regression_proportional
import unitto.core.common.generated.resources.regression_quadratic
import unitto.core.common.generated.resources.regression_quartic
import unitto.core.common.generated.resources.regression_stats
import unitto.core.common.generated.resources.regression_title
import unitto.core.common.generated.resources.regression_typical_gap

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

/** Points that can be moved with a finger, the curve through them and how it compares to the others. */
@Composable
private fun RegressionScreen(
  openDrawer: () -> Unit,
  viewModel: RegressionViewModel,
  rows: List<List<String>>,
  format: NumberFormat,
) {
  var tab by rememberSaveable { mutableIntStateOf(CURVE_TAB) }
  val points = remember(rows) { rows.dataPoints() }

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
      when (tab) {
        DATA_TAB ->
          DataTable(
            modifier = content,
            headers = listOf("x", "y"),
            rows = rows,
            onCellChange = viewModel.table::setCell,
            onDeleteRow = viewModel.table::deleteRow,
            onClear = viewModel.table::clear,
          )
        CURVE_TAB -> CurveTab(content, viewModel, rows, points, format)
        else -> StatsTab(content, points, format)
      }
    }
  }
}

/** Decimals that are enough for a point on a chart this wide. */
private fun Double.chartValue(span: Double) = rounded((CHART_DECIMALS - floor(log10(span))).toInt().coerceIn(0, MAX_DECIMALS))

/** Where the first forecast is: a little after the last point. */
private fun firstForecast(points: List<DataPoint>): String {
  val low = points.minOfOrNull { it.x } ?: 0.0
  val high = points.maxOfOrNull { it.x } ?: 0.0
  return ceil(high + (high - low) * FORECAST_SHARE).rounded(0)
}

private fun RegressionResult.pointAt(x: Double) = valueAt(x).takeIf { it.isFinite() }?.let { DataPoint(x, it) }

private fun RegressionViewModel.actions(rows: List<List<String>>, window: FitWindow, moved: (Pair<Int, DataPoint>?) -> Unit, last: () -> Pair<Int, DataPoint>?) =
  ChartActions(
    onMove = { index, point -> moved(index to point) },
    onDrop = {
      last()?.let { (index, point) ->
        setPoint(rows.pointRows()[index], point.x.chartValue(window.width), point.y.chartValue(window.height))
      }
      moved(null)
    },
    onAdd = { addPoint(it.x.chartValue(window.width), it.y.chartValue(window.height)) },
    onRemove = { table.deleteRow(rows.pointRows()[it]) },
  )

@Composable
private fun CurveTab(
  modifier: Modifier,
  viewModel: RegressionViewModel,
  rows: List<List<String>>,
  points: List<DataPoint>,
  format: NumberFormat,
) {
  // A moved point is saved when it is dropped, and the chart stays still until then
  var moved by remember { mutableStateOf<Pair<Int, DataPoint>?>(null) }
  var forecast by rememberSaveable { mutableStateOf<String?>(null) }
  val model = viewModel.model
  val shown = moved?.let { (index, point) -> points.mapIndexed { i, old -> if (i == index) point else old } } ?: points
  val fits = remember(shown) { rankedFits(shown) }
  val result = fits.firstOrNull { it.result.model == model }?.result
  val forecastText = forecast ?: firstForecast(points)
  val forecastAt = forecastText.number()
  val prediction = result?.pointAt(forecastAt)
  val saved = remember(points, model) { regression(points, model) }
  val window = remember(points, saved, forecastAt) { fitWindow(points + listOfNotNull(saved?.pointAt(forecastAt))) }
  val actions = viewModel.actions(rows, window, { moved = it }, { moved })

  PhysicsColumns(
    modifier = modifier,
    live = {
      LiveCard {
        FitChart(shown, result, window, prediction, actions) { format.format(it) }
        Hint(stringResource(Res.string.regression_hint))
        when {
          points.size < MIN_POINTS ->
            FilledTonalButton(onClick = viewModel::loadExample) { Text(stringResource(Res.string.regression_example)) }
          result == null -> Text(stringResource(Res.string.regression_not_possible), color = MaterialTheme.colorScheme.error)
          else -> FitSummary(shown, result, format)
        }
      }
    },
    settings = {
      if (prediction != null) ForecastCard(forecastText, prediction, points, format) { forecast = it }
    },
    explanation = {
      if (fits.isNotEmpty()) FitRanking(fits, model, format, viewModel::selectModel)
    },
  )
}

@Composable
private fun Hint(text: String) {
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** The curve as numbers, then what it means. */
@Composable
private fun FitSummary(points: List<DataPoint>, result: RegressionResult, format: NumberFormat) {
  Text(
    modifier = Modifier.fillMaxWidth(),
    text = equation(result, format),
    style = MaterialTheme.typography.titleLarge,
    fontWeight = FontWeight.Medium,
    color = MaterialTheme.colorScheme.tertiary,
    textAlign = TextAlign.Center,
  )
  val determination = result.determination
  if (determination != null) {
    val share = (determination.coerceIn(0.0, 1.0) * PERCENT).rounded(1)
    Text(stringResource(Res.string.regression_explained, "$share%"), style = MaterialTheme.typography.bodyMedium)
  }
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    if (determination != null) Answer(Modifier.weight(1f), "r²", format.format(determination))
    result.correlation?.let { Answer(Modifier.weight(1f), "r", format.format(it)) }
    Answer(Modifier.weight(1f), Res.string.regression_typical_gap, format.format(typicalGap(points, result)))
  }
}

/** What the curve says for an x that was not measured. */
@Composable
private fun ForecastCard(value: String, prediction: DataPoint, points: List<DataPoint>, format: NumberFormat, onChange: (String) -> Unit) {
  val low = points.minOf { it.x }
  val high = points.maxOf { it.x }
  val span = (high - low).takeIf { it > 0 } ?: 1.0
  val condition = Condition("x", "", Res.string.regression_predict_at, (low - span).coerceAtMost(0.0)..(high + span), span / SLIDER_STEPS)
  Card(stringResource(Res.string.regression_predict)) {
    ConditionRow(condition, value, ".", onChange)
    Answer(Modifier.fillMaxWidth(), "y", format.format(prediction.y))
  }
}

/** All the curves that fit, the best first. Tapping one shows it on the chart. */
@Composable
private fun FitRanking(
  fits: List<RankedFit>,
  selected: RegressionModel,
  format: NumberFormat,
  onSelect: (RegressionModel) -> Unit,
) {
  Card(stringResource(Res.string.regression_best_title)) {
    Hint(stringResource(Res.string.regression_best_note))
    fits.forEach { fit ->
      FitRow(fit.result, fit.result.model == selected, fit.isBest, format) { onSelect(fit.result.model) }
    }
  }
}

@Composable
private fun FitRow(result: RegressionResult, isSelected: Boolean, isBest: Boolean, format: NumberFormat, onClick: () -> Unit) {
  val scheme = MaterialTheme.colorScheme
  val determination = result.determination
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(ROW_SHAPE)
        .background(if (isSelected) scheme.secondaryContainer else Color.Transparent)
        .clickable(onClick = onClick)
        .padding(10.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(stringResource(result.model.title), style = MaterialTheme.typography.titleSmall)
      if (isBest) {
        Text(
          stringResource(Res.string.regression_best),
          modifier = Modifier.clip(ROW_SHAPE).background(scheme.tertiary).padding(horizontal = 8.dp, vertical = 2.dp),
          style = MaterialTheme.typography.labelSmall,
          color = scheme.onTertiary,
        )
      }
      Box(Modifier.weight(1f))
      if (determination != null) Text("r² ${format.format(determination)}", style = MaterialTheme.typography.labelMedium)
    }
    Text(result.model.formula, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    Box(Modifier.fillMaxWidth().height(BAR_HEIGHT).clip(ROW_SHAPE).background(scheme.surfaceContainerHighest)) {
      val share = (determination ?: 0.0).coerceIn(0.0, 1.0).toFloat()
      if (share > 0f) Box(Modifier.fillMaxWidth(share).height(BAR_HEIGHT).clip(ROW_SHAPE).background(scheme.primary))
    }
  }
}

@Composable
private fun StatsTab(modifier: Modifier, points: List<DataPoint>, format: NumberFormat) {
  val statistics = remember(points) { pointStatistics(points) }
  if (statistics == null) {
    Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) { Hint(stringResource(Res.string.regression_hint)) }
    return
  }
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

private val TABS = listOf(Res.string.regression_data, Res.string.regression_curve, Res.string.regression_stats)
private const val DATA_TAB = 0
private const val CURVE_TAB = 1
private const val MIN_POINTS = 2
private const val CHART_DECIMALS = 2.5
private const val MAX_DECIMALS = 6
private const val FORECAST_SHARE = 0.25
private const val PERCENT = 100
private const val SLIDER_STEPS = 100
private val ROW_SHAPE = RoundedCornerShape(12.dp)
private val BAR_HEIGHT = 6.dp
