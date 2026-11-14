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

package com.sadellie.unitto.feature.calculator.inflation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.core.designsystem.icons.symbols.SwapHoriz
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.textfield.formatExpression
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.inflation_amount
import unitto.core.common.generated.resources.inflation_amount_in_year
import unitto.core.common.generated.resources.inflation_average_since
import unitto.core.common.generated.resources.inflation_buying_power
import unitto.core.common.generated.resources.inflation_compare
import unitto.core.common.generated.resources.inflation_drag_hint
import unitto.core.common.generated.resources.inflation_highest_since
import unitto.core.common.generated.resources.inflation_in_year
import unitto.core.common.generated.resources.inflation_lowest_since
import unitto.core.common.generated.resources.inflation_per_year
import unitto.core.common.generated.resources.inflation_prices
import unitto.core.common.generated.resources.inflation_prices_since
import unitto.core.common.generated.resources.inflation_rate
import unitto.core.common.generated.resources.inflation_sources
import unitto.core.common.generated.resources.inflation_swap
import unitto.core.common.generated.resources.inflation_tap_bar
import unitto.core.common.generated.resources.inflation_title
import unitto.core.common.generated.resources.inflation_value

private enum class InflationTab {
  VALUE,
  RATE,
  COMPARE,
}

/** What money was worth, the inflation of each year, and currencies side by side. */
@Composable
internal fun InflationRoute(openDrawer: () -> Unit) {
  val symbols = rememberFormatterSymbols() ?: return
  var currencyIndex by rememberSaveable { mutableIntStateOf(0) }
  var tab by rememberSaveable { mutableStateOf(InflationTab.VALUE) }
  val currency = CURRENCIES[currencyIndex]
  PhysicsScreen(
    title = Res.string.inflation_title,
    openDrawer = openDrawer,
    live = {
      Tabs(tab) { tab = it }
      if (tab != InflationTab.COMPARE) CurrencyChips(currencyIndex) { currencyIndex = it }
      when (tab) {
        InflationTab.VALUE -> ValueTab(currency, symbols)
        InflationTab.RATE -> RateTab(currency, symbols)
        InflationTab.COMPARE -> CompareTab(symbols)
      }
    },
    settings = {},
    explanation = {
      Text(
        stringResource(Res.string.inflation_sources),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
  )
}

/** Separators of the calculator settings, null until they are read. */
@Composable
internal fun rememberFormatterSymbols(): FormatterSymbols? {
  val preferences = koinInject<UserPreferencesRepository>()
  return remember(preferences) { preferences.calculatorPrefs.map { it.formatterSymbols } }.collectAsState(null).value
}

@Composable
private fun Tabs(selected: InflationTab, onSelect: (InflationTab) -> Unit) {
  val labels = listOf(Res.string.inflation_value, Res.string.inflation_rate, Res.string.inflation_compare)
  SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    InflationTab.entries.forEachIndexed { index, tab ->
      SegmentedButton(
        selected = tab == selected,
        onClick = { onSelect(tab) },
        shape = SegmentedButtonDefaults.itemShape(index, InflationTab.entries.size),
        label = { Text(stringResource(labels[index]), maxLines = 1) },
      )
    }
  }
}

@Composable
private fun CurrencyChips(selected: Int, onSelect: (Int) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    CURRENCIES.forEachIndexed { index, currency ->
      FilterChip(
        selected = index == selected,
        onClick = { onSelect(index) },
        label = { Text("${currency.symbol}  ${stringResource(currency.name)}") },
      )
    }
  }
}

