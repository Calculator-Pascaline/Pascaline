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
import pascaline.core.common.generated.resources.unit_attofarad
import pascaline.core.common.generated.resources.unit_attofarad_short
import pascaline.core.common.generated.resources.unit_exafarad
import pascaline.core.common.generated.resources.unit_exafarad_short
import pascaline.core.common.generated.resources.unit_farad
import pascaline.core.common.generated.resources.unit_farad_short
import pascaline.core.common.generated.resources.unit_gigafarad
import pascaline.core.common.generated.resources.unit_gigafarad_short
import pascaline.core.common.generated.resources.unit_kilofarad
import pascaline.core.common.generated.resources.unit_kilofarad_short
import pascaline.core.common.generated.resources.unit_megafarad
import pascaline.core.common.generated.resources.unit_megafarad_short
import pascaline.core.common.generated.resources.unit_microfarad
import pascaline.core.common.generated.resources.unit_microfarad_short
import pascaline.core.common.generated.resources.unit_millifarad
import pascaline.core.common.generated.resources.unit_millifarad_short
import pascaline.core.common.generated.resources.unit_nanofarad
import pascaline.core.common.generated.resources.unit_nanofarad_short
import pascaline.core.common.generated.resources.unit_petafarad
import pascaline.core.common.generated.resources.unit_petafarad_short
import pascaline.core.common.generated.resources.unit_picofarad
import pascaline.core.common.generated.resources.unit_picofarad_short
import pascaline.core.common.generated.resources.unit_statfarad
import pascaline.core.common.generated.resources.unit_statfarad_short

internal val electrostaticCapacitance: List<BasicUnit> by lazy {
  listOf(
    NormalUnit(
      UnitID.attofarad,
      KBigDecimal("1"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_attofarad,
      Res.string.unit_attofarad_short,
    ),
    NormalUnit(
      UnitID.picofarad,
      KBigDecimal("1000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_picofarad,
      Res.string.unit_picofarad_short,
    ),
    NormalUnit(
      UnitID.statfarad,
      KBigDecimal("1112650.0561"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_statfarad,
      Res.string.unit_statfarad_short,
    ),
    NormalUnit(
      UnitID.nanofarad,
      KBigDecimal("1000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_nanofarad,
      Res.string.unit_nanofarad_short,
    ),
    NormalUnit(
      UnitID.microfarad,
      KBigDecimal("1000000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_microfarad,
      Res.string.unit_microfarad_short,
    ),
    NormalUnit(
      UnitID.millifarad,
      KBigDecimal("1000000000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_millifarad,
      Res.string.unit_millifarad_short,
    ),
    NormalUnit(
      UnitID.farad,
      KBigDecimal("1000000000000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_farad,
      Res.string.unit_farad_short,
    ),
    NormalUnit(
      UnitID.kilofarad,
      KBigDecimal("1000000000000000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_kilofarad,
      Res.string.unit_kilofarad_short,
    ),
    NormalUnit(
      UnitID.megafarad,
      KBigDecimal("1000000000000000000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_megafarad,
      Res.string.unit_megafarad_short,
    ),
    NormalUnit(
      UnitID.gigafarad,
      KBigDecimal("1000000000000000000000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_gigafarad,
      Res.string.unit_gigafarad_short,
    ),
    NormalUnit(
      UnitID.petafarad,
      KBigDecimal("1000000000000000000000000000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_petafarad,
      Res.string.unit_petafarad_short,
    ),
    NormalUnit(
      UnitID.exafarad,
      KBigDecimal("1000000000000000000000000000000000000"),
      UnitGroup.ELECTROSTATIC_CAPACITANCE,
      Res.string.unit_exafarad,
      Res.string.unit_exafarad_short,
    ),
  )
}
