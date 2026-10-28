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
import com.sadellie.unitto.core.data.converter.UnitID
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.unit.BasicUnit
import com.sadellie.unitto.core.model.converter.unit.NormalUnit
import unitto.core.common.generated.resources.Res
import unitto.core.common.generated.resources.unit_attojoule
import unitto.core.common.generated.resources.unit_attojoule_short
import unitto.core.common.generated.resources.unit_calorie_th
import unitto.core.common.generated.resources.unit_calorie_th_short
import unitto.core.common.generated.resources.unit_electron_volt
import unitto.core.common.generated.resources.unit_electron_volt_short
import unitto.core.common.generated.resources.unit_energy_horse_power_metric
import unitto.core.common.generated.resources.unit_energy_horse_power_metric_short
import unitto.core.common.generated.resources.unit_energy_ton
import unitto.core.common.generated.resources.unit_energy_ton_short
import unitto.core.common.generated.resources.unit_gigajoule
import unitto.core.common.generated.resources.unit_gigajoule_short
import unitto.core.common.generated.resources.unit_gigaton
import unitto.core.common.generated.resources.unit_gigaton_short
import unitto.core.common.generated.resources.unit_gigawatt_hour
import unitto.core.common.generated.resources.unit_gigawatt_hour_short
import unitto.core.common.generated.resources.unit_joule
import unitto.core.common.generated.resources.unit_joule_short
import unitto.core.common.generated.resources.unit_kilocalorie_th
import unitto.core.common.generated.resources.unit_kilocalorie_th_short
import unitto.core.common.generated.resources.unit_kilojoule
import unitto.core.common.generated.resources.unit_kilojoule_short
import unitto.core.common.generated.resources.unit_kiloton
import unitto.core.common.generated.resources.unit_kiloton_short
import unitto.core.common.generated.resources.unit_kilowatt_hour
import unitto.core.common.generated.resources.unit_kilowatt_hour_short
import unitto.core.common.generated.resources.unit_megajoule
import unitto.core.common.generated.resources.unit_megajoule_short
import unitto.core.common.generated.resources.unit_megaton
import unitto.core.common.generated.resources.unit_megaton_short
import unitto.core.common.generated.resources.unit_megawatt_hour
import unitto.core.common.generated.resources.unit_megawatt_hour_short
import unitto.core.common.generated.resources.unit_milliwatt_hour
import unitto.core.common.generated.resources.unit_milliwatt_hour_short
import unitto.core.common.generated.resources.unit_watt_hour
import unitto.core.common.generated.resources.unit_watt_hour_short

internal val energyCollection: List<BasicUnit> by lazy {
  listOf(
    NormalUnit(
      UnitID.electron_volt,
      KBigDecimal("0.160217733"),
      UnitGroup.ENERGY,
      Res.string.unit_electron_volt,
      Res.string.unit_electron_volt_short,
    ),
    NormalUnit(
      UnitID.attojoule,
      KBigDecimal("1.00"),
      UnitGroup.ENERGY,
      Res.string.unit_attojoule,
      Res.string.unit_attojoule_short,
    ),
    NormalUnit(
      UnitID.joule,
      KBigDecimal("1000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_joule,
      Res.string.unit_joule_short,
    ),
    NormalUnit(
      UnitID.kilojoule,
      KBigDecimal("1000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_kilojoule,
      Res.string.unit_kilojoule_short,
    ),
    NormalUnit(
      UnitID.megajoule,
      KBigDecimal("1000000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_megajoule,
      Res.string.unit_megajoule_short,
    ),
    NormalUnit(
      UnitID.gigajoule,
      KBigDecimal("1000000000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_gigajoule,
      Res.string.unit_gigajoule_short,
    ),
    NormalUnit(
      UnitID.milliwatt_hour,
      KBigDecimal("3600000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_milliwatt_hour,
      Res.string.unit_milliwatt_hour_short,
    ),
    NormalUnit(
      UnitID.watt_hour,
      KBigDecimal("3600000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_watt_hour,
      Res.string.unit_watt_hour_short,
    ),
    NormalUnit(
      UnitID.kilowatt_hour,
      KBigDecimal("3600000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_kilowatt_hour,
      Res.string.unit_kilowatt_hour_short,
    ),
    NormalUnit(
      UnitID.megawatt_hour,
      KBigDecimal("3600000000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_megawatt_hour,
      Res.string.unit_megawatt_hour_short,
    ),
    NormalUnit(
      UnitID.gigawatt_hour,
      KBigDecimal("3600000000000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_gigawatt_hour,
      Res.string.unit_gigawatt_hour_short,
    ),
    NormalUnit(
      UnitID.energy_ton,
      KBigDecimal("4184000000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_energy_ton,
      Res.string.unit_energy_ton_short,
    ),
    NormalUnit(
      UnitID.kiloton,
      KBigDecimal("4184000000000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_kiloton,
      Res.string.unit_kiloton_short,
    ),
    NormalUnit(
      UnitID.megaton,
      KBigDecimal("4184000000000000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_megaton,
      Res.string.unit_megaton_short,
    ),
    NormalUnit(
      UnitID.gigaton,
      KBigDecimal("4184000000000000000000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_gigaton,
      Res.string.unit_gigaton_short,
    ),
    NormalUnit(
      UnitID.energy_horse_power_metric,
      KBigDecimal("2647795500000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_energy_horse_power_metric,
      Res.string.unit_energy_horse_power_metric_short,
    ),
    NormalUnit(
      UnitID.calorie_th,
      KBigDecimal("4184000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_calorie_th,
      Res.string.unit_calorie_th_short,
    ),
    NormalUnit(
      UnitID.kilocalorie_th,
      KBigDecimal("4184000000000000000000"),
      UnitGroup.ENERGY,
      Res.string.unit_kilocalorie_th,
      Res.string.unit_kilocalorie_th_short,
    ),
  )
}
