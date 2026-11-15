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

package io.github.calculatorpascaline.feature.calculator.inflation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.calculatorpascaline.core.common.FormatterSymbols
import io.github.calculatorpascaline.core.common.KBigDecimal
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Add
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Remove
import io.github.calculatorpascaline.core.designsystem.icons.symbols.Symbols
import io.github.calculatorpascaline.feature.calculator.physics.PhysicsScreen
import io.github.calculatorpascaline.feature.calculator.projectile.Card
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.changeover_all_prices
import pascaline.core.common.generated.resources.changeover_amount_in
import pascaline.core.common.generated.resources.changeover_amount_work
import pascaline.core.common.generated.resources.changeover_at_inflation
import pascaline.core.common.generated.resources.changeover_buys
import pascaline.core.common.generated.resources.changeover_cash
import pascaline.core.common.generated.resources.changeover_context_sources
import pascaline.core.common.generated.resources.changeover_decimal
import pascaline.core.common.generated.resources.changeover_fixed_rates
import pascaline.core.common.generated.resources.changeover_food
import pascaline.core.common.generated.resources.changeover_food_faster
import pascaline.core.common.generated.resources.changeover_food_same
import pascaline.core.common.generated.resources.changeover_food_since
import pascaline.core.common.generated.resources.changeover_food_slower
import pascaline.core.common.generated.resources.changeover_loaf_work
import pascaline.core.common.generated.resources.changeover_pay_note
import pascaline.core.common.generated.resources.changeover_pay_share
import pascaline.core.common.generated.resources.changeover_pence
import pascaline.core.common.generated.resources.changeover_pounds
import pascaline.core.common.generated.resources.changeover_shillings
import pascaline.core.common.generated.resources.changeover_sources
import pascaline.core.common.generated.resources.changeover_then_and_now
import pascaline.core.common.generated.resources.changeover_timeline_hint
import pascaline.core.common.generated.resources.changeover_title
import pascaline.core.common.generated.resources.changeover_today
import pascaline.core.common.generated.resources.changeover_year
import pascaline.core.common.generated.resources.inflation_amount
import pascaline.core.common.generated.resources.inflation_amount_in_year
import pascaline.core.common.generated.resources.inflation_in_year
import pascaline.core.common.generated.resources.unit_hour_short
import pascaline.core.common.generated.resources.unit_minute_short

/** What is typed: the amount, in pounds, shillings and pence before 1971 in the United Kingdom. */
private class Typed(val amount: String, val shillings: String, val pence: String)

/**
 * Old money of a country in the currencies that followed it, at their fixed rates, then what it is
 * worth today with the inflation since, and what it meant then and means now: pay, work and bread.
 */
