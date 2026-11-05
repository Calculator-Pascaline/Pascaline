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

package com.sadellie.unitto.feature.calculator.place

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sadellie.unitto.core.common.FormatterSymbols
import com.sadellie.unitto.core.common.OutputFormat
import com.sadellie.unitto.feature.calculator.datatable.NumberFormat
import org.jetbrains.compose.resources.stringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.unit_celsius_short
import unitto.core.common.generated.resources.unit_fahrenheit_short
import unitto.core.common.generated.resources.unit_foot_short
import unitto.core.common.generated.resources.unit_hour_short
import unitto.core.common.generated.resources.unit_inch_short
import unitto.core.common.generated.resources.unit_kilometer_short
import unitto.core.common.generated.resources.unit_meter_short
import unitto.core.common.generated.resources.unit_millimeter_short
import unitto.core.common.generated.resources.unit_minute_short

/** Symbols of the units shown, in the person's language. */
internal data class UnitLabels(
  val temperature: String,
  val length: String,
  val rain: String,
  val distance: String,
  val hour: String,
  val minute: String,
)

/** Metric or imperial units, written with the number format of the calculator. */
internal class PlaceUnits(val isMetric: Boolean, symbols: FormatterSymbols, private val labels: UnitLabels) {
  private val formats = (0..2).map { NumberFormat(symbols, it, OutputFormat.PLAIN) }

  val temperatureUnit = labels.temperature

  fun number(value: Double, decimals: Int) = formats[decimals].format(value)

  private fun shownTemperature(celsius: Double) =
    if (isMetric) celsius else celsius * FAHRENHEIT_PER_CELSIUS + FAHRENHEIT_AT_ZERO

  private fun length(meters: Double) = if (isMetric) meters else meters / FOOT

  fun temperature(celsius: Double) = "${number(shownTemperature(celsius), 0)} ${labels.temperature}"

  fun temperatureRange(low: Double, high: Double) =
    "${number(shownTemperature(low), 0)} – ${number(shownTemperature(high), 0)} ${labels.temperature}"

  fun tideHeight(meters: Double) = "${number(length(meters), 1)} ${labels.length}"

  fun elevation(meters: Double) = "${number(length(meters), 0)} ${labels.length}"

  fun rain(millimeters: Double) =
    if (isMetric) "${number(millimeters, 0)} ${labels.rain}" else "${number(millimeters / INCH, 1)} ${labels.rain}"

  fun distance(kilometers: Double) =
    "${number(if (isMetric) kilometers else kilometers / MILE_KM, 0)} ${labels.distance}"

  fun percent(fraction: Double) = "${number(fraction * PERCENT, 0)} %"

  fun degrees(value: Double) = "${number(value, 0)}°"

  /** "9 h 05 m" */
  fun duration(millis: Long): String {
    val minutes = (millis / MILLIS_PER_MINUTE).toInt()
    val hours = minutes / MINUTES_PER_HOUR
    val rest = (minutes % MINUTES_PER_HOUR).toString().padStart(2, '0')
    return "$hours ${labels.hour} $rest ${labels.minute}"
  }
}

@Composable
internal fun rememberPlaceUnits(isMetric: Boolean, symbols: FormatterSymbols): PlaceUnits {
  val labels =
    UnitLabels(
      temperature = stringResource(if (isMetric) Res.string.unit_celsius_short else Res.string.unit_fahrenheit_short),
      length = stringResource(if (isMetric) Res.string.unit_meter_short else Res.string.unit_foot_short),
      rain = stringResource(if (isMetric) Res.string.unit_millimeter_short else Res.string.unit_inch_short),
      distance = if (isMetric) stringResource(Res.string.unit_kilometer_short) else MILE,
      hour = stringResource(Res.string.unit_hour_short),
      minute = stringResource(Res.string.unit_minute_short),
    )
  return remember(isMetric, symbols, labels) { PlaceUnits(isMetric, symbols, labels) }
}

private const val FAHRENHEIT_PER_CELSIUS = 1.8
private const val FAHRENHEIT_AT_ZERO = 32.0
private const val FOOT = 0.3048
private const val INCH = 25.4
private const val MILE_KM = 1.609344
private const val PERCENT = 100
private const val MINUTES_PER_HOUR = 60
// The same in every language
private const val MILE = "mi"
