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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.ui.textfield.formatExpression
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.projectile.Card
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.changeover_amount_in
import unitto.core.common.generated.resources.changeover_buys
import unitto.core.common.generated.resources.changeover_cash
import unitto.core.common.generated.resources.changeover_context_sources
import unitto.core.common.generated.resources.changeover_decimal
import unitto.core.common.generated.resources.changeover_pay
import unitto.core.common.generated.resources.changeover_pay_note
import unitto.core.common.generated.resources.changeover_pay_share
import unitto.core.common.generated.resources.changeover_pence
import unitto.core.common.generated.resources.changeover_pounds
import unitto.core.common.generated.resources.changeover_shillings
import unitto.core.common.generated.resources.changeover_sources
import unitto.core.common.generated.resources.changeover_then_and_now
import unitto.core.common.generated.resources.changeover_title
import unitto.core.common.generated.resources.changeover_today
import unitto.core.common.generated.resources.changeover_year
import unitto.core.common.generated.resources.inflation_amount
import unitto.core.common.generated.resources.inflation_amount_in_year
import unitto.core.common.generated.resources.inflation_in_year

/**
 * Old money of a country in the currencies that followed it, at their fixed rates, then what it is
 * worth today with the inflation since.
 */
@Composable
internal fun ChangeoverRoute(openDrawer: () -> Unit) {
  val symbols = rememberFormatterSymbols() ?: return
  var countryIndex by rememberSaveable { mutableIntStateOf(0) }
  val country = COUNTRIES[countryIndex]
  var eraIndex by rememberSaveable(countryIndex) { mutableIntStateOf(0) }
  val era = country.eras[eraIndex.coerceIn(country.eras.indices)]
  val years = country.years(era)
  var chosenYear by rememberSaveable(countryIndex, eraIndex) { mutableIntStateOf((years.first + years.last) / 2) }
  val year = chosenYear.coerceIn(years)
  var amount by rememberSaveable { mutableStateOf("1000") }
  var shillings by rememberSaveable { mutableStateOf("0") }
  var pence by rememberSaveable { mutableStateOf("0") }
  fun parse(text: String) = text.replace(symbols.fractional, ".").toDoubleOrNull() ?: 0.0
  val value =
    if (era.isLsd) parse(amount) + parse(shillings) / SHILLINGS_PER_POUND + parse(pence) / (SHILLINGS_PER_POUND * PENCE_PER_SHILLING)
    else parse(amount)
  PhysicsScreen(
    title = Res.string.changeover_title,
    openDrawer = openDrawer,
    live = {
      CountryChips(countryIndex) { countryIndex = it }
      Timeline(country, era, year)
      Steps(country, era, year, value, symbols, lsd = Triple(amount, shillings, pence))
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
      Text("${stringResource(Res.string.changeover_year)}: $year", style = MaterialTheme.typography.titleSmall)
      if (years.last > years.first) {
        Slider(
          value = year.toFloat(),
          onValueChange = { chosenYear = it.roundToInt() },
          valueRange = years.first.toFloat()..years.last.toFloat(),
        )
      }
    },
    explanation = {
      Text(
        stringResource(Res.string.changeover_sources),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        stringResource(Res.string.changeover_context_sources),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
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
private fun AmountField(
  value: String,
  label: org.jetbrains.compose.resources.StringResource,
  modifier: Modifier,
  suffix: String? = null,
  onChange: (String) -> Unit,
) {
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

/** The currencies of the country one after the other, with a mark at [year]. */
@Composable
private fun Timeline(country: Country, selected: Era, year: Int) {
  val first = country.history.baseYear
  val last = country.history.lastYear
  val spans = country.eras.mapIndexed { index, era ->
    val end = country.eras.getOrNull(index + 1)?.since?.minus(1) ?: last
    maxOf(era.since, first)..minOf(end, last)
  }
  val colors = eraColors()
  Card(stringResource(country.name)) {
    Box(modifier = Modifier.fillMaxWidth().height(TIMELINE_HEIGHT).clip(RoundedCornerShape(8.dp))) {
      Row(modifier = Modifier.fillMaxSize()) {
        country.eras.forEachIndexed { index, era ->
          val (background, content) = colors[index % colors.size]
          Box(
            modifier =
              Modifier.weight((spans[index].last - spans[index].first + 1).coerceAtLeast(1).toFloat())
                .fillMaxHeight()
                .background(if (era == selected) background else background.copy(alpha = UNSELECTED_ALPHA)),
            contentAlignment = Alignment.Center,
          ) {
            Text(era.symbol, color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
          }
        }
      }
      val mark = MaterialTheme.colorScheme.onSurface
      Canvas(Modifier.fillMaxSize()) {
        val x = (year - first + 0.5f) / (last - first + 1) * size.width
        drawLine(mark, Offset(x, 0f), Offset(x, size.height), strokeWidth = MARK_WIDTH.toPx())
      }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
      Text("$first", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
      Text("$last", style = MaterialTheme.typography.labelSmall)
    }
    country.eras.zipWithNext().forEach { (era, next) -> ChangeLine(era, next) }
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
  Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/**
 * The amount in each currency that followed, then what it is worth today. Inflation is the one of
 * the country, before and after the changeovers.
 */
@Composable
private fun Steps(country: Country, era: Era, year: Int, value: Double, symbols: FormatterSymbols, lsd: Triple<String, String, String>) {
  val colors = MaterialTheme.colorScheme
  val start = country.eras.indexOf(era)
  val written = if (era.isLsd) "£${lsd.first} ${lsd.second}s ${lsd.third}d" else money(value, era.symbol, symbols)
  var converted = value
  Column(
    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surfaceContainer).padding(14.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(stringResource(Res.string.inflation_amount_in_year, written, year), style = MaterialTheme.typography.bodyMedium)
    country.eras.drop(start + 1).forEachIndexed { offset, next ->
      converted /= country.eras[start + offset].perNext
      Row(verticalAlignment = Alignment.Bottom) {
        Text("= " + money(converted, next.symbol, symbols), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        Text("${next.since}", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
      }
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
    Text(stringResource(Res.string.changeover_today), style = MaterialTheme.typography.labelLarge, color = colors.primary)
    val today = country.history.value(converted, year, country.history.lastYear)
    Text(
      "≈ " + money(today, country.eras.last().symbol, symbols),
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = colors.primary,
    )
    Text(stringResource(Res.string.inflation_in_year, country.history.lastYear), style = MaterialTheme.typography.bodySmall)
    country.thenAndNow(converted, year)?.let { (then, now) -> ThenAndNow(country, era, then, now, symbols) }
  }
}

/** Bread and pay in [then] and today, with what the amount was worth of each. */
@Composable
private fun ThenAndNow(country: Country, era: Era, then: Side, now: Side, symbols: FormatterSymbols) {
  val factor = country.toToday(era)
  val todaySymbol = country.eras.last().symbol
  val thenMoney = { value: Double?, decimals: Int ->
    value?.let { if (era.isLsd) lsd(it) else money(it / factor, era.symbol, symbols, decimals) } ?: "—"
  }
  val nowMoney = { value: Double?, decimals: Int -> value?.let { money(it, todaySymbol, symbols, decimals) } ?: "—" }
  val count = { value: Double? -> value?.let { number(it, symbols, 0) } ?: "—" }
  val share = { value: Double? -> value?.let { percent(it, symbols) } ?: "—" }
  HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
  Text(
    stringResource(Res.string.changeover_then_and_now),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.primary,
  )
  ContextRow("", "${then.year}", "${now.year}", header = true)
  val bread = country.bread
  if (bread != null && then.loaf != null) {
    ContextRow(stringResource(bread.name), thenMoney(then.loaf, MONEY_DECIMALS), nowMoney(now.loaf, MONEY_DECIMALS))
    ContextRow(stringResource(Res.string.changeover_buys), count(then.loaves), count(now.loaves))
  }
  if (then.monthlyPay != null) {
    ContextRow(stringResource(Res.string.changeover_pay), thenMoney(then.monthlyPay, 0), nowMoney(now.monthlyPay, 0))
    ContextRow(stringResource(Res.string.changeover_pay_share), share(then.payShare), share(now.payShare))
    Text(
      stringResource(Res.string.changeover_pay_note),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** A line of the then and now table: a label and two values. */
@Composable
private fun ContextRow(label: String, then: String, now: String, header: Boolean = false) {
  val style = if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium
  val color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(label, modifier = Modifier.weight(LABEL_WEIGHT), style = style, color = color)
    Text(then, modifier = Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End)
    Text(now, modifier = Modifier.weight(1f), style = style, color = color, textAlign = TextAlign.End)
  }
}

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

private const val UNSELECTED_ALPHA = 0.45f
private const val LABEL_WEIGHT = 1.4f
private const val MONEY_DECIMALS = 2
private val TIMELINE_HEIGHT = 40.dp
private val MARK_WIDTH = 2.dp
