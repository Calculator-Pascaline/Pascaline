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

package com.sadellie.unitto.core.data.converter

import com.sadellie.unitto.core.common.KBigDecimal
import com.sadellie.unitto.core.common.KRoundingMode
import com.sadellie.unitto.core.common.setMaxScale
import com.sadellie.unitto.core.data.converter.collections.lengthCollection
import com.sadellie.unitto.core.database.CurrencyRatesDaoInMemory
import com.sadellie.unitto.core.database.UnitsDaoInMemory
import com.sadellie.unitto.core.model.converter.UnitGroup
import com.sadellie.unitto.core.model.converter.UnitsListSorting
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
