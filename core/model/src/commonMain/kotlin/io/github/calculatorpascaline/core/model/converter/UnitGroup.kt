/*
 * Unitto is a calculator for Android
 * Copyright (c) 2022-2025 Elshan Agaev
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

package io.github.calculatorpascaline.core.model.converter

import org.jetbrains.compose.resources.StringResource
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.unit_group_acceleration
import pascaline.core.common.generated.resources.unit_group_angle
import pascaline.core.common.generated.resources.unit_group_area
import pascaline.core.common.generated.resources.unit_group_data
import pascaline.core.common.generated.resources.unit_group_data_transfer
import pascaline.core.common.generated.resources.unit_group_electrostatic_capacitance
import pascaline.core.common.generated.resources.unit_group_energy
import pascaline.core.common.generated.resources.unit_group_flow_rate
import pascaline.core.common.generated.resources.unit_group_flux
import pascaline.core.common.generated.resources.unit_group_force
import pascaline.core.common.generated.resources.unit_group_fuel_consumption
import pascaline.core.common.generated.resources.unit_group_length
import pascaline.core.common.generated.resources.unit_group_luminance
import pascaline.core.common.generated.resources.unit_group_mass
import pascaline.core.common.generated.resources.unit_group_number_base
import pascaline.core.common.generated.resources.unit_group_power
import pascaline.core.common.generated.resources.unit_group_prefix
import pascaline.core.common.generated.resources.unit_group_pressure
import pascaline.core.common.generated.resources.unit_group_speed
import pascaline.core.common.generated.resources.unit_group_temperature
import pascaline.core.common.generated.resources.unit_group_time
import pascaline.core.common.generated.resources.unit_group_torque
import pascaline.core.common.generated.resources.unit_group_volume

enum class UnitGroup(val res: StringResource) {
  // NOTE: This order is used as default for new users
  LENGTH(res = Res.string.unit_group_length),
  MASS(res = Res.string.unit_group_mass),
  SPEED(res = Res.string.unit_group_speed),
  TEMPERATURE(res = Res.string.unit_group_temperature),
  AREA(res = Res.string.unit_group_area),
  TIME(res = Res.string.unit_group_time),
  VOLUME(res = Res.string.unit_group_volume),
  DATA(res = Res.string.unit_group_data),
  PRESSURE(res = Res.string.unit_group_pressure),
  ACCELERATION(res = Res.string.unit_group_acceleration),
  ENERGY(res = Res.string.unit_group_energy),
  POWER(res = Res.string.unit_group_power),
  ANGLE(res = Res.string.unit_group_angle),
  DATA_TRANSFER(res = Res.string.unit_group_data_transfer),
  FLUX(res = Res.string.unit_group_flux),
  NUMBER_BASE(res = Res.string.unit_group_number_base),
  ELECTROSTATIC_CAPACITANCE(res = Res.string.unit_group_electrostatic_capacitance),
  PREFIX(res = Res.string.unit_group_prefix),
  FORCE(res = Res.string.unit_group_force),
  TORQUE(res = Res.string.unit_group_torque),
  FLOW_RATE(res = Res.string.unit_group_flow_rate),
  LUMINANCE(res = Res.string.unit_group_luminance),
  FUEL_CONSUMPTION(res = Res.string.unit_group_fuel_consumption),
}
