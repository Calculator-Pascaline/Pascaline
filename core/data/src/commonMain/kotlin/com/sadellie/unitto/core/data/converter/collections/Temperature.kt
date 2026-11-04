/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
 * Copyright (c) 2026 Calculator-Pascaline (modifications)
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

package com.sadellie.unitto.core.data.converter.collections

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.setMaxScale
import com.sadellie.unitto.core.data.converter.UnitID
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.unit.BasicUnit
import org.jetbrains.compose.resources.StringResource
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.unit_celsius
import unitto.core.common.generated.resources.unit_celsius_short
import unitto.core.common.generated.resources.unit_fahrenheit
import unitto.core.common.generated.resources.unit_fahrenheit_short
import unitto.core.common.generated.resources.unit_kelvin
import unitto.core.common.generated.resources.unit_kelvin_short
import unitto.core.common.generated.resources.unit_rankine
import unitto.core.common.generated.resources.unit_rankine_short

/** Temperatures go through kelvin: each unit only says how it turns into kelvin and back. */
private class TemperatureUnit(
  override val id: String,
  override val displayName: StringResource,
  override val shortName: StringResource,
  val toKelvin: (KBigDecimal) -> KBigDecimal,
  val fromKelvin: (KBigDecimal) -> KBigDecimal,
) : BasicUnit.Default {
  override val group: UnitGroup = UnitGroup.TEMPERATURE
  override val factor: KBigDecimal = KBigDecimal.ONE
  override val backward: Boolean = false

  override fun convert(unitTo: BasicUnit.Default, value: KBigDecimal): KBigDecimal {
    if (unitTo.id == id || unitTo !is TemperatureUnit) return value
    return unitTo.fromKelvin(toKelvin(value.setMaxScale()))
  }
}

private val ABSOLUTE_ZERO_CELSIUS = KBigDecimal("273.15")
private val ABSOLUTE_ZERO_FAHRENHEIT = KBigDecimal("459.67")
private val FIVE = KBigDecimal("5")
private val NINE = KBigDecimal("9")
private val NINE_FIFTHS = KBigDecimal("1.8")

internal val temperatureCollection: List<BasicUnit> by lazy {
  listOf(
    TemperatureUnit(
      id = UnitID.celsius,
      displayName = Res.string.unit_celsius,
      shortName = Res.string.unit_celsius_short,
      toKelvin = { it.plus(ABSOLUTE_ZERO_CELSIUS) },
      fromKelvin = { it.minus(ABSOLUTE_ZERO_CELSIUS) },
    ),
    TemperatureUnit(
      id = UnitID.fahrenheit,
      displayName = Res.string.unit_fahrenheit,
      shortName = Res.string.unit_fahrenheit_short,
      toKelvin = { it.plus(ABSOLUTE_ZERO_FAHRENHEIT).times(FIVE).div(NINE) },
      fromKelvin = { it.times(NINE_FIFTHS).minus(ABSOLUTE_ZERO_FAHRENHEIT) },
    ),
    TemperatureUnit(
      id = UnitID.kelvin,
      displayName = Res.string.unit_kelvin,
      shortName = Res.string.unit_kelvin_short,
      toKelvin = { it },
      fromKelvin = { it },
    ),
    // Fahrenheit degrees from absolute zero (issue #265)
    TemperatureUnit(
      id = UnitID.rankine,
      displayName = Res.string.unit_rankine,
      shortName = Res.string.unit_rankine_short,
      toKelvin = { it.times(FIVE).div(NINE) },
      fromKelvin = { it.times(NINE_FIFTHS) },
    ),
  )
}
