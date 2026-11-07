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

package com.sadellie.unitto.feature.calculator.finance

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.physics.LiveCard
import com.sadellie.unitto.feature.calculator.physics.PhysicsScreen
import com.sadellie.unitto.feature.calculator.physics.number
import com.sadellie.unitto.feature.calculator.physics.rememberFractional
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import com.sadellie.unitto.feature.calculator.projectile.Answer
import com.sadellie.unitto.feature.calculator.projectile.Card
import com.sadellie.unitto.feature.calculator.projectile.Condition
import com.sadellie.unitto.feature.calculator.projectile.ConditionRow
import kotlin.math.round
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.finance_amount
import unitto.core.common.generated.resources.finance_compound
import unitto.core.common.generated.resources.finance_compound_hint
import unitto.core.common.generated.resources.finance_compoundings
import unitto.core.common.generated.resources.finance_days
import unitto.core.common.generated.resources.finance_deposited
import unitto.core.common.generated.resources.finance_earned
import unitto.core.common.generated.resources.finance_final_value
import unitto.core.common.generated.resources.finance_find
import unitto.core.common.generated.resources.finance_future_value
import unitto.core.common.generated.resources.finance_incomplete
import unitto.core.common.generated.resources.finance_interest
import unitto.core.common.generated.resources.finance_interest_part
import unitto.core.common.generated.resources.finance_interest_rate
import unitto.core.common.generated.resources.finance_length
import unitto.core.common.generated.resources.finance_loan
import unitto.core.common.generated.resources.finance_monthly_deposit
import unitto.core.common.generated.resources.finance_monthly_payment
import unitto.core.common.generated.resources.finance_no_solution
import unitto.core.common.generated.resources.finance_pay_at_end
import unitto.core.common.generated.resources.finance_pay_at_start
import unitto.core.common.generated.resources.finance_payment
import unitto.core.common.generated.resources.finance_payments
import unitto.core.common.generated.resources.finance_payments_count
import unitto.core.common.generated.resources.finance_present_value
import unitto.core.common.generated.resources.finance_principal
import unitto.core.common.generated.resources.finance_rate
import unitto.core.common.generated.resources.finance_repaid
import unitto.core.common.generated.resources.finance_savings
import unitto.core.common.generated.resources.finance_simple
import unitto.core.common.generated.resources.finance_start_amount
import unitto.core.common.generated.resources.finance_title
import unitto.core.common.generated.resources.finance_total_interest
import unitto.core.common.generated.resources.finance_total_paid
import unitto.core.common.generated.resources.finance_year
import unitto.core.common.generated.resources.finance_year_number

private val AMOUNT = Condition("A", "", Res.string.finance_amount, 0.0..1_000_000.0, 1000.0)
private val RATE = Condition("r", "%", Res.string.finance_interest_rate, 0.0..20.0, 0.05)
private val LENGTH = Condition("n", "", Res.string.finance_length, 1.0..40.0, 1.0)
private val START = Condition("A", "", Res.string.finance_start_amount, 0.0..100_000.0, 100.0)
private val DEPOSIT = Condition("+", "", Res.string.finance_monthly_deposit, 0.0..5000.0, 10.0)

@Composable
internal fun FinanceRoute(openDrawer: () -> Unit) {
  val viewModel: FinanceViewModel = koinViewModel()
  val format = viewModel.numberFormat.collectAsStateWithLifecycleKMP().value
  if (format == null) {
    EmptyScreen()
    return
  }
  FinanceScreen(openDrawer, viewModel, format)
}

/** A loan or savings in plain words with what happens each year, and the NumWorks solvers. */
@Composable
private fun FinanceScreen(openDrawer: () -> Unit, viewModel: FinanceViewModel, format: NumberFormat) {
  val fractional = rememberFractional()
  PhysicsScreen(
    title = Res.string.finance_title,
    openDrawer = openDrawer,
    live = {
      ModeChips(viewModel.mode) { viewModel.mode = it }
      when (viewModel.mode) {
        FinanceMode.LOAN -> LoanResult(viewModel, format)
        FinanceMode.SAVINGS -> SavingsResult(viewModel, format)
        FinanceMode.SIMPLE -> SimpleInterest(viewModel, format)
        FinanceMode.COMPOUND -> CompoundInterest(viewModel, format)
      }
    },
    settings = {},
    explanation = {
      if (viewModel.mode == FinanceMode.LOAN) {
        Card(stringResource(Res.string.finance_loan)) {
          ConditionRow(AMOUNT, viewModel.loanAmount, fractional) { viewModel.loanAmount = it }
          ConditionRow(RATE, viewModel.loanRate, fractional) { viewModel.loanRate = it }
          ConditionRow(LENGTH, viewModel.loanYears, fractional) { viewModel.loanYears = it }
        }
      }
      if (viewModel.mode == FinanceMode.SAVINGS) {
        Card(stringResource(Res.string.finance_savings)) {
          ConditionRow(START, viewModel.savedStart, fractional) { viewModel.savedStart = it }
          ConditionRow(DEPOSIT, viewModel.savedDeposit, fractional) { viewModel.savedDeposit = it }
          ConditionRow(RATE, viewModel.savedRate, fractional) { viewModel.savedRate = it }
          ConditionRow(LENGTH, viewModel.savedYears, fractional) { viewModel.savedYears = it }
        }
      }
    },
  )
}