@Composable
internal fun ChangeoverRoute(openDrawer: () -> Unit) {
  val symbols = rememberFormatterSymbols() ?: return
  var countryIndex by rememberSaveable { mutableIntStateOf(0) }
  val country = COUNTRIES[countryIndex]
  // Starts on the money of 1995, a year with pay, bread and the money before the euro
  var eraIndex by rememberSaveable(countryIndex) {
    mutableIntStateOf(country.eras.indexOfFirst { DEFAULT_YEAR in country.years(it) }.coerceAtLeast(0))
  }
  val era = country.eras[eraIndex.coerceIn(country.eras.indices)]
  val years = country.years(era)
  var chosenYear by rememberSaveable(countryIndex) { mutableIntStateOf(DEFAULT_YEAR) }
  val year = chosenYear.coerceIn(years)
  var amount by rememberSaveable { mutableStateOf("1000") }
  var shillings by rememberSaveable { mutableStateOf("0") }
  var pence by rememberSaveable { mutableStateOf("0") }
  fun parse(text: String) = text.replace(symbols.fractional, ".").toDoubleOrNull() ?: 0.0
  val value =
    if (era.isLsd) parse(amount) + parse(shillings) / SHILLINGS_PER_POUND + parse(pence) / (SHILLINGS_PER_POUND * PENCE_PER_SHILLING)
    else parse(amount)
  // A year picks the currency used then, unless the one chosen was still used
  fun pickYear(newYear: Int) {
    chosenYear = newYear
    if (newYear !in country.years(era)) {
      eraIndex = country.eras.indexOfFirst { newYear in country.years(it) }.coerceAtLeast(0)
    }
  }
  PhysicsScreen(
    title = Res.string.changeover_title,
    openDrawer = openDrawer,
    live = {
      CountryChips(countryIndex) { countryIndex = it }
      Result(country, era, year, value, symbols, Typed(amount, shillings, pence))
      Timeline(country, era, year, onEra = { eraIndex = it }, onYear = ::pickYear)
    },
    settings = {
      Text(stringResource(Res.string.changeover_amount_in), style = MaterialTheme.typography.titleSmall)
      Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        country.eras.forEachIndexed { index, item ->
          FilterChip(
            selected = item == era,
            onClick = { eraIndex = index },
            label = { Text("${item.symbol}  ${stringResource(item.name)}") },
          )
        }
      }
      if (era.isLsd) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          AmountField(amount, Res.string.changeover_pounds, Modifier.weight(1f)) { amount = it }
          AmountField(shillings, Res.string.changeover_shillings, Modifier.weight(1f)) { shillings = it }
          AmountField(pence, Res.string.changeover_pence, Modifier.weight(1f)) { pence = it }
        }
      } else {
        AmountField(amount, Res.string.inflation_amount, Modifier.fillMaxWidth(), suffix = era.symbol) { amount = it }
      }
      country.thenAndNow(value * country.toToday(era), year)?.let { (then, now) -> ThenAndNow(country, era, then, now, symbols) }
    },
    explanation = {
      Note(stringResource(Res.string.changeover_sources))
      Note(stringResource(Res.string.changeover_context_sources))
    },
  )
}

@Composable
private fun CountryChips(selected: Int, onSelect: (Int) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    COUNTRIES.forEachIndexed { index, country ->
      FilterChip(selected = index == selected, onClick = { onSelect(index) }, label = { Text(stringResource(country.name)) })
    }
  }
}

@Composable
private fun AmountField(value: String, label: StringResource, modifier: Modifier, suffix: String? = null, onChange: (String) -> Unit) {
  OutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onChange,
    label = { Text(stringResource(label), maxLines = 1, overflow = TextOverflow.Ellipsis) },
    suffix = suffix?.let { { Text(it) } },
    singleLine = true,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

@Composable
private fun Note(text: String) =
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

/**
 * The answer first: the amount and the year, what it is worth today with inflation, and under it the
 * same money at the fixed rates of each changeover.
 */
@Composable
private fun Result(country: Country, era: Era, year: Int, value: Double, symbols: FormatterSymbols, typed: Typed) {
  val colors = MaterialTheme.colorScheme
  val start = country.eras.indexOf(era)
  val written = if (era.isLsd) "£${typed.amount} ${typed.shillings}s ${typed.pence}d" else money(value, era.symbol, symbols)
  val converted = value * country.toToday(era)
  val today = country.history.value(converted, year, country.history.lastYear)
  Column(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(colors.primaryContainer).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(stringResource(Res.string.inflation_amount_in_year, written, year), style = MaterialTheme.typography.bodyLarge, color = colors.onPrimaryContainer)
    Text(
      "≈ " + money(today, country.eras.last().symbol, symbols),
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold,
      color = colors.onPrimaryContainer,
    )
    Text(
      stringResource(Res.string.changeover_today) + " · " + stringResource(Res.string.inflation_in_year, country.history.lastYear),
      style = MaterialTheme.typography.bodyMedium,
      color = colors.onPrimaryContainer,
    )
    val steps = country.eras.drop(start + 1)
    if (steps.isNotEmpty()) {
      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = colors.onPrimaryContainer.copy(alpha = DIVIDER_ALPHA))
      Text(stringResource(Res.string.changeover_fixed_rates), style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer)
      var amount = value
      steps.forEachIndexed { offset, next ->
        amount /= country.eras[start + offset].perNext
        Row(verticalAlignment = Alignment.Bottom) {
          Text("= " + money(amount, next.symbol, symbols), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, color = colors.onPrimaryContainer)
          Text("${next.since}", style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer)
        }
      }
    }
  }
}

