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

import com.sadellie.unitto.core.database.CurrencyRatesDaoInMemory
import com.sadellie.unitto.core.database.UnitsDaoInMemory
import com.sadellie.unitto.core.database.UnitsEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitRepositoryGetPairIdTest {
  private val fakeUnitsDao = UnitsDaoInMemory()
  private val unitsRepository = UnitsRepository(fakeUnitsDao)
  private val unitConverterRepo =
    UnitConverterRepositoryImpl(unitsRepository)

  @Test
  fun getPairId_useDatabaseInfo() = runTest {
    // insert info about paired unit
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.attometer, pairedUnitId = UnitID.kilometer))

    // now try to find via repository
    val expected = UnitID.kilometer
    val actual = unitConverterRepo.getPairId(UnitID.attometer)
    assertEquals(expected, actual)
  }

  @Test
  fun getPairId_fallbackToFirstInCollection() = runTest {
    // first in lengthCollection
    val expected = UnitID.attometer
    val actual = unitConverterRepo.getPairId(UnitID.kilometer)
    assertEquals(expected, actual)
  }

  @Test
  fun getPairId_fallbackToPopularWithoutFavorite() = runTest {
    // insert info about paired unit
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.meter, isFavorite = false, frequency = 100))
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.kilometer, isFavorite = false, frequency = 200))
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.mile, isFavorite = false, frequency = 0))
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.inch, isFavorite = false, frequency = 50))
    // add some trash, must be ignored (different unit group)
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.binary, isFavorite = true, frequency = 1_000))
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.kelvin, isFavorite = true, frequency = 60_000))

    // Fallback to most popular
    val expected = UnitID.kilometer
    val actual = unitConverterRepo.getPairId(UnitID.attometer)
    assertEquals(expected, actual)
  }

  @Test
  fun getPairId_fallbackToPopularFavorite() = runTest {
    // insert info about paired unit
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.meter, isFavorite = false, frequency = 100))
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.kilometer, isFavorite = false, frequency = 200))
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.mile, isFavorite = true, frequency = 0))
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.inch, isFavorite = true, frequency = 50))
    // add some trash, must be ignored (different unit group)
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.binary, isFavorite = true, frequency = 1_000))
    fakeUnitsDao.insertUnit(UnitsEntity(UnitID.kelvin, isFavorite = true, frequency = 60_000))

    // Fallback to most popular and favorite
    val expected = UnitID.inch
    val actual = unitConverterRepo.getPairId(UnitID.attometer)
    assertEquals(expected, actual)
  }
}
