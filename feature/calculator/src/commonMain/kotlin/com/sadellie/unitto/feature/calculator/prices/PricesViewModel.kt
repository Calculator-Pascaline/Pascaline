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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sadellie.unitto.core.common.stateIn
import com.sadellie.unitto.core.datastore.UserPreferencesRepository
import com.sadellie.unitto.feature.calculator.probability.toNumberOrNull
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Offer as it is typed. */
internal class OfferItem(price: String = "", quantity: String = "", unit: QuantityUnit = QuantityUnit.GRAM) {
  var price by mutableStateOf(price)
  var quantity by mutableStateOf(quantity)
  var unit by mutableStateOf(unit)

  /** null until price and quantity are numbers. */
  fun toOffer(): Offer? {
    val price = price.toNumberOrNull() ?: return null
    val quantity = quantity.toNumberOrNull() ?: return null
    return Offer(price, quantity, unit)
  }
}

/** Offers to compare by their price per kg, per liter… */
internal class PricesViewModel(private val userPrefsRepository: UserPreferencesRepository) : ViewModel() {
  val formatterSymbols = userPrefsRepository.calculatorPrefs.map { it.formatterSymbols }.stateIn(viewModelScope, null)

  val offers = mutableStateListOf<OfferItem>()

  /** Shown with prices, the one of the region until it is changed. */
  var currency by mutableStateOf(localCurrencySymbol())

  var isLoaded by mutableStateOf(false)
    private set

  init {
    viewModelScope.launch {
      decode(userPrefsRepository.appData(DATA_KEY).first())
      while (offers.size < MIN_OFFERS) offers += OfferItem()
      isLoaded = true
      saveChanges()
    }
  }

  @OptIn(FlowPreview::class)
  private suspend fun saveChanges() {
    snapshotFlow { encode() }
      .drop(1)
      .debounce(SAVE_DELAY_MS)
      .collect { userPrefsRepository.updateAppData(DATA_KEY, it) }
  }

  /** New offer with the unit of the last one, which is often the same. */
  fun add() {
    if (offers.size < MAX_OFFERS) offers += OfferItem(unit = offers.lastOrNull()?.unit ?: QuantityUnit.GRAM)
  }

  fun delete(index: Int) {
    if (offers.size > MIN_OFFERS && index in offers.indices) offers.removeAt(index)
  }

  /** Currency on the first line, then `price⇥quantity⇥unit` per offer. */
  private fun encode(): String =
    (listOf(currency) + offers.map { listOf(it.price, it.quantity, it.unit.name).joinToString(SEPARATOR) })
      .joinToString(LINE_SEPARATOR)

  private fun decode(text: String) {
    if (text.isEmpty()) return
    val lines = text.split(LINE_SEPARATOR)
    currency = lines.first()
    lines.drop(1).forEach { line ->
      val parts = line.split(SEPARATOR)
      val unit = QuantityUnit.entries.firstOrNull { it.name == parts.getOrNull(2) } ?: QuantityUnit.GRAM
      offers += OfferItem(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, unit)
    }
  }
}

/** Symbol of the currency of the region, like € or $. */
internal expect fun localCurrencySymbol(): String

internal const val MIN_OFFERS = 2
/** One letter per offer: A to Z */
internal const val MAX_OFFERS = 26
private const val DATA_KEY = "PRICES_DATA"
private const val SEPARATOR = "\t"
private const val LINE_SEPARATOR = "\n"
private const val SAVE_DELAY_MS = 500L