/**
 * The currencies of the country one after the other. A tap on one picks it, a tap or a drag along the
 * years picks the year, and the buttons move it a year at a time.
 */
@Composable
private fun Timeline(country: Country, selected: Era, year: Int, onEra: (Int) -> Unit, onYear: (Int) -> Unit) {
  val first = country.history.baseYear
  val last = country.history.lastYear
  val spans = country.eras.mapIndexed { index, era ->
    val end = country.eras.getOrNull(index + 1)?.since?.minus(1) ?: last
    maxOf(era.since, first)..minOf(end, last)
  }
  val colors = eraColors()
  Card(stringResource(country.name)) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).height(ERA_HEIGHT)) {
      country.eras.forEachIndexed { index, era ->
        val (background, content) = colors[index % colors.size]
        Box(
          modifier =
            Modifier.weight((spans[index].last - spans[index].first + 1).coerceAtLeast(1).toFloat())
              .fillMaxHeight()
              .background(if (era == selected) background else background.copy(alpha = UNSELECTED_ALPHA))
              .clickable { onEra(index) },
          contentAlignment = Alignment.Center,
        ) {
          Text(era.symbol, color = content, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
      }
    }
    YearTrack(first, last, year, onYear)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      Text("$first", style = MaterialTheme.typography.labelSmall)
      Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onYear((year - 1).coerceAtLeast(first)) }) { Icon(Symbols.Remove, null) }
        Text(
          "${stringResource(Res.string.changeover_year)} $year",
          modifier = Modifier.width(YEAR_WIDTH),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        IconButton(onClick = { onYear((year + 1).coerceAtMost(last)) }) { Icon(Symbols.Add, null) }
      }
      Text("$last", style = MaterialTheme.typography.labelSmall)
    }
    Note(stringResource(Res.string.changeover_timeline_hint))
    country.eras.zipWithNext().forEach { (era, next) -> ChangeLine(era, next) }
  }
}

/** The years as a track with a knob at [year]: tap or drag it. */
@Composable
private fun YearTrack(first: Int, last: Int, year: Int, onYear: (Int) -> Unit) {
  val track = MaterialTheme.colorScheme.surfaceContainerHighest
  val knob = MaterialTheme.colorScheme.primary
  val pick by rememberUpdatedState(onYear)
  fun yearAt(x: Float, width: Int) = (first + (x / width * (last - first)).roundToInt()).coerceIn(first, last)
  Canvas(
    Modifier.fillMaxWidth()
      .height(TRACK_HEIGHT)
      .pointerInput(first, last) { detectTapGestures { pick(yearAt(it.x, size.width)) } }
      .pointerInput(first, last) { detectHorizontalDragGestures { change, _ -> pick(yearAt(change.position.x, size.width)) } }
  ) {
    val middle = size.height / 2
    val thickness = TRACK_THICKNESS.toPx()
    drawRoundRect(track, Offset(0f, middle - thickness / 2), Size(size.width, thickness), CornerRadius(thickness / 2))
    val x = (year - first).toFloat() / (last - first).coerceAtLeast(1) * size.width
    drawRoundRect(knob, Offset(0f, middle - thickness / 2), Size(x, thickness), CornerRadius(thickness / 2))
    drawCircle(knob, KNOB_RADIUS.toPx(), Offset(x, middle))
  }
}

