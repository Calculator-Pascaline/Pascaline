/*
 * Unitto is a calculator for Android
 * Copyright (c) 2024-2026 Elshan Agaev
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

package io.github.calculatorpascaline.core.data.converter

import io.github.calculatorpascaline.core.common.KBigDecimal
import io.github.calculatorpascaline.core.common.KRoundingMode
import io.github.calculatorpascaline.core.common.setMaxScale
import io.github.calculatorpascaline.core.data.converter.collections.lengthCollection
import io.github.calculatorpascaline.core.database.CurrencyRatesDaoInMemory
import io.github.calculatorpascaline.core.database.UnitsDaoInMemory
import io.github.calculatorpascaline.core.model.converter.UnitGroup
import io.github.calculatorpascaline.core.model.converter.UnitsListSorting
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitRepositoryFilterUnitsAndBatchConvertTest {
  private val fakeUnitsDao = UnitsDaoInMemory()
  private val unitsRepository = UnitsRepository(fakeUnitsDao)
  private val unitConverterRepo =
    UnitConverterRepositoryImpl(unitsRepository)

  @Test
  fun filterUnitsAndBatchConvert_convertWithoutIssues() = runTest {
    unitConverterRepo.favorite(UnitID.meter)

    val expected =
      mapOf(
        UnitGroup.LENGTH to
          listOf(
            UnitSearchResultItem(
              lengthCollection.first { it.id == UnitID.meter },
              UnitStats(UnitID.meter, isFavorite = true, frequency = 0),
              ConverterResult.Default(
                value = KBigDecimal("12000").setMaxScale(),
                calculation = KBigDecimal("12").setMaxScale(),
              ),
            )
          )
      )

    val actual =
      unitConverterRepo.filterUnitsAndBatchConvert(
        query = "",
        unitGroup = UnitGroup.LENGTH,
        favoritesOnly = true,
        sorting = UnitsListSorting.USAGE,
        unitFromId = UnitID.kilometer,
        input1 = "12",
        input2 = "",
      )

    assertEquals(expected, actual)
  }

  @Test
  fun filterUnitsAndBatchConvert_favoritesFirstWithInput() = runTest {
    // Favorites lead the list of the second unit whether a value is typed or not
    unitConverterRepo.favorite(UnitID.mile)
    unitConverterRepo.favorite(UnitID.inch)

    for (input in listOf("", "12")) {
      val units =
        unitConverterRepo
          .filterUnitsAndBatchConvert(
            query = "",
            unitGroup = UnitGroup.LENGTH,
            favoritesOnly = false,
            sorting = UnitsListSorting.USAGE,
            unitFromId = UnitID.kilometer,
            input1 = input,
            input2 = "",
          )
          .getValue(UnitGroup.LENGTH)

      assertEquals(lengthCollection.size, units.size)
      assertEquals(
        setOf(UnitID.mile, UnitID.inch),
        units.take(2).map { it.basicUnit.id }.toSet(),
      )
      assertEquals(listOf(true, true, false), units.take(3).map { it.stats.isFavorite })
    }
  }

  @Test
  fun filterUnitsAndBatchConvert_brokenInput() = runTest {
    unitConverterRepo.favorite(UnitID.meter)

    val expected =
      mapOf(
        UnitGroup.LENGTH to
          listOf(
            UnitSearchResultItem(
              lengthCollection.first { it.id == UnitID.meter },
              UnitStats(UnitID.meter, isFavorite = true, frequency = 0),
              null,
            )
          )
      )

    val actual =
      unitConverterRepo.filterUnitsAndBatchConvert(
        query = "",
        unitGroup = UnitGroup.LENGTH,
        favoritesOnly = true,
        sorting = UnitsListSorting.USAGE,
        unitFromId = UnitID.kilometer,
        input1 = "!!!",
        input2 = "",
      )

    assertEquals(expected, actual)
  }

  @Test
  fun filterUnitsAndBatchConvert_convertFootInchInput() = runTest {
    unitConverterRepo.favorite(UnitID.inch)

    val expected =
      mapOf(
        UnitGroup.LENGTH to
          listOf(
            UnitSearchResultItem(
              lengthCollection.first { it.id == UnitID.inch },
              UnitStats(UnitID.inch, isFavorite = true, frequency = 0),
              ConverterResult.Default(
                value = KBigDecimal("126").setMaxScale(),
                calculation = KBigDecimal("10.5").setMaxScale(),
              ),
            )
          )
      )

    val actual =
      unitConverterRepo.filterUnitsAndBatchConvert(
        query = "",
        unitGroup = UnitGroup.LENGTH,
        favoritesOnly = true,
        sorting = UnitsListSorting.USAGE,
        unitFromId = UnitID.foot,
        input1 = "10",
        input2 = "6",
      )

    assertEquals(expected, actual)
  }
}
