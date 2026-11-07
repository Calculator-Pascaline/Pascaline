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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.datatable.SavedTable
import com.sadellie.unitto.feature.calculator.functions.formatTick
import com.sadellie.unitto.feature.calculator.functions.gridValues
import com.sadellie.unitto.feature.calculator.functions.niceStep
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import kotlin.math.ceil
import kotlin.math.max
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.common_add
import unitto.core.common.generated.resources.common_clear
import unitto.core.common.generated.resources.common_delete
import unitto.core.common.generated.resources.common_ok
import unitto.core.common.generated.resources.statistics_add_hint
import unitto.core.common.generated.resources.statistics_automatic
import unitto.core.common.generated.resources.statistics_bar
import unitto.core.common.generated.resources.statistics_bar_start
import unitto.core.common.generated.resources.statistics_bar_width
import unitto.core.common.generated.resources.statistics_deviation
import unitto.core.common.generated.resources.statistics_edit
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
import unitto.core.common.generated.resources.statistics_start
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
  StatisticsScreen(openDrawer, viewModel.table, rows, format)
}

/** One page: the values, what matters most, a chart of everything and all the other numbers. */
@Composable
private fun StatisticsScreen(openDrawer: () -> Unit, table: SavedTable, rows: List<List<String>>, format: NumberFormat) {
  val values = remember(rows) { rows.weightedValues() }
  val statistics = remember(values) { statisticsOf(values) }
  PhysicsScreen(
    title = Res.string.statistics_title,
    openDrawer = openDrawer,
    live = {
      DataCard(table, rows)
      if (statistics == null) {
        Text(
          text = stringResource(Res.string.statistics_start),
          modifier = Modifier.fillMaxWidth().padding(24.dp),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
      } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Answer(Modifier.weight(1f), Res.string.statistics_mean, format.format(statistics.mean))
          Answer(Modifier.weight(1f), Res.string.statistics_median, format.format(statistics.median))
          Answer(Modifier.weight(1f), Res.string.statistics_deviation, format.format(statistics.deviation))
        }
      }
    },
    settings = { if (statistics != null) ChartCard(values, statistics, format) },
    explanation = { if (statistics != null) DetailsCard(statistics, format) },
  )
}

/** Values as chips: type one to add it, tap one to change it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DataCard(table: SavedTable, rows: List<List<String>>) {
  var text by rememberSaveable { mutableStateOf("") }
  var editing by rememberSaveable { mutableStateOf<Int?>(null) }
  fun add() {
    splitValues(text).forEach { value -> table.setCell(table.rows?.size ?: 0, 0, value) }
    text = ""
  }
  LiveCard {
    OutlinedTextField(
      modifier = Modifier.fillMaxWidth(),
      value = text,
      onValueChange = { text = it },
      label = { Text(stringResource(Res.string.statistics_add_hint)) },
      singleLine = true,
      trailingIcon = {
        IconButton(onClick = ::add) { Icon(Symbols.Add, stringResource(Res.string.common_add)) }
      },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { add() }),
    )
    if (rows.isNotEmpty()) {
      FlowRow(
        modifier = Modifier.fillMaxWidth().heightIn(max = CHIPS_HEIGHT).verticalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
      ) {
        rows.forEachIndexed { index, cells ->
          AssistChip(onClick = { editing = index }, label = { Text(chipLabel(cells)) })
        }
        TextButton(onClick = table::clear) { Text(stringResource(Res.string.common_clear)) }
      }
    }
  }
  editing?.let { index ->
    val cells = rows.getOrNull(index) ?: return@let
    ValueDialog(
      cells = cells,
      onDismiss = { editing = null },
      onDelete = {
        table.deleteRow(index)
        editing = null
      },
      onSave = { value, frequency ->
        table.setCell(index, 1, frequency)
        table.setCell(index, 0, value)
        editing = null
      },
    )
  }
}

/** `15`, or `15 ×3` when it was seen many times. */
private fun chipLabel(cells: List<String>): String {
  val frequency = cells.getOrNull(1).orEmpty()
  return if (frequency.isBlank() || frequency.toNumberOrNull() == 1.0) cells[0] else "${cells[0]} ×$frequency"
}

