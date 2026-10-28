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

package com.sadellie.unitto.feature.calculator.prices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.collectAsStateWithLifecycleKMP
import com.sadellie.unitto.core.designsystem.icons.symbols.Add
import com.sadellie.unitto.core.designsystem.icons.symbols.Delete
import com.sadellie.unitto.core.designsystem.icons.symbols.Symbols
import com.sadellie.unitto.core.ui.DrawerButton
import com.sadellie.unitto.core.ui.EmptyScreen
import com.sadellie.unitto.core.ui.ScaffoldWithTopBar
import com.sadellie.unitto.core.ui.textfield.formatExpression
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.prices_add
import unitto.core.common.generated.resources.prices_cheaper
import unitto.core.common.generated.resources.prices_currency
import unitto.core.common.generated.resources.prices_delete
import unitto.core.common.generated.resources.prices_different_kinds
import unitto.core.common.generated.resources.prices_not_enough
import unitto.core.common.generated.resources.prices_offer
import unitto.core.common.generated.resources.prices_other
import unitto.core.common.generated.resources.prices_per
import unitto.core.common.generated.resources.prices_piece
import unitto.core.common.generated.resources.prices_price
import unitto.core.common.generated.resources.prices_quantity
import unitto.core.common.generated.resources.prices_same
import unitto.core.common.generated.resources.prices_title
import unitto.core.common.generated.resources.prices_unit

@Composable
internal fun PricesRoute(openDrawer: () -> Unit) {
  val viewModel: PricesViewModel = koinViewModel()
  val formatterSymbols = viewModel.formatterSymbols.collectAsStateWithLifecycleKMP().value
  if (formatterSymbols == null || !viewModel.isLoaded) {
    EmptyScreen()
    return
  }
  PricesScreen(openDrawer, viewModel, formatterSymbols)
}

/** Offers with their price and quantity, and which one is the cheapest per kg, liter… */
@Composable
private fun PricesScreen(openDrawer: () -> Unit, viewModel: PricesViewModel, formatterSymbols: FormatterSymbols) {
  val offers = viewModel.offers
  val result = compare(offers.map { it.toOffer() })
  val money = { value: Double -> money(value, viewModel.currency, formatterSymbols) }

  ScaffoldWithTopBar(
    title = { Text(stringResource(Res.string.prices_title)) },
    navigationIcon = { DrawerButton(onClick = openDrawer) },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier.padding(paddingValues).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      item {
        OutlinedTextField(
          modifier = Modifier.width(CURRENCY_WIDTH),
          value = viewModel.currency,
          onValueChange = { viewModel.currency = it.take(MAX_CURRENCY_LENGTH) },
          label = { Text(stringResource(Res.string.prices_currency)) },
          singleLine = true,
        )
      }
      itemsIndexed(offers) { index, offer ->
        OfferCard(
          letter = letter(index),
          offer = offer,
          currency = viewModel.currency,
          isCheapest = (result as? ComparisonResult.Compared)?.comparison?.let { it.cheapest == index && !it.isTie } == true,
          canDelete = offers.size > MIN_OFFERS,
          onDelete = { viewModel.delete(index) },
        )
      }
      if (offers.size < MAX_OFFERS) {
        item {
          TextButton(onClick = viewModel::add) {
            Icon(Symbols.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.prices_add))
          }
        }
      }
      item { ResultCard(result, money, formatterSymbols) }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferCard(
  letter: String,
  offer: OfferItem,
  currency: String,
  isCheapest: Boolean,
  canDelete: Boolean,
  onDelete: () -> Unit,
) {
  val background =
    if (isCheapest) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
  Column(
    modifier = Modifier.fillMaxWidth().clip(CARD_SHAPE).background(background).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        modifier = Modifier.weight(1f),
        text = stringResource(Res.string.prices_offer, letter),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      if (canDelete) {
        IconButton(onClick = onDelete) { Icon(Symbols.Delete, stringResource(Res.string.prices_delete)) }
      }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      NumberField(
        modifier = Modifier.weight(1f),
        value = offer.price,
        onValueChange = { offer.price = it },
        label = stringResource(Res.string.prices_price),
        // Same side as in the results: $2.50, 2.50 €
        prefix = currency.takeIf { it in CURRENCIES_FIRST },
        suffix = currency.takeIf { it !in CURRENCIES_FIRST },
      )
      NumberField(
        modifier = Modifier.weight(1f),
        value = offer.quantity,
        onValueChange = { offer.quantity = it },
        label = stringResource(Res.string.prices_quantity),
      )
      var expanded by remember { mutableStateOf(false) }
      ExposedDropdownMenuBox(modifier = Modifier.weight(1f), expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
          modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
          value = offer.unit.symbol,
          onValueChange = {},
          readOnly = true,
          singleLine = true,
          label = { Text(stringResource(Res.string.prices_unit)) },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          QuantityUnit.entries.forEach { unit ->
            DropdownMenuItem(
              text = { Text(unit.symbol) },
              onClick = {
                offer.unit = unit
                expanded = false
              },
            )
          }
        }
      }
    }
  }
}

