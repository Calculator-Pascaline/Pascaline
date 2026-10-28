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

package com.sadellie.unitto.feature.calculator.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.datatable.DataTable
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.functions.formatTick
import com.sadellie.unitto.feature.calculator.functions.gridValues
import com.sadellie.unitto.feature.calculator.functions.niceStep
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlin.math.max
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.statistics_automatic
import unitto.core.common.generated.resources.statistics_bar
import unitto.core.common.generated.resources.statistics_bar_start
import unitto.core.common.generated.resources.statistics_bar_width
import unitto.core.common.generated.resources.statistics_box_plot
import unitto.core.common.generated.resources.statistics_data
import unitto.core.common.generated.resources.statistics_deviation
import unitto.core.common.generated.resources.statistics_empty
import unitto.core.common.generated.resources.statistics_first_quartile
import unitto.core.common.generated.resources.statistics_frequencies
import unitto.core.common.generated.resources.statistics_histogram
import unitto.core.common.generated.resources.statistics_interquartile_range
import unitto.core.common.generated.resources.statistics_maximum
import unitto.core.common.generated.resources.statistics_mean
import unitto.core.common.generated.resources.statistics_median
import unitto.core.common.generated.resources.statistics_minimum
import unitto.core.common.generated.resources.statistics_range
import unitto.core.common.generated.resources.statistics_sample_deviation
import unitto.core.common.generated.resources.statistics_stats
import unitto.core.common.generated.resources.statistics_sum
import unitto.core.common.generated.resources.statistics_sum_of_squares
import unitto.core.common.generated.resources.statistics_tap_bar
import unitto.core.common.generated.resources.statistics_third_quartile
import unitto.core.common.generated.resources.statistics_title
import unitto.core.common.generated.resources.statistics_total_frequency
import unitto.core.common.generated.resources.statistics_values
import unitto.core.common.generated.resources.statistics_variance

@Composable
internal fun StatisticsRoute(openDrawer: () -> Unit) {
  val viewModel: StatisticsViewModel = koinViewModel()
  val format = viewModel.numberFormat.collectAsStateWithLifecycleKMP().value
  val rows = viewModel.table.rows
  if (format == null || rows == null) {
    EmptyScreen()
    return
  }
  StatisticsScreen(openDrawer, viewModel, rows, format)
}

/** Like the Statistics app of NumWorks: data, histogram, box plot and statistics. */
@Composable
private fun StatisticsScreen(
  openDrawer: () -> Unit,
  viewModel: StatisticsViewModel,
  rows: List<List<String>>,
  format: NumberFormat,
) {
  var tab by rememberSaveable { mutableIntStateOf(0) }
  val values = remember(rows) { rows.weightedValues() }
  val statistics = remember(values) { statisticsOf(values) }

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.statistics_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
      PrimaryScrollableTabRow(
        selectedTabIndex = tab,
        containerColor = MaterialTheme.colorScheme.surface,
        edgePadding = 8.dp,
      ) {
        TABS.forEachIndexed { index, title ->
          Tab(selected = tab == index, onClick = { tab = index }, text = { Text(stringResource(title)) })
        }
      }
      val content = Modifier.weight(1f).fillMaxWidth()
      when {
        tab == DATA_TAB ->
          DataTable(
            modifier = content,
            headers = listOf(stringResource(Res.string.statistics_values), stringResource(Res.string.statistics_frequencies)),
            rows = rows,
            onCellChange = viewModel.table::setCell,
            onDeleteRow = viewModel.table::deleteRow,
            onClear = viewModel.table::clear,
          )
        statistics == null -> EmptyData(content)
        tab == HISTOGRAM_TAB -> HistogramTab(content, values, format)
        tab == BOX_TAB -> BoxPlotTab(content, statistics, format)
        else -> StatsTab(content, statistics, format)
      }
    }
  }
}