/** "1960 · 100 AF = 1 F", with when coins and notes came when it isn't the same year. */
@Composable
private fun ChangeLine(era: Era, next: Era) {
  val cashYear = era.until?.plus(1)
  val text =
    if (era.isLsd) {
      "${next.since} · " + stringResource(Res.string.changeover_decimal)
    } else {
      val rate = "${next.since} · ${exact(era.perNext)} ${era.symbol} = 1 ${next.symbol}"
      if (era.fixedIn != null && cashYear != null && cashYear != era.fixedIn) rate + ", " + stringResource(Res.string.changeover_cash, cashYear)
      else rate
    }
  Note(text)
}

/**
 * What the amount meant in its year and means today: a share of a month's pay, hours of work, loaves
 * of bread, and how food prices went compared with all prices.
 */
@Composable
private fun ThenAndNow(country: Country, era: Era, then: Side, now: Side, symbols: FormatterSymbols) {
  val todaySymbol = country.eras.last().symbol
  val factor = country.toToday(era)
  val nowMoney = { value: Double? -> value?.let { money(it, todaySymbol, symbols) } ?: DASH }
  val time = workTime(symbols)
  Card(stringResource(Res.string.changeover_then_and_now)) {
    TableRow("", "${then.year}", "${now.year}", header = true)
    if (then.payShare != null) {
      TableRow(stringResource(Res.string.changeover_pay_share), percentText(then.payShare, symbols), percentText(now.payShare, symbols))
      TableRow(stringResource(Res.string.changeover_amount_work), time(then.workHours?.times(MINUTES_PER_HOUR)), time(now.workHours?.times(MINUTES_PER_HOUR)))
    }
    val bread = country.bread
    val loaf = then.loaf
    if (bread != null && loaf != null) {
      HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
      val thenLoaf = if (era.isLsd) lsd(loaf) else money(loaf / factor, era.symbol, symbols)
      TableRow(stringResource(bread.name), thenLoaf, nowMoney(now.loaf))
      TableRow(stringResource(Res.string.changeover_at_inflation), "", nowMoney(country.history.value(loaf, then.year, now.year)), muted = true)
      TableRow(stringResource(Res.string.changeover_buys), count(then.loaves, symbols), count(now.loaves, symbols))
      if (then.loafMinutes != null) TableRow(stringResource(Res.string.changeover_loaf_work), time(then.loafMinutes), time(now.loafMinutes))
    }
    country.foodRise(then.year)?.let { FoodBars(it, symbols) }
    Note(stringResource(Res.string.changeover_pay_note))
  }
}

/** Food and all prices since the year as two bars, and which rose faster. */
@Composable
private fun FoodBars(rise: FoodRise, symbols: FormatterSymbols) {
  HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
  val signed = { share: Double -> (if (share >= 0) "+" else "") + percentText(share * PERCENT_SCALE, symbols) }
  Text(
    stringResource(Res.string.changeover_food_since, rise.from, signed(rise.food), signed(rise.prices)),
    style = MaterialTheme.typography.bodyMedium,
  )
  val top = maxOf(rise.food, rise.prices, MIN_BAR_TOP)
  val colors = MaterialTheme.colorScheme
  Bar(stringResource(Res.string.changeover_food), signed(rise.food), rise.food / top, colors.tertiary)
  Bar(stringResource(Res.string.changeover_all_prices), signed(rise.prices), rise.prices / top, colors.primary)
  val verdict =
    when {
      abs(rise.food - rise.prices) < SAME_RISE -> Res.string.changeover_food_same
      rise.food > rise.prices -> Res.string.changeover_food_faster
      else -> Res.string.changeover_food_slower
    }
  Text(stringResource(verdict), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
}

/** A rise as a bar as long as its [share] of the longest, with its name and value above it. */
@Composable
private fun Bar(label: String, value: String, share: Double, color: Color) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
    }
    Box(
      Modifier.fillMaxWidth(share.toFloat().coerceIn(MIN_BAR, 1f))
        .height(BAR_HEIGHT)
        .clip(RoundedCornerShape(BAR_HEIGHT / 2))
        .background(color)
    )
  }
}