/** Cheapest offer and how much is saved compared to every other one. */
@Composable
private fun ResultCard(result: ComparisonResult, money: (Double) -> String, formatterSymbols: FormatterSymbols) {
  Column(
    modifier =
      Modifier.fillMaxWidth().clip(CARD_SHAPE).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    when (result) {
      is ComparisonResult.Problem ->
        Text(
          text =
            stringResource(
              when (result.problem) {
                ComparisonProblem.NOT_ENOUGH_OFFERS -> Res.string.prices_not_enough
                ComparisonProblem.DIFFERENT_KINDS -> Res.string.prices_different_kinds
              }
            ),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      is ComparisonResult.Compared -> {
        val comparison = result.comparison
        val unit = if (comparison.kind == QuantityKind.COUNT) stringResource(Res.string.prices_piece) else comparison.kind.baseSymbol
        @Composable fun perUnit(value: Double) = stringResource(Res.string.prices_per, money(value), unit)
        Text(
          text =
            if (comparison.isTie) stringResource(Res.string.prices_same)
            else stringResource(Res.string.prices_cheaper, letter(comparison.cheapest)),
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary,
        )
        Text(perUnit(comparison.unitPrices[comparison.cheapest]), style = MaterialTheme.typography.titleMedium)
        if (comparison.isTie) return@Column
        comparison.unitPrices.forEachIndexed { index, price ->
          if (index == comparison.cheapest || price.isNaN()) return@forEachIndexed
          Text(
            text =
              stringResource(
                Res.string.prices_other,
                letter(index),
                perUnit(price),
                perUnit(comparison.savingPerUnit(index)),
                percent(comparison.savingShare(index), formatterSymbols),
              ),
            style = MaterialTheme.typography.bodyLarge,
          )
        }
      }
    }
  }
}

@Composable
private fun NumberField(
  modifier: Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  prefix: String? = null,
  suffix: String? = null,
) {
  OutlinedTextField(
    modifier = modifier,
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, maxLines = 1) },
    prefix = prefix?.let { { Text(it) } },
    suffix = suffix?.let { { Text(it) } },
    singleLine = true,
    isError = value.isNotBlank() && value.toNumberOrNull() == null,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
  )
}

/** A for the first offer, B for the second… */
private fun letter(index: Int): String = ('A' + index).toString()

/** 2 decimals, 4 for small prices like 0.0042 per gram. Symbols like $ go first, others after. */
private fun money(value: Double, currency: String, formatterSymbols: FormatterSymbols): String {
  if (!value.isFinite()) return "—"
  val decimals = if (abs(value) >= SMALL_PRICE || value == 0.0) PRICE_DECIMALS else SMALL_PRICE_DECIMALS
  val number =
    KBigDecimal.valueOf(value).setScale(decimals, KRoundingMode.HALF_EVEN).toPlainString().formatExpression(formatterSymbols)
  return if (currency in CURRENCIES_FIRST) currency + number else "$number $currency"
}

private fun percent(share: Double, formatterSymbols: FormatterSymbols): String =
  KBigDecimal.valueOf(share * 100).setScale(1, KRoundingMode.HALF_EVEN).toPlainString().formatExpression(formatterSymbols) +
    " %"

private val CURRENCIES_FIRST = setOf("$", "US$", "£", "¥", "₹", "₩", "₦", "₱", "R$", "A$", "C$")
private const val SMALL_PRICE = 0.1
private const val PRICE_DECIMALS = 2
private const val SMALL_PRICE_DECIMALS = 4
private const val MAX_CURRENCY_LENGTH = 5
private val CURRENCY_WIDTH = 140.dp
private val CARD_SHAPE = RoundedCornerShape(20.dp)