@Composable
private fun EmptyData(modifier: Modifier) {
  Box(modifier = modifier.padding(24.dp), contentAlignment = Alignment.Center) {
    Text(
      text = stringResource(Res.string.statistics_empty),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

/** Bars of the frequencies. Width and start are automatic until they are typed. Tap a bar to read it. */
@Composable
private fun HistogramTab(modifier: Modifier, values: List<WeightedValue>, format: NumberFormat) {
  var widthText by rememberSaveable { mutableStateOf("") }
  var startText by rememberSaveable { mutableStateOf("") }
  val width = widthText.toNumberOrNull()?.takeIf { it > 0 } ?: autoBarWidth(values)
  val start = startText.toNumberOrNull() ?: autoBarStart(values, width)
  val bars = remember(values, start, width) { histogram(values, start, width) }
  var selected by remember(bars) { mutableStateOf<Int?>(null) }

  val measurer = rememberTextMeasurer()
  val barColor = MaterialTheme.colorScheme.primary
  val selectedColor = MaterialTheme.colorScheme.tertiary
  val axisColor = MaterialTheme.colorScheme.onSurface
  val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)

  Column(modifier = modifier) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      NumberField(Modifier.weight(1f), widthText, { widthText = it }, stringResource(Res.string.statistics_bar_width), format.format(width))
      NumberField(Modifier.weight(1f), startText, { startText = it }, stringResource(Res.string.statistics_bar_start), format.format(start))
    }
    Canvas(
      modifier =
        Modifier.weight(1f).fillMaxWidth().padding(16.dp).pointerInput(bars) {
          detectTapGestures { tap ->
            val plot = plotArea(size.width.toFloat(), size.height.toFloat())
            val index = ((tap.x - plot.left) / plot.width * bars.size).toInt()
            selected = index.takeIf { it in bars.indices }
          }
        }
    ) {
      if (bars.isEmpty()) return@Canvas
      val plot = plotArea(size.width, size.height)
      val maxFrequency = bars.maxOf { it.frequency }.coerceAtLeast(MIN_FREQUENCY)
      val stepY = niceStep(maxFrequency / Y_TICKS)
      val top = (kotlin.math.ceil(maxFrequency / stepY) * stepY)
      val barWidth = plot.width / bars.size
      fun yOf(frequency: Double) = plot.bottom - (frequency / top * plot.height).toFloat()

      gridValues(0.0, top, stepY).forEach { tick ->
        val y = yOf(tick)
        drawLine(axisColor.copy(alpha = GRID_ALPHA), Offset(plot.left, y), Offset(plot.right, y))
        drawLabelEnd(measurer, formatTick(tick, stepY), Offset(plot.left - LABEL_GAP.toPx(), y), labelStyle)
      }
      bars.forEachIndexed { index, bar ->
        val left = plot.left + index * barWidth
        val y = yOf(bar.frequency)
        drawRect(
          color = if (index == selected) selectedColor else barColor,
          topLeft = Offset(left + BAR_GAP.toPx() / 2, y),
          size = Size(barWidth - BAR_GAP.toPx(), plot.bottom - y),
        )
      }
      drawLine(axisColor, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), strokeWidth = 2f)
      // Bounds under the bars, fewer when they don't fit
      val every = max(1, (bars.size + 1) / MAX_X_LABELS + 1)
      (0..bars.size step every).forEach { index ->
        val x = plot.left + index * barWidth
        val text = formatTick(start + index * width, width)
        drawLabelCentered(measurer, text, Offset(x, plot.bottom + LABEL_GAP.toPx()), labelStyle)
      }
    }
    val bar = selected?.let { bars.getOrNull(it) }
    Text(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      text =
        if (bar == null) stringResource(Res.string.statistics_tap_bar)
        else stringResource(Res.string.statistics_bar, format.format(bar.start), format.format(bar.end), format.format(bar.frequency)),
      style = MaterialTheme.typography.titleMedium,
      color = if (bar == null) MaterialTheme.colorScheme.onSurfaceVariant else selectedColor,
      textAlign = TextAlign.Center,
    )
  }
}

/** Minimum, quartiles, median and maximum, drawn and written. */
@Composable
private fun BoxPlotTab(modifier: Modifier, statistics: Statistics, format: NumberFormat) {
  val measurer = rememberTextMeasurer()
  val color = MaterialTheme.colorScheme.primary
  val fill = MaterialTheme.colorScheme.primaryContainer
  val axisColor = MaterialTheme.colorScheme.onSurface
  val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
  val points =
    listOf(
      Res.string.statistics_minimum to statistics.minimum,
      Res.string.statistics_first_quartile to statistics.firstQuartile,
      Res.string.statistics_median to statistics.median,
      Res.string.statistics_third_quartile to statistics.thirdQuartile,
      Res.string.statistics_maximum to statistics.maximum,
    )

  Column(modifier = modifier) {
    Canvas(modifier = Modifier.fillMaxWidth().height(BOX_PLOT_HEIGHT).padding(horizontal = 24.dp, vertical = 16.dp)) {
      val range = statistics.range.takeIf { it > 0 } ?: 1.0
      val from = statistics.minimum - range * BOX_MARGIN
      val to = statistics.maximum + range * BOX_MARGIN
      fun xOf(value: Double) = ((value - from) / (to - from) * size.width).toFloat()
      val axisY = size.height - AXIS_LABEL_HEIGHT.toPx()
      val middle = axisY / 2
      val half = axisY * BOX_SHARE / 2
      val stroke = BOX_STROKE.toPx()

      // Whiskers with their ends
      drawLine(color, Offset(xOf(statistics.minimum), middle), Offset(xOf(statistics.firstQuartile), middle), stroke)
      drawLine(color, Offset(xOf(statistics.thirdQuartile), middle), Offset(xOf(statistics.maximum), middle), stroke)
      listOf(statistics.minimum, statistics.maximum).forEach {
        drawLine(color, Offset(xOf(it), middle - half / 2), Offset(xOf(it), middle + half / 2), stroke)
      }
      // Box from the first to the third quartile, with the median
      val boxLeft = xOf(statistics.firstQuartile)
      val boxSize = Size(xOf(statistics.thirdQuartile) - boxLeft, half * 2)
      drawRect(fill, Offset(boxLeft, middle - half), boxSize)
      drawRect(color, Offset(boxLeft, middle - half), boxSize, style = Stroke(stroke))
      drawLine(color, Offset(xOf(statistics.median), middle - half), Offset(xOf(statistics.median), middle + half), stroke)

      // Axis
      val step = niceStep((to - from) / X_TICKS)
      drawLine(axisColor, Offset(0f, axisY), Offset(size.width, axisY), 2f)
      gridValues(from, to, step).forEach { tick ->
        val x = xOf(tick)
        drawLine(axisColor, Offset(x, axisY - TICK.toPx()), Offset(x, axisY + TICK.toPx()), 2f)
        drawLabelCentered(measurer, formatTick(tick, step), Offset(x, axisY + LABEL_GAP.toPx()), labelStyle)
      }
    }
    HorizontalDivider()
    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
      items(points) { (label, value) -> StatLine(stringResource(label), format.format(value)) }
    }
  }
}