/** A line of the then and now table: a label and two values. */
@Composable
private fun TableRow(label: String, then: String, now: String, header: Boolean = false, muted: Boolean = false) {
  val style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium
  val color =
    if (header || muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
    Text(label, modifier = Modifier.weight(LABEL_WEIGHT), style = style, color = color)
    Text(then, modifier = Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End)
    Text(now, modifier = Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End, fontWeight = if (header || muted) null else FontWeight.Bold)
  }
}

/** A time of work given in minutes: 2.8 m under 10 minutes, 14 m, then hours and minutes, 3 h 05 m. */
@Composable
private fun workTime(symbols: FormatterSymbols): (Double?) -> String {
  val minute = stringResource(Res.string.unit_minute_short)
  val hour = stringResource(Res.string.unit_hour_short)
  return { minutes ->
    when {
      minutes == null || !minutes.isFinite() -> DASH
      minutes < MINUTES_WITH_DECIMAL -> number(minutes, symbols, 1) + " " + minute
      minutes < MINUTES_PER_HOUR -> number(minutes, symbols, 0) + " " + minute
      else -> {
        val total = minutes.roundToLong()
        "${total / MINUTES_PER_HOUR} $hour ${(total % MINUTES_PER_HOUR).toString().padStart(2, '0')} $minute"
      }
    }
  }
}

private fun percentText(value: Double?, symbols: FormatterSymbols) = value?.let { percent(it, symbols) } ?: DASH

private fun count(value: Double?, symbols: FormatterSymbols) = value?.let { number(it, symbols, 0) } ?: DASH

/** An amount in pounds as pounds, shillings and pence: £1 2s 6d, 1s 7d. */
internal fun lsd(pounds: Double): String {
  val pence = (pounds * SHILLINGS_PER_POUND * PENCE_PER_SHILLING).roundToLong()
  val l = pence / (SHILLINGS_PER_POUND * PENCE_PER_SHILLING)
  val s = pence / PENCE_PER_SHILLING % SHILLINGS_PER_POUND
  val d = pence % PENCE_PER_SHILLING
  return listOfNotNull("£$l".takeIf { l > 0 }, "${s}s".takeIf { s > 0 }, "${d}d".takeIf { d > 0 || pence == 0L })
    .joinToString(" ")
}

/** A container and its text color for each currency of a country, in order. */
@Composable
private fun eraColors(): List<Pair<Color, Color>> {
  val colors = MaterialTheme.colorScheme
  return listOf(
    colors.tertiaryContainer to colors.onTertiaryContainer,
    colors.secondaryContainer to colors.onSecondaryContainer,
    colors.primaryContainer to colors.onPrimaryContainer,
  )
}

/** All the digits of a fixed rate: 6.55957, 1936.27. */
private fun exact(rate: Double): String = KBigDecimal(rate.toString()).stripTrailingZeros().toPlainString().let {
  if (it.endsWith(".0")) it.dropLast(2) else it
}

/** Pounds, shillings and pence, the British money before 1971. */
private val Era.isLsd: Boolean
  get() = symbol == "£sd"

private const val DASH = "—"
private const val DEFAULT_YEAR = 1995
private const val UNSELECTED_ALPHA = 0.45f
private const val DIVIDER_ALPHA = 0.3f
private const val LABEL_WEIGHT = 1.6f
private const val PERCENT_SCALE = 100
private const val MINUTES_PER_HOUR = 60
private const val MIN_BAR_TOP = 0.01
private const val MIN_BAR = 0.02f
private const val SAME_RISE = 0.03

/** Minutes under this are written with a decimal: 2.8 m, 14 m. */
private const val MINUTES_WITH_DECIMAL = 10
private val ERA_HEIGHT = 44.dp
private val TRACK_HEIGHT = 32.dp
private val TRACK_THICKNESS = 6.dp
private val KNOB_RADIUS = 10.dp
private val YEAR_WIDTH = 120.dp
private val BAR_HEIGHT = 10.dp