@Composable
private fun ValueDialog(
  cells: List<String>,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
  onSave: (String, String) -> Unit,
) {
  var value by remember(cells) { mutableStateOf(cells[0]) }
  var frequency by remember(cells) { mutableStateOf(cells.getOrNull(1).orEmpty()) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(Res.string.statistics_edit)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DialogField(value, stringResource(Res.string.statistics_values)) { value = it }
        DialogField(frequency, stringResource(Res.string.statistics_frequencies)) { frequency = it }
      }
    },
    confirmButton = {
      TextButton(onClick = { if (value.isBlank()) onDelete() else onSave(value, frequency) }) {
        Text(stringResource(Res.string.common_ok))
      }
    },
    dismissButton = { TextButton(onClick = onDelete) { Text(stringResource(Res.string.common_delete)) } },
  )
}

@Composable
private fun DialogField(value: String, label: String, onValueChange: (String) -> Unit) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    isError = value.isNotBlank() && value.toNumberOrNull() == null,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

/** Bars of the frequencies and the box plot, one above the other on the same axis. */
@Composable
private fun ChartCard(values: List<WeightedValue>, statistics: Statistics, format: NumberFormat) {
  var widthText by rememberSaveable { mutableStateOf("") }
  var startText by rememberSaveable { mutableStateOf("") }
  val width = widthText.toNumberOrNull()?.takeIf { it > 0 } ?: autoBarWidth(values)
  val start = startText.toNumberOrNull() ?: autoBarStart(values, width)
  val bars = remember(values, start, width) { histogram(values, start, width) }
  var selected by remember(bars) { mutableStateOf<Int?>(null) }
  val growth = remember(bars) { Animatable(0f) }
  LaunchedEffect(bars) { growth.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }

  val colors = MaterialTheme.colorScheme
  val measurer = rememberTextMeasurer()
  val labels = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
  val mean = stringResource(Res.string.statistics_mean)
  LiveCard {
    Text(stringResource(Res.string.statistics_histogram), style = MaterialTheme.typography.titleMedium, color = colors.primary)
    Canvas(
      modifier =
        Modifier.fillMaxWidth().height(CHART_HEIGHT).padding(top = MEAN_LABEL_HEIGHT).pointerInput(bars) {
          detectTapGestures { tap ->
            val plot = plotArea(size.width.toFloat(), size.height.toFloat())
            val index = ((tap.x - plot.left) / plot.width * bars.size).toInt()
            selected = index.takeIf { it in bars.indices && tap.y <= plot.bottom }
          }
        }
    ) {
      if (bars.isEmpty()) return@Canvas
      val plot = plotArea(size.width, size.height)
      val bar = colors.primary
      drawFrequencies(bars, plot, growth.value) { index -> if (index == selected) colors.tertiary else bar }
      drawGrid(bars, plot, measurer, labels, colors.onSurface)
      val from = bars.first().start
      val to = bars.last().end
      fun xOf(value: Double) = (plot.left + (value - from) / (to - from) * plot.width).toFloat().coerceIn(plot.left, plot.right)
      drawBox(statistics, plot, bar, colors.primaryContainer, ::xOf)
      drawMean(xOf(statistics.mean), plot, measurer, mean, labels.copy(color = colors.tertiary), colors.tertiary)
      drawBounds(bars, plot, measurer, labels)
    }
    val bar = selected?.let { bars.getOrNull(it) }
    Text(
      modifier = Modifier.fillMaxWidth(),
      text =
        if (bar == null) stringResource(Res.string.statistics_tap_bar)
        else stringResource(Res.string.statistics_bar, format.format(bar.start), format.format(bar.end), format.format(bar.frequency)),
      style = MaterialTheme.typography.titleSmall,
      color = if (bar == null) colors.onSurfaceVariant else colors.tertiary,
      textAlign = TextAlign.Center,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      NumberField(Modifier.weight(1f), widthText, { widthText = it }, stringResource(Res.string.statistics_bar_width), format.format(width))
      NumberField(Modifier.weight(1f), startText, { startText = it }, stringResource(Res.string.statistics_bar_start), format.format(start))
    }
  }
}