@Composable
private fun ModeChips(mode: FinanceMode, onChoose: (FinanceMode) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    FinanceMode.entries.forEach {
      FilterChip(selected = it == mode, onClick = { onChoose(it) }, label = { Text(stringResource(it.title)) })
    }
  }
}

private val FinanceMode.title: StringResource
  get() =
    when (this) {
      FinanceMode.LOAN -> Res.string.finance_loan
      FinanceMode.SAVINGS -> Res.string.finance_savings
      FinanceMode.SIMPLE -> Res.string.finance_simple
      FinanceMode.COMPOUND -> Res.string.finance_compound
    }

private fun money(value: Double, format: NumberFormat) = format.format(round(value * CENTS) / CENTS)

@Composable
private fun LoanResult(viewModel: FinanceViewModel, format: NumberFormat) {
  val amount = viewModel.loanAmount.number()
  val result = loan(amount, viewModel.loanRate.number(), viewModel.loanYears.number().wholeYears())
  if (result == null) {
    NoSolution()
    return
  }
  LiveCard {
    Hero(stringResource(Res.string.finance_monthly_payment), money(result.payment, format))
    YearChart(result.years, Res.string.finance_repaid, Res.string.finance_interest_part, format)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.finance_total_interest, money(result.totalInterest, format))
      Answer(Modifier.weight(1f), Res.string.finance_total_paid, money(amount + result.totalInterest, format))
    }
  }
}

@Composable
private fun SavingsResult(viewModel: FinanceViewModel, format: NumberFormat) {
  val years =
    savings(
      viewModel.savedStart.number(),
      viewModel.savedDeposit.number(),
      viewModel.savedRate.number(),
      viewModel.savedYears.number().wholeYears(),
    )
  if (years == null) {
    NoSolution()
    return
  }
  val last = years.last()
  LiveCard {
    Hero(stringResource(Res.string.finance_final_value), money(last.base + last.interest, format))
    YearChart(years, Res.string.finance_deposited, Res.string.finance_earned, format)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Answer(Modifier.weight(1f), Res.string.finance_deposited, money(last.base, format))
      Answer(Modifier.weight(1f), Res.string.finance_earned, money(last.interest, format))
    }
  }
}

/** Not a whole number of years is rounded down, nothing valid gives 0 and no result. */
private fun Double.wholeYears() = if (isFinite()) coerceIn(0.0, MAX_TYPED_YEARS).toInt() else 0

@Composable
private fun NoSolution() {
  Text(stringResource(Res.string.finance_no_solution), color = MaterialTheme.colorScheme.error)
}

@Composable
private fun Hero(name: String, value: String) {
  Column {
    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
  }
}

/** Stacked bars that grow in, tap one to read its year. */
@Composable
private fun YearChart(years: List<MoneyYear>, baseName: StringResource, interestName: StringResource, format: NumberFormat) {
  val growth = remember { Animatable(0f) }
  LaunchedEffect(Unit) { growth.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) }
  var selected by remember(years.size) { mutableStateOf<Int?>(null) }
  val base = MaterialTheme.colorScheme.primary
  val interest = MaterialTheme.colorScheme.tertiary
  Canvas(
    modifier =
      Modifier.fillMaxWidth().height(CHART_HEIGHT).pointerInput(years.size) {
        detectTapGestures { tap -> selected = (tap.x / size.width * years.size).toInt().takeIf { it in years.indices } }
      }
  ) {
    val top = years.maxOf { it.base + it.interest }.takeIf { it > 0 } ?: 1.0
    val width = size.width / years.size
    val gap = (width * BAR_GAP).coerceAtMost(MAX_GAP.toPx())
    years.forEachIndexed { index, year ->
      val isFaded = selected != null && selected != index
      val baseHeight = (year.base / top).toFloat() * size.height * growth.value
      val interestHeight = (year.interest / top).toFloat() * size.height * growth.value
      val left = index * width + gap / 2
      val barSize = Size(width - gap, baseHeight)
      drawRect(base.faded(isFaded), Offset(left, size.height - baseHeight), barSize)
      drawRect(interest.faded(isFaded), Offset(left, size.height - baseHeight - interestHeight), Size(width - gap, interestHeight))
    }
  }
  val year = selected?.let { years.getOrNull(it) }
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    if (year != null) {
      Text(
        stringResource(Res.string.finance_year_number, ((selected ?: 0) + 1).toString()),
        style = MaterialTheme.typography.labelLarge,
      )
    }
    Legend(base, stringResource(baseName), year?.let { money(it.base, format) })
    Legend(interest, stringResource(interestName), year?.let { money(it.interest, format) })
  }
}

