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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.finance_compound
import unitto.core.common.generated.resources.finance_compound_hint
import unitto.core.common.generated.resources.finance_compoundings
import unitto.core.common.generated.resources.finance_days
import unitto.core.common.generated.resources.finance_find
import unitto.core.common.generated.resources.finance_future_value
import unitto.core.common.generated.resources.finance_incomplete
import unitto.core.common.generated.resources.finance_interest
import unitto.core.common.generated.resources.finance_no_solution
import unitto.core.common.generated.resources.finance_pay_at_end
import unitto.core.common.generated.resources.finance_pay_at_start
import unitto.core.common.generated.resources.finance_payment
import unitto.core.common.generated.resources.finance_payments
import unitto.core.common.generated.resources.finance_payments_count
import unitto.core.common.generated.resources.finance_present_value
import unitto.core.common.generated.resources.finance_principal
import unitto.core.common.generated.resources.finance_rate
import unitto.core.common.generated.resources.finance_simple
import unitto.core.common.generated.resources.finance_title
import unitto.core.common.generated.resources.finance_year

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

/** Like the Finance app of NumWorks: simple and compound interest, any value can be found. */
@Composable
private fun FinanceScreen(openDrawer: () -> Unit, viewModel: FinanceViewModel, format: NumberFormat) {
  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.finance_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    Column(
      modifier = Modifier.padding(paddingValues).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        listOf(false to Res.string.finance_simple, true to Res.string.finance_compound).forEachIndexed { index, (isCompound, title) ->
          SegmentedButton(
            selected = viewModel.isCompound == isCompound,
            onClick = { viewModel.isCompound = isCompound },
            shape = SegmentedButtonDefaults.itemShape(index, 2),
            label = { Text(stringResource(title)) },
          )
        }
      }
      if (viewModel.isCompound) CompoundInterest(viewModel, format) else SimpleInterest(viewModel, format)
    }
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

private val CARD_SHAPE = RoundedCornerShape(20.dp)
