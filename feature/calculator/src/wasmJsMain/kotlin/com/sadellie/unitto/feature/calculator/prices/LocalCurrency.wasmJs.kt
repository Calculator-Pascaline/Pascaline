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

import com.sadellie.unitto.core.common.BrowserLocale
import kotlinx.browser.window

// The browser says the region but not its currency, it can be changed on the screen
internal actual fun localCurrencySymbol(): String {
  val region = BrowserLocale.region() ?: return "$"
  val code = currencies.entries.firstOrNull { region in it.value }?.key ?: return "$"
  return BrowserLocale.currencySymbol(window.navigator.language, code) ?: "$"
}

// Currency of each region (ISO 4217 to ISO 3166), the most used ones
private val currencies: Map<String, List<String>> =
  listOf(
      "EUR" to "AT BE CY DE EE ES FI FR GR HR IE IT LT LU LV MT NL PT SI SK AD MC SM VA ME XK",
      "USD" to "US EC SV PA PR TL",
      "GBP" to "GB",
      "JPY" to "JP",
      "CNY" to "CN",
      "INR" to "IN",
      "RUB" to "RU",
      "TRY" to "TR",
      "BRL" to "BR",
      "IDR" to "ID",
      "HUF" to "HU",
      "CHF" to "CH LI",
      "CAD" to "CA",
      "AUD" to "AU",
      "NZD" to "NZ",
      "MXN" to "MX",
      "KRW" to "KR",
      "PLN" to "PL",
      "CZK" to "CZ",
      "SEK" to "SE",
      "NOK" to "NO",
      "DKK" to "DK",
      "RON" to "RO",
      "BGN" to "BG",
      "UAH" to "UA",
      "ZAR" to "ZA",
      "NGN" to "NG",
      "EGP" to "EG",
      "SAR" to "SA",
      "AED" to "AE",
      "ILS" to "IL",
      "ARS" to "AR",
      "CLP" to "CL",
      "COP" to "CO",
      "PEN" to "PE",
      "PHP" to "PH",
      "THB" to "TH",
      "VND" to "VN",
      "MYR" to "MY",
      "SGD" to "SG",
      "HKD" to "HK",
      "TWD" to "TW",
      "PKR" to "PK",
      "BDT" to "BD",
      "KZT" to "KZ",
      "MAD" to "MA",
      "DZD" to "DZ",
      "TND" to "TN",
      "KES" to "KE",
      "RSD" to "RS",
      "ISK" to "IS",
      "BYN" to "BY",
    )
    .associate { (code, regions) -> code to regions.split(' ') }