/** An amount from one year brought to another, on the curve of prices. */
@Composable
private fun ValueTab(currency: Currency, symbols: FormatterSymbols) {
  val history = currency.history
  var amount by rememberSaveable { mutableStateOf("100") }
  var from by rememberSaveable { mutableIntStateOf(history.lastYear - DEFAULT_SPAN) }
  var to by rememberSaveable { mutableIntStateOf(history.lastYear) }
  val fromYear = from.coerceIn(history.years)
  val toYear = to.coerceIn(history.years)
  val value = amount.replace(symbols.fractional, ".").toDoubleOrNull() ?: 0.0
  val result = history.value(value, fromYear, toYear)
  Card(stringResource(Res.string.inflation_value)) {
    PriceCurve(history, fromYear, toYear) { newFrom, newTo ->
      from = newFrom
      to = newTo
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        stringResource(Res.string.inflation_drag_hint),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      IconButton(onClick = { from = toYear.also { to = fromYear } }) {
        Icon(Symbols.SwapHoriz, stringResource(Res.string.inflation_swap))
      }
    }
    ResultCard(
      before = stringResource(Res.string.inflation_amount_in_year, money(value, currency.symbol, symbols), fromYear),
      result = money(result, currency.symbol, symbols),
      after = stringResource(Res.string.inflation_in_year, toYear),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.inflation_per_year, percent(history.yearlyRate(fromYear, toYear), symbols))
      Answer(Modifier.weight(1f), Res.string.inflation_prices, "× " + number(history.level(toYear) / history.level(fromYear), symbols))
      Answer(
        Modifier.weight(1f),
        Res.string.inflation_buying_power,
        percent(history.level(fromYear) / history.level(toYear) * PERCENT, symbols, decimals = 0),
      )
    }
  }
  OutlinedTextField(
    modifier = Modifier.fillMaxWidth(),
    value = amount,
    onValueChange = { amount = it },
    label = { Text(stringResource(Res.string.inflation_amount)) },
    suffix = { Text(currency.symbol) },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

/** The amount before, the big result, and the year it is in. */
@Composable
private fun ResultCard(before: String, result: String, after: String, prefix: String = "= ") {
  val colors = MaterialTheme.colorScheme
  Column(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.primaryContainer).padding(14.dp),
  ) {
    Text(before, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
    Text(
      prefix + result,
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = colors.onPrimaryContainer,
    )
    Text(after, style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
  }
}

/**
 * Prices over the years, the span from [from] to [to] filled. Dragging moves the nearest of the
 * two ends, a tap puts it where it is.
 */
@Composable
private fun PriceCurve(history: PriceHistory, from: Int, to: Int, onChange: (Int, Int) -> Unit) {
  val color = MaterialTheme.colorScheme.primary
  val current by rememberUpdatedState(from to to)
  var movesFrom by remember { mutableStateOf(true) }
  fun yearAt(x: Float, width: Float): Int =
    (history.baseYear + (x / width * (history.lastYear - history.baseYear)).roundToInt()).coerceIn(history.years)
  fun move(x: Float, width: Float, pick: Boolean) {
    val year = yearAt(x, width)
    val (start, end) = current
    if (pick) movesFrom = abs(year - start) < abs(year - end)
    if (movesFrom) onChange(year, end) else onChange(start, year)
  }
  Canvas(
    modifier =
      Modifier.fillMaxWidth()
        .height(CURVE_HEIGHT)
        .pointerInput(history) {
          detectTapGestures { move(it.x, size.width.toFloat(), pick = true) }
        }
        .pointerInput(history) {
          detectDragGestures(
            onDragStart = { move(it.x, size.width.toFloat(), pick = true) },
            onDrag = { change, _ -> move(change.position.x, size.width.toFloat(), pick = false) },
          )
        }
  ) {
    drawPriceCurve(history, from, to, color)
  }
  Row(modifier = Modifier.fillMaxWidth()) {
    Text("${history.baseYear}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
    Text("${history.lastYear}", style = MaterialTheme.typography.labelSmall)
  }
}

private fun DrawScope.drawPriceCurve(history: PriceHistory, from: Int, to: Int, color: Color) {
  val years = history.years
  val top = history.level(history.lastYear).coerceAtLeast(history.years.maxOf { history.level(it) })
  fun x(year: Int) = (year - years.first).toFloat() / (years.last - years.first) * size.width
  fun y(year: Int) = size.height - (history.level(year) / top).toFloat() * size.height * CURVE_TOP
  fun area(range: IntRange) =
    Path().apply {
      moveTo(x(range.first), size.height)
      range.forEach { lineTo(x(it), y(it)) }
      lineTo(x(range.last), size.height)
      close()
    }
  drawPath(area(years), color.copy(alpha = AREA_ALPHA))
  drawPath(area(minOf(from, to)..maxOf(from, to)), color.copy(alpha = SPAN_ALPHA))
  val line = Path().apply { years.forEachIndexed { index, year -> if (index == 0) moveTo(x(year), y(year)) else lineTo(x(year), y(year)) } }
  drawPath(line, color, style = Stroke(width = LINE_WIDTH.toPx(), cap = StrokeCap.Round))
  listOf(from, to).forEach { year ->
    drawLine(color, Offset(x(year), y(year)), Offset(x(year), size.height), strokeWidth = 1.dp.toPx())
    drawCircle(color, HANDLE_RADIUS.toPx(), Offset(x(year), y(year)))
    drawCircle(Color.White, HANDLE_RADIUS.toPx() / 2, Offset(x(year), y(year)))
  }
}

/** Inflation of every year as bars, the chosen one explained under them. */
@Composable
private fun RateTab(currency: Currency, symbols: FormatterSymbols) {
  val history = currency.history
  var selected by rememberSaveable { mutableIntStateOf(history.lastYear) }
  val year = selected.coerceIn(history.baseYear + 1, history.lastYear)
  Card(stringResource(Res.string.inflation_rate)) {
    RateBars(history, year) { selected = it }
    Row(modifier = Modifier.fillMaxWidth()) {
      Text("${history.baseYear + 1}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
      Text("${history.lastYear}", style = MaterialTheme.typography.labelSmall)
    }
    Text(
      stringResource(Res.string.inflation_tap_bar),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val rate = history.rate(year) ?: 0.0
    ResultCard(
      before = "$year",
      result = percent(rate, symbols),
      after = recordText(history, year) ?: "",
      prefix = "",
    )
    Answer(
      Modifier.fillMaxWidth(),
      stringResource(Res.string.inflation_average_since, history.baseYear),
      percent(history.yearlyRate(history.baseYear, history.lastYear), symbols),
    )
  }
}

/** "Highest since 1985" when no year since then was as high, "lowest since" when none was as low. */
@Composable
private fun recordText(history: PriceHistory, year: Int): String? {
  val rate = history.rate(year) ?: return null
  val before = (year - 1 downTo history.baseYear + 1)
  val higher = before.firstOrNull { (history.rate(it) ?: 0.0) >= rate }
  val lower = before.firstOrNull { (history.rate(it) ?: 0.0) <= rate }
  return when {
    higher == null -> stringResource(Res.string.inflation_highest_since, history.baseYear + 1)
    year - higher > RECORD_GAP -> stringResource(Res.string.inflation_highest_since, higher)
    lower == null -> stringResource(Res.string.inflation_lowest_since, history.baseYear + 1)
    year - lower > RECORD_GAP -> stringResource(Res.string.inflation_lowest_since, lower)
    else -> null
  }
}

@Composable
private fun RateBars(history: PriceHistory, selected: Int, onSelect: (Int) -> Unit) {
  val highlight = MaterialTheme.colorScheme.onSurface
  Canvas(
    modifier =
      Modifier.fillMaxWidth().height(BARS_HEIGHT).pointerInput(history) {
        fun select(x: Float) {
          val years = history.lastYear - history.baseYear
          onSelect(history.baseYear + 1 + (x / size.width * years).toInt().coerceIn(0, years - 1))
        }
        detectTapGestures { select(it.x) }
      }
  ) {
    val rates = history.rates
    val top = rates.max().coerceAtLeast(1.0)
    val bottom = rates.min().coerceAtMost(0.0)
    val zero = (top / (top - bottom) * size.height).toFloat()
    val width = size.width / rates.size
    rates.forEachIndexed { index, rate ->
      val height = (rate / (top - bottom) * size.height).toFloat()
      val isSelected = history.baseYear + 1 + index == selected
      drawRect(
        color = if (isSelected) highlight else rateColor(rate),
        topLeft = Offset(index * width + width * BAR_GAP / 2, if (height > 0) zero - height else zero),
        size = androidx.compose.ui.geometry.Size(width * (1 - BAR_GAP), abs(height).coerceAtLeast(1f)),
      )
    }
    drawLine(highlight.copy(alpha = AREA_ALPHA), Offset(0f, zero), Offset(size.width, zero))
  }
}

/** Blue when prices went down, then teal, amber and coral as they go up faster. */
internal fun rateColor(rate: Double): Color =
  when {
    rate < 0 -> FALLING_COLOR
    rate <= LOW_RATE -> LOW_COLOR
    rate <= HIGH_RATE -> MEDIUM_COLOR
    else -> HIGH_COLOR
  }

private val FALLING_COLOR = Color(0xFF378ADD)
private val LOW_COLOR = Color(0xFF1D9E75)
private val MEDIUM_COLOR = Color(0xFFBA7517)
private val HIGH_COLOR = Color(0xFFD85A30)

/** Every currency from the same year, prices of that year being 1. */
@Composable
private fun CompareTab(symbols: FormatterSymbols) {
  val first = CURRENCIES.maxOf { it.history.baseYear }
  val last = CURRENCIES.minOf { it.history.lastYear }
  var since by rememberSaveable { mutableIntStateOf(COMPARE_START) }
  val start = since.coerceIn(first, last - 1)
  var highlighted by remember { mutableStateOf<Int?>(null) }
  Card(stringResource(Res.string.inflation_prices_since, start)) {
    Canvas(Modifier.fillMaxWidth().height(CURVE_HEIGHT)) { drawComparison(start, last, highlighted) }
    Slider(
      value = start.toFloat(),
      onValueChange = { since = it.roundToInt() },
      valueRange = first.toFloat()..(last - 1).toFloat(),
    )
    CURRENCIES.indices
      .sortedByDescending { CURRENCIES[it].history.level(last) / CURRENCIES[it].history.level(start) }
      .forEach { index ->
        val history = CURRENCIES[index].history
        CompareLine(index, highlighted == index, history.level(last) / history.level(start), history.yearlyRate(start, last), symbols) {
          highlighted = if (highlighted == index) null else index
        }
      }
  }
}

@Composable
private fun CompareLine(index: Int, isHighlighted: Boolean, times: Double, perYear: Double, symbols: FormatterSymbols, onClick: () -> Unit) {
  val currency = CURRENCIES[index]
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(if (isHighlighted) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
        .clickable(onClick = onClick)
        .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(LINE_COLORS[index]))
    Text("${currency.symbol}  ${stringResource(currency.name)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    Text(stringResource(Res.string.inflation_per_year) + ": " + percent(perYear, symbols), style = MaterialTheme.typography.bodySmall)
    Text("× " + number(times, symbols), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
  }
}

private fun DrawScope.drawComparison(start: Int, last: Int, highlighted: Int?) {
  val top = CURRENCIES.maxOf { currency -> (start..last).maxOf { currency.history.level(it) / currency.history.level(start) } }
  fun x(year: Int) = (year - start).toFloat() / (last - start) * size.width
  CURRENCIES.forEachIndexed { index, currency ->
    val history = currency.history
    fun y(year: Int) = size.height - ((history.level(year) / history.level(start) - BASE_LEVEL) / (top - BASE_LEVEL)).toFloat() * size.height
    val path = Path().apply {
      moveTo(x(start), y(start))
      for (year in start + 1..last) lineTo(x(year), y(year))
    }
    val isFaded = highlighted != null && highlighted != index
    drawPath(
      path,
      LINE_COLORS[index].copy(alpha = if (isFaded) FADED_ALPHA else 1f),
      style = Stroke(width = (if (highlighted == index) BOLD_LINE_WIDTH else LINE_WIDTH).toPx(), cap = StrokeCap.Round),
    )
  }
}

/** Grouped like the calculator, 2 decimals: 1 234.50 €. Symbols like $ go first. */
internal fun money(value: Double, symbol: String, symbols: FormatterSymbols): String {
  val text = number(value, symbols)
  return if (symbol in SYMBOLS_FIRST) symbol + text else "$text $symbol"
}

internal fun number(value: Double, symbols: FormatterSymbols, decimals: Int = MONEY_DECIMALS): String {
  if (!value.isFinite()) return "—"
  return KBigDecimal.valueOf(value).setScale(decimals, KRoundingMode.HALF_EVEN).toPlainString().formatExpression(symbols)
}

internal fun percent(value: Double, symbols: FormatterSymbols, decimals: Int = 1): String = number(value, symbols, decimals) + " %"

private val SYMBOLS_FIRST = setOf("$", "£", "¥", "CN¥", "A$", "C$", "IR£")

/** One color per currency, mid tones that read on light and dark. */
private val LINE_COLORS =
  listOf(
    Color(0xFF7F77DD),
    Color(0xFFD85A30),
    Color(0xFF1D9E75),
    Color(0xFF378ADD),
    Color(0xFFBA7517),
    Color(0xFFD4537E),
    Color(0xFF639922),
    Color(0xFF888780),
  )

private const val PERCENT = 100.0
private const val MONEY_DECIMALS = 2
private const val DEFAULT_SPAN = 30
private const val COMPARE_START = 2000
private const val RECORD_GAP = 2
private const val LOW_RATE = 2.0
private const val HIGH_RATE = 5.0
private const val BASE_LEVEL = 1.0
private const val CURVE_TOP = 0.92f
private const val AREA_ALPHA = 0.15f
private const val SPAN_ALPHA = 0.4f
private const val FADED_ALPHA = 0.2f
private const val BAR_GAP = 0.25f
private val CURVE_HEIGHT = 180.dp
private val BARS_HEIGHT = 160.dp
private val LINE_WIDTH = 2.dp
private val BOLD_LINE_WIDTH = 3.5.dp
private val HANDLE_RADIUS = 7.dp