private fun DrawScope.drawFrequencies(bars: List<HistogramBar>, plot: PlotArea, growth: Float, color: (Int) -> Color) {
  val top = roundedTop(bars)
  val barWidth = plot.width / bars.size
  bars.forEachIndexed { index, bar ->
    val height = (bar.frequency / top * plot.height).toFloat() * growth
    drawRoundRect(
      color = color(index),
      topLeft = Offset(plot.left + index * barWidth + BAR_GAP.toPx() / 2, plot.bottom - height),
      size = Size(barWidth - BAR_GAP.toPx(), height),
      cornerRadius = CornerRadius(BAR_CORNER.toPx()),
    )
  }
}

/** The highest frequency, up to the next line of the grid. */
private fun roundedTop(bars: List<HistogramBar>): Double {
  val highest = bars.maxOf { it.frequency }.coerceAtLeast(MIN_FREQUENCY)
  val step = niceStep(highest / Y_TICKS)
  return ceil(highest / step) * step
}

private fun DrawScope.drawGrid(bars: List<HistogramBar>, plot: PlotArea, measurer: TextMeasurer, labels: TextStyle, axis: Color) {
  val top = roundedTop(bars)
  val step = niceStep(bars.maxOf { it.frequency }.coerceAtLeast(MIN_FREQUENCY) / Y_TICKS)
  gridValues(0.0, top, step).forEach { tick ->
    val y = plot.bottom - (tick / top * plot.height).toFloat()
    drawLine(axis.copy(alpha = GRID_ALPHA), Offset(plot.left, y), Offset(plot.right, y))
    drawLabelEnd(measurer, formatTick(tick, step), Offset(plot.left - LABEL_GAP.toPx(), y), labels)
  }
  drawLine(axis, Offset(plot.left, plot.bottom), Offset(plot.right, plot.bottom), strokeWidth = 2f)
}

/** Minimum, quartiles, median and maximum under the bars they belong to. */
private fun DrawScope.drawBox(statistics: Statistics, plot: PlotArea, color: Color, fill: Color, xOf: (Double) -> Float) {
  val middle = plot.bottom + BOX_AREA.toPx() / 2
  val half = BOX_HALF.toPx()
  val stroke = BOX_STROKE.toPx()
  drawLine(color, Offset(xOf(statistics.minimum), middle), Offset(xOf(statistics.firstQuartile), middle), stroke)
  drawLine(color, Offset(xOf(statistics.thirdQuartile), middle), Offset(xOf(statistics.maximum), middle), stroke)
  listOf(statistics.minimum, statistics.maximum).forEach {
    drawLine(color, Offset(xOf(it), middle - half / 2), Offset(xOf(it), middle + half / 2), stroke)
  }
  val left = xOf(statistics.firstQuartile)
  val box = Size(xOf(statistics.thirdQuartile) - left, half * 2)
  val corner = CornerRadius(BAR_CORNER.toPx())
  drawRoundRect(fill, Offset(left, middle - half), box, corner)
  drawRoundRect(color, Offset(left, middle - half), box, corner, style = Stroke(stroke))
  drawLine(color, Offset(xOf(statistics.median), middle - half), Offset(xOf(statistics.median), middle + half), stroke)
}

/** Line through the bars and the box, named above them. */
private fun DrawScope.drawMean(x: Float, plot: PlotArea, measurer: TextMeasurer, name: String, style: TextStyle, color: Color) {
  drawLine(color, Offset(x, plot.top), Offset(x, plot.bottom + BOX_AREA.toPx()), strokeWidth = MEAN_WIDTH.toPx())
  drawLabelCentered(measurer, name, Offset(x, plot.top - MEAN_LABEL_HEIGHT.toPx()), style)
}

