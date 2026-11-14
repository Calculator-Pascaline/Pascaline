/*
 * Unitto is a calculator for Android
 * Copyright (c) 2023-2025 Elshan Agaev
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

package io.github.calculatorpascaline.core.data.converter.collections

import io.github.calculatorpascaline.core.common.KBigDecimal
import io.github.calculatorpascaline.core.data.converter.UnitID
import io.github.calculatorpascaline.core.model.converter.UnitGroup
import io.github.calculatorpascaline.core.model.converter.unit.BasicUnit
import io.github.calculatorpascaline.core.model.converter.unit.NormalUnit
import pascaline.core.common.generated.resources.Res
import pascaline.core.common.generated.resources.unit_angle_minute
import pascaline.core.common.generated.resources.unit_angle_minute_short
import pascaline.core.common.generated.resources.unit_angle_second
import pascaline.core.common.generated.resources.unit_angle_second_short
import pascaline.core.common.generated.resources.unit_degree
import pascaline.core.common.generated.resources.unit_degree_short
import pascaline.core.common.generated.resources.unit_radian
import pascaline.core.common.generated.resources.unit_radian_short
import pascaline.core.common.generated.resources.unit_sextant
import pascaline.core.common.generated.resources.unit_sextant_short
import pascaline.core.common.generated.resources.unit_turn
import pascaline.core.common.generated.resources.unit_turn_short

internal val angleCollection: List<BasicUnit> by lazy {
  listOf(
    NormalUnit(
      UnitID.angle_second,
      KBigDecimal("1"),
      UnitGroup.ANGLE,
      Res.string.unit_angle_second,
      Res.string.unit_angle_second_short,
    ),
    NormalUnit(
      UnitID.angle_minute,
      KBigDecimal("60"),
      UnitGroup.ANGLE,
      Res.string.unit_angle_minute,
      Res.string.unit_angle_minute_short,
    ),
    NormalUnit(
      UnitID.degree,
      KBigDecimal("3600"),
      UnitGroup.ANGLE,
      Res.string.unit_degree,
      Res.string.unit_degree_short,
    ),
    NormalUnit(
      UnitID.radian,
      KBigDecimal("206264.8062471"),
      UnitGroup.ANGLE,
      Res.string.unit_radian,
      Res.string.unit_radian_short,
    ),
    NormalUnit(
      UnitID.sextant,
      KBigDecimal("216000"),
      UnitGroup.ANGLE,
      Res.string.unit_sextant,
      Res.string.unit_sextant_short,
    ),
    NormalUnit(
      UnitID.turn,
      KBigDecimal("1296000"),
      UnitGroup.ANGLE,
      Res.string.unit_turn,
      Res.string.unit_turn_short,
    ),
  )
}