@Composable
private fun StatsTab(modifier: Modifier, statistics: Statistics, format: NumberFormat) {
  val lines =
    listOf(
      Res.string.statistics_total_frequency to statistics.totalFrequency,
      Res.string.statistics_minimum to statistics.minimum,
      Res.string.statistics_maximum to statistics.maximum,
      Res.string.statistics_range to statistics.range,
      Res.string.statistics_mean to statistics.mean,
      Res.string.statistics_deviation to statistics.deviation,
      Res.string.statistics_variance to statistics.variance,
      Res.string.statistics_sample_deviation to (statistics.sampleDeviation ?: Double.NaN),
      Res.string.statistics_first_quartile to statistics.firstQuartile,
      Res.string.statistics_median to statistics.median,
      Res.string.statistics_third_quartile to statistics.thirdQuartile,
      Res.string.statistics_interquartile_range to statistics.interquartileRange,
      Res.string.statistics_sum to statistics.sum,
      Res.string.statistics_sum_of_squares to statistics.sumOfSquares,
    )
  LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = 8.dp)) {
    items(lines) { (label, value) ->
      StatLine(stringResource(label), format.format(value))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

/** `Mean        2.5` */
@Composable
internal fun StatLine(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(modifier = Modifier.weight(1f), text = label, style = MaterialTheme.typography.bodyLarge)
    Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
  }
}

/** Number typed with the decimal keyboard. [automatic] is shown while it is empty. */
@Composable
private fun NumberField(
  modifier: Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  automatic: String,
) {
  OutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = { Text(automatic) },
    // Automatic value stays readable while the field is not focused
    supportingText = if (value.isBlank()) ({ Text(stringResource(Res.string.statistics_automatic, automatic)) }) else null,
    singleLine = true,
    isError = value.isNotBlank() && value.toNumberOrNull() == null,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

/** Where bars are drawn: room on the left for frequencies and at the bottom for bounds. */
private class PlotArea(val left: Float, val right: Float, val bottom: Float, val top: Float) {
  val width: Float
    get() = right - left

  val height: Float
    get() = bottom - top
}

private fun Density.plotArea(width: Float, height: Float) =
  PlotArea(left = Y_LABELS_WIDTH.toPx(), right = width, bottom = height - AXIS_LABEL_HEIGHT.toPx(), top = 0f)

private fun DrawScope.drawLabelCentered(measurer: TextMeasurer, text: String, topCenter: Offset, style: TextStyle) {
  val layout = measurer.measure(text, style)
  val x = (topCenter.x - layout.size.width / 2).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
  drawText(layout, topLeft = Offset(x, topCenter.y))
}

private fun DrawScope.drawLabelEnd(measurer: TextMeasurer, text: String, centerEnd: Offset, style: TextStyle) {
  val layout = measurer.measure(text, style)
  drawText(layout, topLeft = Offset((centerEnd.x - layout.size.width).coerceAtLeast(0f), centerEnd.y - layout.size.height / 2))
}

private val TABS: List<StringResource> =
  listOf(
    Res.string.statistics_data,
    Res.string.statistics_histogram,
    Res.string.statistics_box_plot,
    Res.string.statistics_stats,
  )
private const val DATA_TAB = 0
private const val HISTOGRAM_TAB = 1
private const val BOX_TAB = 2
private const val MIN_FREQUENCY = 1e-9
private const val Y_TICKS = 5.0
private const val X_TICKS = 5.0
private const val MAX_X_LABELS = 8
private const val GRID_ALPHA = 0.15f
private const val BOX_MARGIN = 0.08
private const val BOX_SHARE = 0.5f
private val Y_LABELS_WIDTH = 36.dp
private val AXIS_LABEL_HEIGHT = 20.dp
private val LABEL_GAP = 4.dp
private val BAR_GAP = 2.dp
private val BOX_PLOT_HEIGHT = 200.dp
private val BOX_STROKE = 2.5.dp
private val TICK = 4.dp