/** Bounds under the box, fewer when they don't fit. */
private fun DrawScope.drawBounds(bars: List<HistogramBar>, plot: PlotArea, measurer: TextMeasurer, labels: TextStyle) {
  val width = bars.first().end - bars.first().start
  val barWidth = plot.width / bars.size
  val every = max(1, (bars.size + 1) / MAX_X_LABELS + 1)
  for (index in 0..bars.size step every) {
    val text = formatTick(bars.first().start + index * width, width)
    drawLabelCentered(measurer, text, Offset(plot.left + index * barWidth, plot.bottom + BOX_AREA.toPx()), labels)
  }
}

@Composable
private fun DetailsCard(statistics: Statistics, format: NumberFormat) {
  val lines =
    listOf(
      Res.string.statistics_total_frequency to statistics.totalFrequency,
      Res.string.statistics_minimum to statistics.minimum,
      Res.string.statistics_first_quartile to statistics.firstQuartile,
      Res.string.statistics_third_quartile to statistics.thirdQuartile,
      Res.string.statistics_maximum to statistics.maximum,
      Res.string.statistics_range to statistics.range,
      Res.string.statistics_interquartile_range to statistics.interquartileRange,
      Res.string.statistics_variance to statistics.variance,
      Res.string.statistics_sample_deviation to (statistics.sampleDeviation ?: Double.NaN),
      Res.string.statistics_sum to statistics.sum,
      Res.string.statistics_sum_of_squares to statistics.sumOfSquares,
    )
  Card(stringResource(Res.string.statistics_stats)) {
    lines.forEachIndexed { index, (label, value) ->
      if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(modifier = Modifier.weight(1f), text = stringResource(label), style = MaterialTheme.typography.bodyMedium)
        Text(format.format(value), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
      }
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

/** Where bars are drawn: room on the left for frequencies and below for the box and the bounds. */
private class PlotArea(val left: Float, val right: Float, val bottom: Float, val top: Float) {
  val width: Float
    get() = right - left

  val height: Float
    get() = bottom - top
}

private fun Density.plotArea(width: Float, height: Float) =
  PlotArea(left = Y_LABELS_WIDTH.toPx(), right = width, bottom = height - AXIS_LABEL_HEIGHT.toPx() - BOX_AREA.toPx(), top = 0f)

private fun DrawScope.drawLabelCentered(measurer: TextMeasurer, text: String, topCenter: Offset, style: TextStyle) {
  val layout = measurer.measure(text, style)
  val x = (topCenter.x - layout.size.width / 2).coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f))
  drawText(layout, topLeft = Offset(x, topCenter.y))
}

private fun DrawScope.drawLabelEnd(measurer: TextMeasurer, text: String, centerEnd: Offset, style: TextStyle) {
  val layout = measurer.measure(text, style)
  drawText(layout, topLeft = Offset((centerEnd.x - layout.size.width).coerceAtLeast(0f), centerEnd.y - layout.size.height / 2))
}

private const val MIN_FREQUENCY = 1e-9
private const val Y_TICKS = 5.0
private const val MAX_X_LABELS = 8
private const val GRID_ALPHA = 0.15f
private val CHIPS_HEIGHT = 160.dp
private val CHART_HEIGHT = 300.dp
private val MEAN_LABEL_HEIGHT = 18.dp
private val MEAN_WIDTH = 2.dp
private val Y_LABELS_WIDTH = 36.dp
private val AXIS_LABEL_HEIGHT = 20.dp
private val LABEL_GAP = 4.dp
private val BAR_GAP = 3.dp
private val BAR_CORNER = 4.dp
private val BOX_AREA = 44.dp
private val BOX_HALF = 12.dp
private val BOX_STROKE = 2.dp