private fun Color.faded(isFaded: Boolean) = if (isFaded) copy(alpha = FADED_ALPHA) else this

@Composable
private fun Legend(color: Color, name: String, value: String?) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    Box(Modifier.size(LEGEND_DOT).clip(CircleShape).background(color))
    Text(if (value == null) name else "$name $value", style = MaterialTheme.typography.labelMedium)
  }
}

/** I = P × r × n / 360 or 365 */
@Composable
private fun SimpleInterest(viewModel: FinanceViewModel, format: NumberFormat) {
  val unknown = viewModel.simpleUnknown
  UnknownChips(SimpleValue.entries, unknown, { viewModel.simpleUnknown = it }) { it.title }
  SimpleValue.entries.filter { it != unknown }.forEach { value ->
    NumberField(viewModel.simpleValues[value].orEmpty(), { viewModel.simpleValues[value] = it }, stringResource(value.title))
  }
  ChoiceChips(
    title = stringResource(Res.string.finance_year),
    options = listOf(DAYS_360 to "360", DAYS_365 to "365"),
    selected = viewModel.yearDays,
    onSelect = { viewModel.yearDays = it },
  )
  ResultCard(stringResource(unknown.title), viewModel.simpleResult(), isComplete = isComplete(SimpleValue.entries - unknown, viewModel.simpleValues), format)
}

/** N, I%, PV, Pmt and FV, with payments and compoundings a year. */
@Composable
private fun CompoundInterest(viewModel: FinanceViewModel, format: NumberFormat) {
  val unknown = viewModel.compoundUnknown
  Text(
    text = stringResource(Res.string.finance_compound_hint),
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  UnknownChips(CompoundValue.entries, unknown, { viewModel.compoundUnknown = it }) { it.title }
  CompoundValue.entries.filter { it != unknown }.forEach { value ->
    NumberField(viewModel.compoundValues[value].orEmpty(), { viewModel.compoundValues[value] = it }, stringResource(value.title))
  }
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    NumberField(
      viewModel.paymentsPerYear,
      { viewModel.paymentsPerYear = it },
      stringResource(Res.string.finance_payments),
      Modifier.weight(1f),
    )
    NumberField(
      viewModel.compoundingsPerYear,
      { viewModel.compoundingsPerYear = it },
      stringResource(Res.string.finance_compoundings),
      Modifier.weight(1f),
    )
  }
  ChoiceChips(
    title = null,
    options =
      listOf(false to stringResource(Res.string.finance_pay_at_end), true to stringResource(Res.string.finance_pay_at_start)),
    selected = viewModel.paysAtStart,
    onSelect = { viewModel.paysAtStart = it },
  )
  ResultCard(
    stringResource(unknown.title),
    viewModel.compoundResult(),
    isComplete = isComplete(CompoundValue.entries - unknown, viewModel.compoundValues),
    format,
  )
}

/** "Find:" and one chip per value. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> UnknownChips(values: List<T>, selected: T, onSelect: (T) -> Unit, title: (T) -> StringResource) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
    Text(stringResource(Res.string.finance_find), style = MaterialTheme.typography.titleMedium)
    values.forEach { value ->
      FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(stringResource(title(value))) })
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(title: String?, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
    if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
    options.forEach { (value, label) ->
      FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label) })
    }
  }
}

/** `Payment = −1 199.10`, or why there is no value yet. */
@Composable
private fun ResultCard(name: String, result: Double?, isComplete: Boolean, format: NumberFormat) {
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    when {
      result != null ->
        Text(format.format(result), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
      isComplete -> Text(stringResource(Res.string.finance_no_solution), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
      else -> Text(stringResource(Res.string.finance_incomplete), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

private fun <T> isComplete(values: List<T>, typed: Map<T, String>) = values.all { typed[it]?.toNumberOrNull() != null }

@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth()) {
  OutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    singleLine = true,
    isError = value.isNotBlank() && value.toNumberOrNull() == null,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

private val SimpleValue.title: StringResource
  get() =
    when (this) {
      SimpleValue.INTEREST -> Res.string.finance_interest
      SimpleValue.RATE -> Res.string.finance_rate
      SimpleValue.PRINCIPAL -> Res.string.finance_principal
      SimpleValue.DAYS -> Res.string.finance_days
    }

private val CompoundValue.title: StringResource
  get() =
    when (this) {
      CompoundValue.N -> Res.string.finance_payments_count
      CompoundValue.RATE -> Res.string.finance_rate
      CompoundValue.PRESENT_VALUE -> Res.string.finance_present_value
      CompoundValue.PAYMENT -> Res.string.finance_payment
      CompoundValue.FUTURE_VALUE -> Res.string.finance_future_value
    }

private const val CENTS = 100
private const val MAX_TYPED_YEARS = 1000.0
private const val BAR_GAP = 0.25f
private const val FADED_ALPHA = 0.3f
private val MAX_GAP = 6.dp
private val CHART_HEIGHT = 160.dp
private val LEGEND_DOT = 10.dp
private val CARD_SHAPE = RoundedCornerShape(20.dp)
